/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.command.RecentTimezoneUpdateCommand;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailure;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailureFactory;
import se.dabox.cocobox.cpweb.module.project.productconfig.ExtraProductConfig;
import se.dabox.cocobox.cpweb.module.project.productconfig.ProductsExtraConfigFactory;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.cocobox.cpweb.state.NewProjectSessionProcessor;
import se.dabox.cocosite.druwa.CocoSiteConfKey;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.ClientFactoryException;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.DeniedException;
import se.dabox.service.common.ccbc.project.MissingProjectProductException;
import se.dabox.service.common.ccbc.project.NewProjectRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectProductException;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequest;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.expiration.GetCourseDefaultExpiration;
import se.dabox.service.common.coursedesign.techinfo.CpDesignTechInfo;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CodecException;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.coursecatalog.client.CocoboxCourseSourceConstants;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.create.CreateSessionRequest;
import se.dabox.service.coursecatalog.client.session.impl.StandardCourseSessionSource;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.proddir.data.ProductTypes;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCourseCatalogClient;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CreateProjectSessionProcessor implements NewProjectSessionProcessor {
    /**
     * The design id that indicates that a matlist project should be created instead.
     */
    private static final int MATLIST_DESIGNID = 0;

    private static final Logger LOGGER = LoggerFactory.
            getLogger(CreateProjectSessionProcessor.class);
    private static final long serialVersionUID = 1L;
    private final long orgId;

    private List<ProjectProductFailure> failures;

    public CreateProjectSessionProcessor(long orgId) {
        this.orgId = orgId;
    }

    @Override
    public RequestTarget processSession(final RequestCycle cycle, final NewProjectSession nps,
                                        final MatListProjectDetailsForm matListDetails) {

        String strNpsId = nps.getUuid().toString();

        if (missingExtraParameters(cycle, nps)) {
            //TODO: Redirect to settings page
            return toNewProductExtraSettingsPage(strNpsId);
        }

        final CreateProjectGeneral input = nps.getCreateProjectGeneral();

        ProjectType ptype = ProjectType.valueOf(nps.getType());

        NewProjectRequest npr = ProjectTypeUtil.call(ptype,
                new ProjectTypeCallable<NewProjectRequest>() {
                    @Override
                    public NewProjectRequest callDesignedProject() {

                        boolean autoIcal = getAutoIcalSetting();

                        return NewProjectRequest.newDesignedProject(
                                input.getProjectname(),
                                orgId,
                                input.getProjectlang(),
                                LoginUserAccountHelper.getUserId(cycle),
                                input.getCountry(),
                                input.getTimezone(),
                                "", //Project description
                                null, //User title
                                null, //User description
                                autoIcal);
                    }

                    @Override
                    public NewProjectRequest callMaterialListProject() {
                        ParamUtil.required(matListDetails, "matListDetails");
                        return NewProjectRequest.
                                newMaterialListProject(
                                        input.getProjectname(),
                                        orgId,
                                        input.getProjectlang(),
                                        LoginUserAccountHelper.getUserId(cycle),
                                        input.getCountry(),
                                        input.getTimezone(),
                                        null,
                                        matListDetails.getUserTitle(),
                                        matListDetails.getUserDescription());

                    }

                    private boolean getAutoIcalSetting() {
                        if (!WebFeatures.getFeatures(cycle).hasFeature(
                                CocositeWebFeatureConstants.AUTOICAL)) {
                            return false;
                        }

                        return DwsRealmHelper.getRealmConfiguration(cycle).getBooleanValue(
                                CocoSiteConfKey.AUTOICAL_DEFAULT_SETTING);
                    }

                    @Override
                    public NewProjectRequest callSingleProductProject() {
                        nps.setProds(Collections.singletonList(nps.getProductId()));
                        return NewProjectRequest.
                                newSingleProductProject(
                                        input.getProjectname(), orgId, input.getProjectlang(),
                                        LoginUserAccountHelper.getUserId(cycle), input.getCountry(),
                                        input.getTimezone(), null, matListDetails.getUserTitle(),
                                        matListDetails.getUserDescription(), nps.getProductId()
                                );
                    }

                });

        OrgProject project = null;
        String projectName = npr.getName();

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);
        AlreadyExistsException aex = null;
        for (int i = 0; i < 20; i++) {
            try {
                aex = null;
                if (i != 0) {
                    npr = npr.withName(String.format("%s (%d)", projectName, i));
                }

                long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
                project = ccbc.newProject(caller, npr);
                break;
            } catch (AlreadyExistsException ex) {
                aex = ex;
            }
        }

        if (aex != null || project == null) {
            throw new IllegalStateException("Unable to create new project. Name already exists",
                    aex);
        }

        if (nps.getOrgmats() != null) {
            for (Long orgMatId : nps.getOrgmats()) {
                addProjectOrgMat(pmcClient, project, orgMatId);
            }
        }

        if (nps.getProds() != null) {
            for (String prodId : nps.getProds()) {
                addProjectProduct(cycle,
                        pmcClient,
                        project,
                        prodId,
                        false,
                        nps.getProductExtraConfig(prodId));
            }
        }

        if (nps.getDesignId() != null && nps.getDesignId() != MATLIST_DESIGNID) {
            activateProjectDesign(cycle, project, nps);
        }

        cycle.getSession().removeAttribute(NewProjectSession.getSessionName(
                strNpsId));

        updateRecentTimezone(cycle, npr);

        if(nps.getCourseId() != null) {
            addCourseSession(cycle, project, nps);
        }

        if (failures != null) {
            cycle.getSession().setFlashAttribute(ProjectProductFailure.VIEWSESSION_NAME, failures);
        }

        if (ptype == ProjectType.DESIGNED_PROJECT) {
            return new WebModuleRedirectRequestTarget(VerifyProjectDesignModule.class,
                    VerifyProjectDesignModule.ACTION_VERIFY_NEW_DESIGN,
                    Long.toString(project.getProjectId()));
        } else if (ptype == ProjectType.SINGLE_PRODUCT_PROJECT) {
            UpdateProjectRequest updateReq
                    = new UpdateProjectRequestBuilder(project.getCreatedBy(), project.getProjectId()).
                            setUnstaged(
                                    false).createUpdateProjectRequest();
            ccbc.updateOrgProject(updateReq);
        }

        return toProjectPage(project.getProjectId());
    }

    private CocoboxCoordinatorClient getCocoboxCordinatorClient(RequestCycle cycle) {
        return Clients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    private RequestTarget toProjectPage(long projectId) {
        return NavigationUtil.toProjectPage(projectId);
    }

    private void activateProjectDesign(RequestCycle cycle, OrgProject project, NewProjectSession nps) {
        CourseDesignClient cdClient = Clients.getClient(cycle, CourseDesignClient.class);

        long userId = LoginUserAccountHelper.getUserId(cycle);

        String techInfo = CpDesignTechInfo.createStageTechInfo(project.getProjectId());

        long newDesignId = cdClient.copyDesign(nps.getDesignId(), userId, techInfo);
        long newDatabank = getCocoboxCordinatorClient(cycle).createDatabank(userId, project.
                getProjectId());

        project.setStageDesignId(newDesignId);
        project.setStageDatabank(newDatabank);

        CourseDesign design = cdClient.getDesign(newDesignId);
        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        String userTitle = cdd.getInfo().getUserTitle();
        String userDesc = cdd.getInfo().getUserDescription();

        UpdateProjectRequestBuilder updateBuilder
                = new UpdateProjectRequestBuilder(userId, project.getProjectId());

        updateBuilder.
                setStageDesignId(newDesignId).
                setStageDatabank(newDatabank).
                setUserTitle(userTitle).
                setUserDescription(userDesc).
                setDefaultExpiration(getDefaultExpiration(cycle, newDesignId));

        UpdateProjectRequest upr = updateBuilder.createUpdateProjectRequest();

        getCocoboxCordinatorClient(cycle).updateOrgProject(upr);
    }

    private void addCourseSession(RequestCycle cycle, OrgProject project, NewProjectSession nps) {
        CourseDesignClient cdClient = Clients.getClient(cycle, CourseDesignClient.class);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        long caller = LoginUserAccountHelper.getUserId(cycle);

        final CatalogCourseId courseId = CatalogCourseId.valueOf(nps.getCourseId());
        final CatalogCourse course =  CollectionsUtil.singleItemOrNull(ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build()));
        if(course == null) {
            return;
        }

        new CreateCatalogCourseSessionCmd(cycle, caller).run(project, course);
    }

    private void addProjectProduct(RequestCycle cycle, ProjectMaterialCoordinatorClient pmcClient,
                                   OrgProject project, String prodId, boolean retryAttempt,
                                   Map<String, String> productExtraSettings)
            throws DeniedException, ProjectProductException {

        try {
            pmcClient.addProjectProduct(project.getProjectId(), prodId, productExtraSettings);
        } catch (MissingProjectProductException ex) {
            ProductDirectoryClient pdClient
                    = CacheClients.getClient(cycle, ProductDirectoryClient.class);

            Product product = ProductFetchUtil.getProduct(pdClient, prodId);
            if (!retryAttempt && product != null && product.isAnonymous() && product.
                    isRealmProduct()) {

                long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
                getCocoboxCordinatorClient(cycle).addOrgProduct(caller, orgId, prodId);
                addProjectProduct(cycle, pmcClient, project, prodId, true, productExtraSettings);
            } else {
                addProjectProductError(prodId, project, ex, cycle);
            }
        } catch (Exception ex) {
            addProjectProductError(prodId, project, ex, cycle);
        }
    }

    private void addProjectProductError(String prodId, OrgProject project, Exception ex,
                                        RequestCycle cycle) {
        LOGGER.warn("Unable to add product {} to project {}",
                prodId, project.getProjectId(), ex);
        if (failures == null) {
            failures = new ArrayList<>();
        }
        ProjectProductFailure failure
                = new ProjectProductFailureFactory(cycle).newFailure(prodId, ex);
        failures.add(failure);
    }

    private void addProjectOrgMat(ProjectMaterialCoordinatorClient pmcClient, OrgProject project,
                                  Long orgMatId) {
        try {
            pmcClient.addProjectOrgMaterial(project.getProjectId(), orgMatId);
        } catch (Exception ex) {
            LOGGER.warn("Unable to add orgmat {} to project {}",
                    project.getProjectId(), orgMatId, ex);
        }
    }

    private ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient(RequestCycle cycle) {
        return CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
    }

    private void updateRecentTimezone(RequestCycle cycle, NewProjectRequest npr) {
        new RecentTimezoneUpdateCommand(cycle).
                updateRecentTimezone(npr.getOrgId(), npr.getTimezone());
    }

    private Long getDefaultExpiration(RequestCycle cycle, long newDesignId) {
        return new GetCourseDefaultExpiration().getDefaultExpiration(cycle, newDesignId);
    }

    private List<ExtraProductConfig> getExtraConfigItems(RequestCycle cycle, NewProjectSession nps) {

        List<Product> products
                = getProjectProducts(cycle, nps);

        return new ProductsExtraConfigFactory(cycle, orgId).getExtraConfigItems(products);
    }

    private boolean missingExtraParameters(RequestCycle cycle, NewProjectSession nps) {

        if (nps.getExtraConfig() == null) {
            List<ExtraProductConfig> items = getExtraConfigItems(cycle, nps);
            nps.setExtraConfig(items);
        }

        for (ExtraProductConfig extraConfig : nps.getExtraConfig()) {
            if (extraConfig.getProjectConfig() != null && extraConfig.getSettings() == null) {
                return true;
            }
        }

        return false;
    }

    private RequestTarget toNewProductExtraSettingsPage(String npsId) {
        return new WebModuleRedirectRequestTarget(NewProjectModule.class, "productExtraSettings", Long.toString(orgId), npsId);
    }

    private List<Product> getProjectProducts(RequestCycle cycle, NewProjectSession nps) throws CodecException, ClientFactoryException {

        if (nps.getDesignId() == null) {
            return getOrgMatProjectProducts(cycle, nps);
        } else {
            return getDesignProjectProducts(cycle, nps);
        }
    }

    private List<Product> getOrgMatProjectProducts(RequestCycle cycle, NewProjectSession nps) {
        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        List<Product> products = pdClient.getProducts(nps.getProds());

        ProductTypes types = pdClient.listTypes();
        types.populateProductType(products);

        return products;
    }

    private List<Product> getDesignProjectProducts(RequestCycle cycle, NewProjectSession nps) {
        CourseDesignClient cdClient
                = CacheClients.getClient(cycle, CourseDesignClient.class);
        CourseDesign design = cdClient.getDesign(nps.getDesignId());

        if (design == null) {
            LOGGER.warn("Unable to find course design {}", nps.getDesignId());
            return Collections.emptyList();
        }

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<ProductId> productIds = cdd.getAllProductIdSet();

        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        List<Product> products = pdClient.getProductsByIds(productIds);

        ProductTypes types = pdClient.listTypes();
        types.populateProductType(products);

        return products;
    }
}
