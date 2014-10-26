/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.partdetails;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.project.ParticipationModule;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocosite.date.DateFormatters;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.participation.crisppart.ParticipationCrispProductReport;
import se.dabox.service.common.ccbc.product.IdProjectProductUtil;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.ccbc.project.ProjectProductTransformers;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.material.FetchMode;
import se.dabox.service.common.ccbc.project.material.GetParticipationCrispProductReportsRequest;
import se.dabox.service.common.ccbc.project.material.GetParticipationCrispProductStatusRequest;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.CocoboxProductTypeConstants;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductTypeUtil;
import se.dabox.service.common.proddir.material.ProductMaterialConverter;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ParticipationDetailsCommand {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParticipationDetailsCommand.class);

    private final RequestCycle cycle;
    private ProjectParticipation participation;
    private OrgProject project;
    private List<ParticipationCrispProductReport> reports;
    private Set<Product> adminLinkProducts;
    private List<ProgressComponentInfo> progressInfos;
    private boolean refreshCrispInformation = false;
    private List<IdProjectModel> idProjects;

    public ParticipationDetailsCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    /**
     * Configures this command if the crisp progress information should be refreshed or not.
     *
     * @param doRefresh True if information should be fetched again.
     *
     * @return
     */
    public ParticipationDetailsCommand refreshCrispInformation(boolean doRefresh) {
        this.refreshCrispInformation = doRefresh;

        return this;
    }

    /**
     * Generate a json response with participation details for the specified participation and
     * project. The project must match the participation.
     *
     * @param project
     * @param participation
     *
     * @return
     *
     */
    public RequestTarget forParticipation(OrgProject project, ProjectParticipation participation) {
        this.project = project;
        this.participation = participation;

        if (refreshCrispInformation) {
            doRefreshCrispInformation();
        }
        getIdProjects();
        getReports();
        getAdminLinkProducts();
        progressInfos = new ProgressComponentResolver(cycle, project, participation).resolve();

        return createJsonResponse();
    }

    private RequestTarget createJsonResponse() {
        Locale locale = CocositeUserHelper.getUserLocale(cycle);

        DateFormat format =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        format.setTimeZone(project.getTimezone());

        byte[] jsonData = new JsonEncoding(format) {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();

                generator.writeArrayFieldStart("reports");
                for (ParticipationCrispProductReport report : reports) {
                    encodeCrispReport(generator, report);
                }
                generator.writeEndArray();

                generator.writeArrayFieldStart("adminlinks");
                for (Product adminProduct : adminLinkProducts) {
                    encodeAdminProduct(generator, adminProduct);
                }
                generator.writeEndArray();

                generator.writeArrayFieldStart("progress");
                for (ProgressComponentInfo pinfo : progressInfos) {
                    encodeProgressComponentInfo(generator, pinfo);
                }
                generator.writeEndArray();

                generator.writeArrayFieldStart("idprojects");
                for (IdProjectModel idProjectModel : idProjects) {
                    encodeIdProjectModel(generator, idProjectModel);
                }
                generator.writeEndArray();

                generator.writeEndObject();
            }

            private void encodeCrispReport(JsonGenerator generator,
                    ParticipationCrispProductReport report) throws IOException {
                generator.writeStartObject();

                generator.writeNumberField("id", report.getId());
                generator.writeStringField("title", report.getTitle());
                generator.writeStringField("type", report.getType());
                boolean isAdminReport = report.inScope("admin");
                generator.writeBooleanField("isAdminReport", isAdminReport);
                if (isAdminReport) {
                    String link = cycle.urlFor(ParticipationModule.class,
                            "participationReport",
                            Long.toString(participation.getParticipationId()),
                            Long.toString(report.getId()));

                    generator.writeStringField("adminReportLink", link);
                }
                generator.writeBooleanField("isUserReport", report.inScope("user"));
                encodeProductInformation(generator, report.getProductId());

                generator.writeEndObject();
            }

            private void encodeProductInformation(JsonGenerator generator, String productId) throws IOException {
                Product product = getProduct(productId);
                encodeProductInformation(generator, product);
            }

            private void encodeProductInformation(JsonGenerator generator, Product product) throws IOException {
                generator.writeStringField("productId", product.getId().getId());
                generator.writeStringField("productTitle", product.getTitle());
                generator.writeStringField("productShortTitle", product.getShortTitle());

                ProductMaterialConverter converter =
                        new ProductMaterialConverter(cycle, getProductDirectoryClient());
                Material material = converter.convert(product);

                String thumbnailUrl = material.getThumbnail(32);

                if (thumbnailUrl != null) {
                    generator.writeStringField("productThumbnail", thumbnailUrl);
                }
            }

            private void encodeOrgMatInformation(JsonGenerator generator, OrgMaterial orgMat) throws IOException {
                generator.writeStringField("orgMatTitle", orgMat.getTitle());
                generator.writeStringField("orgMatFilename", orgMat.getCrlinkFileName());
                generator.writeStringField("orgMatType", orgMat.getType());
            }

            private void encodeComponentInformation(JsonGenerator generator, Component component)
                    throws IOException {
                String title = component.getProperties().get("title");
                generator.writeStringField("componentTitle", title);
                generator.writeStringField("componentType", component.getType());
                generator.writeStringField("componentBasetype", component.getBasetype());
                generator.writeStringField("componentSubtype", component.getSubtype());
            }

            private void encodeAdminProduct(JsonGenerator generator, Product adminProduct) throws IOException {
                generator.writeStartObject();

                generator.writeStringField("adminLink", "http://www.dn.se");
                encodeProductInformation(generator, adminProduct);

                generator.writeEndObject();
            }

            private void encodeProgressComponentInfo(JsonGenerator generator,
                    ProgressComponentInfo pinfo) throws IOException {
                generator.writeStartObject();

                generator.writeStringField("cid", pinfo.getCid().toString());
                generator.writeStringField("type", pinfo.getType().toString());
                generator.writeBooleanField("completed", pinfo.getCompleted() != null);
                if (pinfo.getCompleted() != null) {
                    writeDateField(generator, "completedDate", pinfo.getCompleted());
                }

                if (pinfo.getCompleted() != null) {
                    String isoDate = DateFormatters.JQUERYAGO_FORMAT.format(pinfo.getCompleted());
                    generator.writeStringField("completedDateAgo", isoDate);
                }

                if (pinfo.getProduct() != null) {
                    encodeProductInformation(generator, pinfo.getProduct());
                }

                if (pinfo.getComponent() != null) {
                    encodeComponentInformation(generator, pinfo.getComponent());
                }

                if (pinfo.getOrgMat() != null) {
                    encodeOrgMatInformation(generator, pinfo.getOrgMat());
                }

                generator.writeEndObject();
            }

            private void encodeIdProjectModel(JsonGenerator generator, IdProjectModel idProjectModel)
                    throws IOException {
                generator.writeStartObject();
                generator.writeStringField("name", idProjectModel.getTitle());
                generator.writeStringField("link", idProjectModel.getLink());
                generator.writeNumberField("invited", idProjectModel.getInvited());
                generator.writeNumberField("completed", idProjectModel.getCompleted());
                generator.writeEndObject();
            }
        }.encode();

        return new JsonRequestTarget(jsonData, JsonRequestTarget.DEFAULT_ENCODING);
    }

    private void getReports() {
        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient();

        GetParticipationCrispProductReportsRequest req =
                new GetParticipationCrispProductReportsRequest(getParticipationId());
        req.setIgnoreIntegrationErrors(true);
        reports = pmcClient.getParticipationCrispProductReports(req);
    }

    private void getIdProjects() {
        List<IdProjectModel> list = new ArrayList<>();

        idProjects = list;

        if (project.getType() != ProjectType.DESIGNED_PROJECT) {
            return;
        }

        List<ProjectProduct> projProds =
                getProjectMaterialCoordinatorClient().getProjectProducts(project.getProjectId());
        List<String> productIds =
                CollectionsUtil.transformList(projProds, ProjectProductTransformers.
                getProductIdStrTransformer());

        List<Product> products = getProductDirectoryClient().getProducts(productIds);

        Map<String, String> partState = getParticipationState();

        for (Product product : products) {
            if (!product.getProductTypeId().equals(CocoboxProductTypeConstants.IDPROJECT)) {
                continue;
            }

            String stateName = IdProjectProductUtil.getProjectIdStateName(product.getId());
            String strProjectId = partState.get(stateName);
            if (strProjectId == null) {
                continue;
            }

            long projectId = Long.parseLong(strProjectId);

            int invited = 0;
            int completed = 0;

            List<ProjectParticipation> participations;
            try {
                 participations = getCocoboxCordinatorClient().
                        listProjectParticipations(projectId);
            } catch (NotFoundException nfe) {
                LOGGER.warn(
                        "IdProject {} not found for participation {} and product {} (masterproject {})",
                        projectId, participation.getParticipationId(), product.getId(), project);
                continue;
            }

            if (participations == null) {
                continue;
            }

            for (ProjectParticipation projectParticipation : participations) {
                if (projectParticipation.isActivated() && projectParticipation.getActivityCount()
                        != null && projectParticipation.getActivitiesCompleted() != null) {
                    invited++;
                } else {
                    continue;
                }

                if (projectParticipation.getActivityCount().equals(
                        projectParticipation.getActivitiesCompleted())) {
                    completed++;
                }
            }

            String link = NavigationUtil.toProjectPageUrl(cycle, projectId);
            IdProjectModel model =
                    new IdProjectModel(product.getTitle(), link, projectId, invited, completed);
            list.add(model);
        }

//        Random rand = new Random();
//
//        for (int i = 0; i < 10; i++) {
//            long prjId = 200 + i;
//            int invited = rand.nextInt(2000);
//            int completed = rand.nextInt(invited);
//            String link = NavigationUtil.toProjectPageUrl(cycle, prjId);
//            list.add(new IdProjectModel("ProductName " + i, link, prjId, invited, completed));
//        }
    }

    private ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient() {
        ProjectMaterialCoordinatorClient pmcClient =
                CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
        return pmcClient;
    }

    private ProductDirectoryClient getProductDirectoryClient() {
        return CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }

    private CocoboxCoordinatorClient getCocoboxCordinatorClient() {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    private long getParticipationId() {
        return participation.getParticipationId();
    }

    private Product getProduct(String productId) {
        ProductDirectoryClient pdClient =
                getProductDirectoryClient();

        Product product = pdClient.getProduct(productId);

        ProductTypeUtil.setType(pdClient, product);

        return product;
    }

    private void getAdminLinkProducts() {
        List<ProjectProduct> projProducts = getOrgProducts();

        List<String> productIds = CollectionsUtil.transformList(projProducts,
                ProjectProductTransformers.getProductIdStrTransformer());

        List<Product> products = getProductDirectoryClient().getProducts(true, productIds);
        ProductTypeUtil.setTypes(getProductDirectoryClient(), products);

        Set<Product> set = CollectionsUtil.subset(products, new Predicate<Product>() {
            @Override
            public boolean evalute(Product product) {
                if (!ProductUtils.isCrispProduct(product)) {
                    return false;
                }

                CrispContext ctx =
                        DwsCrispContextHelper.getCrispContext(cycle, product);

                if (ctx == null) {
                    return false;
                }

                return ctx.getDescription().getMethods().getReportUserParticipation() != null;
            }
        });

        adminLinkProducts = set;
    }

    private List<ProjectProduct> getOrgProducts() {
        return getProjectMaterialCoordinatorClient().getProjectProducts(project.getProjectId());
    }

    private void doRefreshCrispInformation() {
        GetParticipationCrispProductStatusRequest req =
                new GetParticipationCrispProductStatusRequest(participation.getParticipationId());
        req.setFetchMode(FetchMode.DIRECT);

        getProjectMaterialCoordinatorClient().getParticipationCrispProductStatus(req);
    }

    private Map<String, String> getParticipationState() {
        List<ProjectParticipationState> list =
                getCocoboxCordinatorClient().getParticipationState(Collections.
                singletonList(participation.getParticipationId()));
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }

        return list.get(0).getMap();
    }
}
