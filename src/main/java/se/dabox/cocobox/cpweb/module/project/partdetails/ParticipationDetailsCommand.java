/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.partdetails;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.project.ParticipationModule;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.date.DateFormatters;
import se.dabox.cocosite.freemarker.util.CdnUtils;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.learnifier.cmi.CompletionStatus;
import se.dabox.learnifier.cmi.SuccessStatus;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.ParticipationProgress;
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
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.activity.Activity;
import se.dabox.service.common.coursedesign.activity.ActivityComponent;
import se.dabox.service.common.coursedesign.activity.ActivityPage;
import se.dabox.service.common.coursedesign.activity.CompletionInfo;
import se.dabox.service.common.coursedesign.activity.MultiPageActivityCourse;
import se.dabox.service.common.coursedesign.activity.MultiPageCourseCddActivityCourseFactory;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatus;
import se.dabox.service.common.coursedesign.extstatus.ExtendedStatusFactory;
import se.dabox.service.common.coursedesign.project.GetProjectCourseDesignCommand;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.CocoboxProductTypeConstants;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductTypeUtil;
import se.dabox.service.common.proddir.material.ProductMaterialConverter;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.ValueUtils;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ParticipationDetailsCommand {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParticipationDetailsCommand.class);

    private enum ActivityStatus { notAttempted, incomplete, completed, overdue, locked, failed };

    private final RequestCycle cycle;
    private ProjectParticipation participation;
    private OrgProject project;
    private List<ParticipationCrispProductReport> reports;
    private Set<Product> adminLinkProducts;
    private List<ProgressComponentInfo> progressInfos;
    private boolean refreshCrispInformation = false;
    private List<IdProjectModel> idProjects;
    private MultiPageActivityCourse course;
    private Map<UUID, ProgressComponentInfo> progressInfoMap;

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

        course = createActivityCourseModel();

        getIdProjects();
        getReports();
        getAdminLinkProducts();
        progressInfos = new ProgressComponentResolver(cycle, project, participation).resolve();

        progressInfoMap = CollectionsUtil.createMap(progressInfos, ProgressComponentInfo::getCid);

        return createJsonResponse();
    }

    private RequestTarget createJsonResponse() {
        final Locale locale = CocositeUserHelper.getUserLocale(cycle);

        DateFormat format =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        format.setTimeZone(project.getTimezone());

        byte[] jsonData = new JsonEncoding(format) {
            private JsonGenerator generator;
            private String thumbnailUrl;

            private final NumberFormat percentFormat = NumberFormat.getPercentInstance(locale);
            private final NumberFormat scoreFormat = NumberFormat.getInstance(locale);

            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                this.generator = generator;
                generator.writeStartObject();

                generator.writeNumberField("activityCount", participation.getActivityCount());
                generator.writeNumberField("activitiesTotal", participation.getActivitiesCompleted());

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

                generator.writeArrayFieldStart("idprojects");
                for (IdProjectModel idProjectModel : idProjects) {
                    encodeIdProjectModel(generator, idProjectModel);
                }
                generator.writeEndArray();

                generator.writeArrayFieldStart("pages");
                for (ActivityPage page : course.getPageList()) {
                    encodeCoursePage(page);
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

                thumbnailUrl = material.getThumbnail(32);

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

                writeStringOrNull(generator, "completionStatus", pinfo.getCompletionStatus());
                writeStringOrNull(generator, "successStatus", pinfo.getSuccessStatus());

                if (pinfo.getScore() == null) {
                    generator.writeNullField("score");
                } else {
                    generator.writeNumberField("score", pinfo.getScore());

                    if (isPercentScore(pinfo)) {

                        BigDecimal pscore = pinfo.getScore().divide(BigDecimal.valueOf(100), 2,
                                RoundingMode.HALF_EVEN);

                        String percent = percentFormat.format(pscore);
                        generator.writeStringField("scoreStr", percent);
                    } else {
                        String numVal = scoreFormat.format(pinfo.getScore());
                        generator.writeStringField("scoreStr", numVal);
                    }

                }

                generator.writeStringField("componentStatus", getComponentStatusStr(pinfo));

                encodeThumbnail(pinfo);


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

            private void writeStringOrNull(JsonGenerator generator, String name,
                    Object obj) throws IOException {
                if (obj == null) {
                    generator.writeNullField(name);
                } else {
                    generator.writeStringField(name, obj.toString());
                }
            }

            private void encodeCoursePage(ActivityPage page) throws IOException {
                generator.writeStartObject();

                generator.writeStringField("title", page.getTitle());
                generator.writeStringField("text", page.getText());

                generator.writeArrayFieldStart("activity");
                for (Activity activity : page.getActivityList()) {
                    encodeCourseActivity(activity);
                }
                generator.writeEndArray();

                generator.writeEndObject();
            }

            private void encodeCourseActivity(Activity activity) throws IOException {
                generator.writeStartObject();

                String title = getActivityTitle(activity);
                generator.writeStringField("title", title);
                writeStringOrNull(generator, "completionStatus", activity.getCompletionStatus());
                writeStringOrNull(generator, "successStatus", activity.getSuccessStatus());
                generator.writeBooleanField("completed", activity.isCompleted());
                generator.writeBooleanField("enabled", activity.isEnabled());
                generator.writeBooleanField("overdue", activity.isOverdue());
                generator.writeBooleanField("visible", activity.isVisible());
                generator.writeStringField("activityStatus", getActivityStatusStr(activity));

                final Date completeByDate = activity.getCompleteByDate();
                writeDateField(generator, "completeByDate", completeByDate);
                if (completeByDate != null) {
                    String isoDate = DateFormatters.JQUERYAGO_FORMAT.format(completeByDate);
                    generator.writeStringField("completeByDateAgo", isoDate);
                }

                final Date completedDate = activity.getCompletedDate();
                writeDateField(generator, "completedDate", completedDate);
                if (completedDate != null) {
                    String isoDate = DateFormatters.JQUERYAGO_FORMAT.format(completedDate);
                    generator.writeStringField("completedDateAgo", isoDate);
                }

                generator.writeArrayFieldStart("component");
                for (ActivityComponent component : activity.getComponents()) {
                    flushProgressComponentInfoCache();
                    encodeActivityComponent(component);
                }
                generator.writeEndArray();

                generator.writeEndObject();
            }

            private void encodeActivityComponent(ActivityComponent component) throws IOException {
                ProgressComponentInfo info = progressInfoMap.get(component.getCid());
                if (info == null) {
                    return;
                }

                encodeProgressComponentInfo(generator, info);
            }

            private String getActivityTitle(Activity activity) {
                String title = activity.getActivityContainer().getProperties().get("title");
                if (title == null) {
                    ActivityComponent pc = activity.getPrimaryComponent();
                    if (pc != null) {
                        title = pc.getProperties().get("title");
                    }
                }

                return ValueUtils.coalesce(title, "");
            }

            private void encodeThumbnail(ProgressComponentInfo pinfo) throws IOException {
                String tn = thumbnailUrl;

                if (tn == null) {
                    if (pinfo.getComponent().getBasetype().equals("ev_classroom")) {
                        tn = CdnUtils.getResourceUrl("/cocobox/img/eventtypes/date.svg");
                    } else if (pinfo.getComponent().getBasetype().equals("ev_virtual")) {
                        tn = CdnUtils.getResourceUrl("/cocobox/img/eventtypes/display1.svg");
                    } else if (pinfo.getComponent().getBasetype().equals("ev_call")) {
                        tn = CdnUtils.getResourceUrl("/cocobox/img/eventtypes/call.svg");
                    } else {
                        tn = CdnUtils.getResourceUrl("/cocobox/img/eventtypes/default.svg");
                    }
                }
                generator.writeStringField("thumbnail", tn);
            }

            private void flushProgressComponentInfoCache() {
                thumbnailUrl = null;
            }

            private String getActivityStatusStr(Activity activity) {
                return getActivityStatus(activity).name();
            }

            private ExtendedStatus getActivityStatus(Activity activity) {
                return new ExtendedStatusFactory().statusFor(activity);
            }

            private String getComponentStatusStr(ProgressComponentInfo pinfo) {
                return getComponentStatus(pinfo).toString();
            }

            private ExtendedStatus getComponentStatus(final ProgressComponentInfo pinfo) {
                return new ExtendedStatusFactory().statusFor(new CompletionInfo() {
                    @Override
                    public Date getCompletedDate() {
                        return pinfo.getCompleted();
                    }

                    @Override
                    public CompletionStatus getCompletionStatus() {
                        return pinfo.getCompletionStatus();
                    }

                    @Override
                    public SuccessStatus getSuccessStatus() {
                        return pinfo.getSuccessStatus();
                    }

                    @Override
                    public boolean isCompleted() {
                        return pinfo.getCompletionStatus() == CompletionStatus.completed;
                    }

                    @Override
                    public boolean isEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isOverdue() {
                        return false;
                    }

                    @Override
                    public boolean isProgressTrackable() {
                        return true;
                    }
                });
            }

            private boolean isPercentScore(ProgressComponentInfo pinfo) {
                Product product = pinfo.getProduct();

                if (product == null) {
                    return false;
                }

                String scoretype = product.getFieldSingleValue("scormscoretype");
                return "percent".equals(scoretype);
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

        Set<Product> set = CollectionsUtil.subset(products, (Product product) -> {
            if (!ProductUtils.isCrispProduct(product)) {
                return false;
            }

            CrispContext ctx =
                    DwsCrispContextHelper.getCrispContext(cycle, product);

            if (ctx == null) {
                return false;
            }

            return ctx.getDescription().getMethods().getReportUserParticipation() != null;
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

    private MultiPageActivityCourse createActivityCourseModel() {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        CourseDesignDefinition cdd
                = new GetProjectCourseDesignCommand(cycle, userLocale).
                setFallbackToStageDesign(true).
                forProject(project);

        List<ParticipationProgress> progress
                = getCocoboxCordinatorClient().getParticipationProgress(participation.
                        getParticipationId());

        DatabankFacade databankFacade
                = new GetDatabankFacadeCommand(cycle).setFallbackToStageDatabank(true).get(project);

        return new MultiPageCourseCddActivityCourseFactory().
                setAllowTemporalProgressTracking(true).
                newActivityCourse(project, progress,
                databankFacade, cdd);
    }
}
