/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import se.dabox.service.common.ajaxlongrun.StatusCallable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocosite.user.MiniUserInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.ccbc.project.ProjectProductTransformers;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.activity.ActivityComponent;
import se.dabox.service.common.coursedesign.activity.MultiPageActivityCourse;
import se.dabox.service.common.coursedesign.activity.MultiPageCourseCddActivityCourseFactory;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatus;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatusFactory;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProductReportBuilder extends AbstractProductReportBuilder<UserProductStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductReportBuilder.class);

    private final Map<String, Map<String, Object>> prodInfoMap = new TreeMap<>();

    private long candidateProjectId;
    private Set<ProductId> candidateProductIds;

    private final Map<Long, UserProductStatus> userProdStatus = new HashMap<>();

    public ProductReportBuilder(ServiceRequestCycle cycle) {
        super(cycle);
    }

    public static StatusCallable<byte[]> getReportForOrgUnitTask(final ServiceRequestCycle cycle,
            final long ouId, final Locale userLocale) {

        StatusCallable<byte[]> callable
                = new ReportCallable<byte[]>(cycle.getServiceApplication()) {
                    private volatile ProductReportBuilder builder;
                    private NumberFormat format;

                    @Override
                    protected byte[] callInCycle(ServiceRequestCycle cycle) throws Exception {
                        builder = new ProductReportBuilder(cycle);
                        return builder.getReportForOrgUnit(ouId);
                    }

                    @Override
                    public Status getStatus() {
                        if (builder == null) {
                            return null;
                        } else {
                            if (format == null) {
                                format = NumberFormat.getIntegerInstance(userLocale);
                            }

                            return new Status("Processing: " + format.format(builder.getProcessed()));
                        }
                    }
                };

        return callable;
    }

    public static StatusCallable<byte[]> getReportForProjectTask(final ServiceRequestCycle cycle,
            final OrgProject project, final Locale userLocale) {

        StatusCallable<byte[]> callable
                = new ReportCallable<byte[]>(cycle.getServiceApplication()) {
                    private volatile ProductReportBuilder builder;
                    private NumberFormat format;

                    @Override
                    protected byte[] callInCycle(ServiceRequestCycle cycle) throws Exception {
                        builder = new ProductReportBuilder(cycle);
                        return builder.getReportForProject(project);
                    }

                    @Override
                    public Status getStatus() {
                        if (builder == null) {
                            return null;
                        } else {
                            if (format == null) {
                                format = NumberFormat.getIntegerInstance(userLocale);
                            }

                            return new Status("Processing: " + format.format(builder.getProcessed()));
                        }
                    }
                };

        return callable;
    }

    private UserProductStatus getMatListParticipantRowData(ProjectData projData,
            ProjectParticipation participant) {
        generateMatListCandidateProductStatusSet(projData);

        UserProductStatus ups = userProdStatus.computeIfAbsent(participant.getUserId(),
                UserProductStatus::new);

        for (ProjectProduct projectProduct : projData.projProducts) {
            Set<UUID> cids = Collections.singleton(projectProduct.getCid());
            ExtendedStatus status;

            if (participant.getLastAccess() == null) {
                status = ExtendedStatus.notAttempted;
            } else if (isProductCompleted(participant, cids)) {
                status = ExtendedStatus.completed;
            } else {
                status = ExtendedStatus.incomplete;
            }

            ups.addStatus(new ProductId(projectProduct.getProductId()), status);
        }

        return ups;
    }

    private UserProductStatus getDesignParticipantRowData(ProjectData projData,
            ProjectParticipation participant) {

        UserProductStatus ups = userProdStatus.computeIfAbsent(participant.getUserId(),
                UserProductStatus::new);

        Map<ProductId, Set<UUID>> pcms = projData.getDefinition().getProductCidMapSet();

        MultiPageActivityCourse course = getAcitivtyCourse(projData, participant);

        for (Map.Entry<ProductId, Set<UUID>> entry : pcms.entrySet()) {
            Set<UUID> cids = entry.getValue();
            ProductId productId = entry.getKey();

            for (UUID cid : cids) {
                ActivityComponent component = course.findComponent(cid);
                if (component == null) {
                    continue;
                }

                ExtendedStatus status = new ExtendedStatusFactory().statusFor(component);
                ups.addStatus(productId, status);
            }
        }

        return ups;
    }


    private List<Map<String, Object>> generateUserDataRows(
            Map<Long, UserProductStatus> userProdStatus) {
        List<Map<String, Object>> rows = new ArrayList<>(userProdStatus.size());

        for (Map.Entry<Long, UserProductStatus> entry : userProdStatus.entrySet()) {
            Map<String, Object> row = new HashMap<>();

            Long userId = entry.getKey();
            Map<String, ExtendedStatus> prodStatusMap = entry.getValue().getProductStatusMap();

            prodStatusMap = onlyExistingProductsMap(prodStatusMap);

            //Skip users that are invited but have no product information
            if (prodStatusMap.isEmpty()) {
                continue;
            }

            MiniUserInfo userInfo = icHelper.getMiniUserInfo(userId);

            row.put("userId", userInfo.getUserId());
            row.put("displayName", userInfo.getDisplayName());
            row.put("email", userInfo.getEmail());
            row.put("userThumbnail", userInfo.getThumbnail());

            row.put("productStatus", prodStatusMap);

            rows.add(row);
        }

        return rows;
    }

    private boolean isProductCompleted(ProjectParticipation participant,
            Set<UUID> cids) {
        List<ParticipationProgress> progress;
        try {
            progress = progressCache.get(participant.getParticipationId());
        } catch (ExecutionException ex) {
            LOGGER.error("Failed to get participation state. Assuming not completed", ex);
            return false;
        }

        for (ParticipationProgress participationProgress : progress) {
            if (!participationProgress.isCompleted()) {
                continue;
            }

            if (cids.contains(participationProgress.getCid())) {
                return true;
            }
        }

        return false;
    }

    private void generateProdInfoMap(Map<Long, UserProductStatus> userProdStatus) {
        Set<String> productIds = new HashSet<>();

        for (UserProductStatus value : userProdStatus.values()) {
            productIds.addAll(value.getProductStatusMap().keySet());
        }

        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        //This list only contains ids that exists
        List<Product> products = pdClient.getProducts(productIds);

        final ReportProductInformation rpi = new ReportProductInformation(cycle);
        for (final Product product : products) {
            Map<String, Object> pim = rpi.forProduct(product);
            prodInfoMap.put(product.getId().getId(), pim);
        }
    }

    private Map<String, ExtendedStatus> onlyExistingProductsMap(Map<String, ExtendedStatus> prodStatusMap) {
        Map<String, ExtendedStatus> newMap = MapUtil.createHash(prodStatusMap.size());

        for (Map.Entry<String, ExtendedStatus> entry : prodStatusMap.entrySet()) {
            String prodId = entry.getKey();
            if (prodInfoMap.containsKey(prodId)) {
                newMap.put(prodId, entry.getValue());
            }
        }

        if (newMap.size() == prodStatusMap.size()) {
            return prodStatusMap;
        }

        return newMap;
    }

    private void generateMatListCandidateProductStatusSet(ProjectData projData) {
        final long projectId = projData.getProject().getProjectId();
        if (candidateProjectId == projectId) {
            return;
        }

        ProjectMaterialCoordinatorClient pmcClient
                = CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
        projData.projProducts
                = pmcClient.getProjectProducts(projectId);

        candidateProductIds = CollectionsUtil.transform(projData.projProducts,
                ProjectProductTransformers.
                getProductIdTransformer());
        candidateProjectId = projectId;
    }

    @Override
    protected UserProductStatus getParticipationData(final ProjectData projData,
            final ProjectParticipation participant) {
        if (!participant.isActivated()) {
            return null;
        }
        MiniUserInfo userInfo = icHelper.getMiniUserInfo(participant.getUserId());

        if (userInfo == null) {
            LOGGER.warn("Failed to get user {} from participation {}. Ignoring participation",
                    participant.getUserId(), participant.getParticipationId());
            return null;
        }
        UserProductStatus row
                = ProjectTypeUtil.call(projData.getProject(),
                        new ProjectTypeCallable<UserProductStatus>() {

                            @Override
                            public UserProductStatus callDesignedProject() {
                                return getDesignParticipantRowData(projData, participant);
                            }

                            @Override
                            public UserProductStatus callMaterialListProject() {
                                return getMatListParticipantRowData(projData, participant);
                            }

                            @Override
                            public UserProductStatus callSingleProductProject() {
                                return callMaterialListProject();
                            }

                        });

        //No data then return directly
        if (row == null) {
            return null;
        }

        return null;
    }

    @Override
    protected void populateJsonData(Map<String, Object> jsonResponse,
            List<UserProductStatus> partData) {

        generateProdInfoMap(userProdStatus);

        jsonResponse.put("aaData", generateUserDataRows(userProdStatus));
        jsonResponse.put("products", prodInfoMap);
    }

    private MultiPageActivityCourse getAcitivtyCourse(ProjectData projData,
            ProjectParticipation participant) {

        OrgProject project = projData.getProject();
        List<ParticipationProgress> progress =
                projData.getProgressMap().getOrDefault(participant.getParticipationId(),
                        Collections.emptyList());

        DatabankFacade databank = projData.getDatabankFacade();

        CourseDesignDefinition cdd = projData.getDefinition();

        MultiPageActivityCourse course
                = new MultiPageCourseCddActivityCourseFactory().newActivityCourse(project, progress, databank,
                        cdd);

        return course;
    }

}
