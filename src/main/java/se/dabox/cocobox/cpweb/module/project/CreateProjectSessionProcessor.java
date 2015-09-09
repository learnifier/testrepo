/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.command.RecentTimezoneUpdateCommand;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.service.common.coursedesign.techinfo.CpDesignTechInfo;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailure;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailureFactory;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.cocobox.cpweb.state.NewProjectSessionProcessor;
import se.dabox.cocobox.crisp.datasource.OrgUnitSource;
import se.dabox.cocobox.crisp.datasource.PdProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProductInfoSource;
import se.dabox.cocobox.crisp.datasource.StandardOrgUnitInfoSource;
import se.dabox.cocobox.crisp.method.GetProjectConfiguration;
import se.dabox.cocobox.crisp.response.ProjectConfigResponse;
import se.dabox.cocobox.crisp.response.json.ProjectConfigResponseJson;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocobox.crisp.runtime.DwsCrispExecutionHelper;
import se.dabox.cocosite.druwa.CocoSiteConfKey;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
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
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.proddir.data.ProductTypes;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CreateProjectSessionProcessor implements NewProjectSessionProcessor {

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
                addProjectProduct(cycle, pmcClient, project, prodId, false);
            }
        }

        if (nps.getDesignId() != null && nps.getDesignId().longValue() != 0) {
            activateProjectDesign(cycle, project, nps);
        }

        cycle.getSession().removeAttribute(NewProjectSession.getSessionName(
                strNpsId));

        updateRecentTimezone(cycle, npr);

        if (failures != null) {
            cycle.getSession().setFlashAttribute(ProjectProductFailure.VIEWSESSION_NAME, failures);
        }

        if (ptype == ProjectType.DESIGNED_PROJECT) {
            return new WebModuleRedirectRequestTarget(VerifyProjectDesignModule.class,
                    VerifyProjectDesignModule.ACTION_VERIFY_NEW_DESIGN,
                    Long.toString(project.getProjectId()));
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

    private void addProjectProduct(RequestCycle cycle, ProjectMaterialCoordinatorClient pmcClient,
            OrgProject project, String prodId, boolean retryAttempt)
            throws DeniedException, ProjectProductException {

        try {
            pmcClient.addProjectProduct(project.getProjectId(), prodId);
        } catch (MissingProjectProductException ex) {
            ProductDirectoryClient pdClient
                    = CacheClients.getClient(cycle, ProductDirectoryClient.class);

            Product product = ProductFetchUtil.getProduct(pdClient, prodId);
            if (!retryAttempt && product != null && product.isAnonymous() && product.
                    isRealmProduct()) {

                long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
                getCocoboxCordinatorClient(cycle).addOrgProduct(caller, orgId, prodId);
                addProjectProduct(cycle, pmcClient, project, prodId, true);
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

        final List<ExtraProductConfig> configList = new ArrayList<>();

        CourseDesignClient cdClient
                = CacheClients.getClient(cycle, CourseDesignClient.class);

        CourseDesign design = cdClient.getDesign(nps.getDesignId());

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<ProductId> productIds = cdd.getAllProductIdSet();

        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        List<Product> products = pdClient.getProductsByIds(productIds);
        ProductTypes types = pdClient.listTypes();
        types.populateProductType(products);

        List<Product> crispProducts
                = CollectionsUtil.sublist(products, p -> ProductUtils.isCrispProduct(p));

        for (Product product : crispProducts) {
            CrispContext ctx = DwsCrispContextHelper.getCrispContext(cycle, product);

            if (ctx.getDescription().getMethods().getGetProjectConfiguration() == null) {
                continue;
            }

            OrgUnitInfo ou = CacheClients.getClient(cycle,
                    OrganizationDirectoryClient.class).getOrgUnitInfo(orgId);
            OrgUnitSource orgUnit = new StandardOrgUnitInfoSource(ou);

            ProductInfoSource productInfo = new PdProductInfoSource(product);

            GetProjectConfiguration config = GetProjectConfiguration.
                    newCreateGetProjectConfiguration(orgUnit, productInfo);

            DwsCrispExecutionHelper execHelper = new DwsCrispExecutionHelper(cycle, ctx);

            ProjectConfigResponse response = null;
            try {
                response = execHelper.executeRequest(Locale.ENGLISH, config,
                        new ProjectConfigResponseJson());
            } catch (CrispException crispException) {
                ErrorState state = new ErrorState(orgId, product, crispException, null);
                throw new RetargetException(NavigationUtil.getIntegrationErrorPage(
                        cycle, state));
            }

            if (response.isEmpty()) {
                continue;
            }

            configList.add(new ExtraProductConfig(product.getId().getId(), response));
        }

        return configList;
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
}
