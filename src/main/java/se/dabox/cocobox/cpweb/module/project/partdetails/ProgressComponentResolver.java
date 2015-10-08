/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.partdetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.apache.commons.lang3.StringUtils;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.ccbc.project.ProjectSubtypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.TemporalComponentExtractor;
import se.dabox.service.common.ccbc.project.TemporalProgressComponent;
import se.dabox.service.common.ccbc.project.cddb.DatabankEntry;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.coursedesign.ComponentUtil;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.ProgressComponentHelper;
import se.dabox.service.common.coursedesign.util.ComponentTypeHelper;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class ProgressComponentResolver {

    private final ServiceRequestCycle cycle;
    private final OrgProject project;
    private ProgressComponentHelper cachedHelper;
    private final ProjectParticipation participation;

    public ProgressComponentResolver(ServiceRequestCycle cycle, OrgProject project,
            ProjectParticipation participation) {
        this.cycle = cycle;
        this.project = project;
        this.participation = participation;
        ParamUtil.required(cycle, "cycle");
        ParamUtil.required(project, "project");
        ParamUtil.required(participation, "participation");
    }

    List<ProgressComponentInfo> resolve() {

        return ProjectTypeUtil.call(project,
                new ProjectTypeCallable<List<ProgressComponentInfo>>() {
                    @Override
                    public List<ProgressComponentInfo> callDesignedProject() {
                        return designProjectComponents();
                    }

                    @Override
                    public List<ProgressComponentInfo> callMaterialListProject() {
                        return materialListComponents();
                    }

                    @Override
                    public List<ProgressComponentInfo> callSingleProductProject() {
                        return materialListComponents();
                    }
                });

    }

    private List<ProgressComponentInfo> designProjectComponents() {
        if (project.getDesignId() == null) {
            return Collections.emptyList();
        }

        CourseDesignClient cdClient = CacheClients.getClient(cycle,
                CourseDesignClient.class);
        CourseDesign design = cdClient.getDesign(project.getDesignId());
        CourseDesignDefinition cdd =
                CddCodec.decode(cycle, design.getDesign());

        List<DatabankEntry> databankEntries = getDatabankEntries();
        List<TemporalProgressComponent> tcList =
                TemporalComponentExtractor.extract(cdd, databankEntries, project.getTimezone());

        Map<UUID, TemporalProgressComponent> temporalMap = CollectionsUtil.createMap(tcList, 
                TemporalProgressComponent::getCid);

        List<ProgressComponentInfo> infos = new ArrayList<>();

        Date now = new Date();

        Map<UUID, ParticipationProgress> progressMap = getProgressMap();

        ProgressComponentHelper pcHelper = getProgressComponentHelper();
        for (Component comp : cdd.getComponentsRecursive()) {
            TemporalProgressComponent tempComp = temporalMap.get(comp.getCid());

            if (tempComp != null) {
                ProgressComponentInfo info =
                        ProgressComponentInfo.forTemporal(tempComp, comp);
                if (tempComp.hasTriggered(now)) {
                    info.setCompleted(tempComp.getTriggerTime());
                }
                infos.add(info);
                continue;
            }

            if (!pcHelper.isTrackingEnabledForComponent(comp)) {
                continue;
            }

            ProgressComponentInfo info;
            if (pcHelper.isClickTrackingEnabledForComponent(comp)) {

                if (StringUtils.startsWith(ComponentTypeHelper.getSubType(comp), "orgmat|")) {
                    OrgMaterial orgMat = getOrgMat(comp);
                    info = ProgressComponentInfo.forOrgMat(comp.getCid(), orgMat);
                } else {
                    ProgressComponentType type = ProgressComponentType.PRODUCT;
                    Product product = getComponentProduct(comp);
                    info = ProgressComponentInfo.forProduct(comp.getCid(), type, product);
                }
            } else {
                ProgressComponentType type = ProgressComponentType.CRISP;
                Product product = getComponentProduct(comp);
                info = ProgressComponentInfo.forProduct(comp.getCid(), type, product);
            }

            info.setComponent(comp);

            ParticipationProgress progress = progressMap.get(info.getCid());
            if (progress != null && progress.isCompleted()) {
                info.setCompleted(progress.getCompletionDate());
            }


            infos.add(info);
        }

        return infos;
    }

    private List<ProgressComponentInfo> materialListComponents() {
        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient();
        long projectId = project.getProjectId();

        List<OrgMaterial> orgMats = pmcClient.getProjectOrgMaterials(projectId);

        List<ProgressComponentInfo> orgMatInfos = toInfos(orgMats);

        List<ProjectProduct> products = pmcClient.getProjectProducts(projectId);

        List<ProgressComponentInfo> productInfos = toProjProductInfos(products);

        List<ProgressComponentInfo> infos = new ArrayList<>(orgMatInfos.size()
                + productInfos.size());
        infos.addAll(orgMatInfos);
        infos.addAll(productInfos);

        Map<UUID, ParticipationProgress> progressMap = getProgressMap();

        for (ProgressComponentInfo progressComponentInfo : infos) {
            ParticipationProgress progress = progressMap.get(progressComponentInfo.getCid());
            if (progress != null && progress.isCompleted()) {
                progressComponentInfo.setCompleted(progress.getCreated());
            }
        }

        return infos;
    }

    private ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient() {
        return CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
    }

    private List<ProgressComponentInfo> toInfos(List<OrgMaterial> orgMats) {
        return CollectionsUtil.transformList(orgMats,
                new Transformer<OrgMaterial, ProgressComponentInfo>() {
                    @Override
                    public ProgressComponentInfo transform(OrgMaterial item) {
                        ProgressComponentInfo pci = ProgressComponentInfo.forOrgMat(item.getCid(),
                                item);
                        return pci;
                    }
                });
    }

    private List<ProgressComponentInfo> toProjProductInfos(List<ProjectProduct> products) {
        return CollectionsUtil.transformListNotNull(products,
                new Transformer<ProjectProduct, ProgressComponentInfo>() {
                    @Override
                    public ProgressComponentInfo transform(ProjectProduct item) {
                        Product product = getProduct(new ProductId(item.getProductId()));
                        ProgressComponentHelper helper = getProgressComponentHelper();

                        if (!helper.isTrackingEnabledForProduct(product)) {
                            return null;
                        }

                        ProgressComponentType type =
                                ProgressComponentType.CRISP;

                        if (helper.isClickTrackingEnabledForProduct(product)) {
                            type = ProgressComponentType.PRODUCT;
                        }

                        return ProgressComponentInfo.forProduct(item.getCid(), type,
                                product);
                    }
                });
    }

    private Product getProduct(ProductId productId) {
        ProductDirectoryClient pdClient =
                CacheClients.getClient(cycle, ProductDirectoryClient.class);

        return ProductFetchUtil.getProduct(pdClient, productId);
    }

    private ProgressComponentHelper getProgressComponentHelper() {
        if (cachedHelper == null) {            
            cachedHelper = new ProgressComponentHelper(cycle);
        }

        return cachedHelper;
    }

    private List<DatabankEntry> getDatabankEntries() {
        return ProjectTypeUtil.callSubtype(project,
                new ProjectSubtypeCallable<List<DatabankEntry>>() {
                    @Override
                    public List<DatabankEntry> callMainProject() {
                        CocoboxCoordinatorClient ccbc =
                                CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

                        return ccbc.getDatabank(project.getMasterDatabank());                        
                    }

                    @Override
                    public List<DatabankEntry> callIdProjectProject() {
                        return Collections.emptyList();
                    }

                    @Override
                    public List<DatabankEntry> callChallengeProject() {
                        return Collections.emptyList();
                    }
                });
    }

    private Map<UUID, ParticipationProgress> getProgressMap() {
        CocoboxCoordinatorClient ccbc =
                CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        List<ParticipationProgress> progressList =
                ccbc.getParticipationProgress(participation.getParticipationId());

        return CollectionsUtil.createMap(progressList, ParticipationProgress::getCid);
    }

    private OrgMaterial getOrgMat(Component comp) {
        String subtype = comp.getSubtype();

        String orgMatId = subtype.split(Pattern.quote("|"), 2)[1];

        CocoboxCoordinatorClient ccbc =
                CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        return ccbc.getOrgMaterial(Long.valueOf(orgMatId));
    }

    private Product getComponentProduct(Component comp) {
        ProductId productId = ComponentUtil.getProductId(comp);

        if (productId == null) {
            return null;
        }

        return getProduct(productId);
    }
}
