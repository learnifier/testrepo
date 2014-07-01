/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.command.RecentTimezoneUpdateCommand;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.cocobox.cpweb.module.coursedesign.DesignTechInfo;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailure;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailureFactory;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.cocobox.cpweb.state.NewProjectSessionProcessor;
import se.dabox.cocosite.druwa.CocoSiteConfKey;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.DeniedException;
import se.dabox.service.common.ccbc.project.NewProjectRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectProductException;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.UpdateProjectRequest;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CreateProjectSessionProcessor implements NewProjectSessionProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateProjectSessionProcessor.class);
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
                        ParamUtil.required(matListDetails,"matListDetails");
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

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);
        AlreadyExistsException aex = null;
        for (int i = 0; i < 20; i++) {
            try {
                aex = null;
                if (i != 0) {
                    npr = npr.withName(String.format("%s (%d)", projectName, i));
                }

                project = ccbc.newProject(npr);
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
                addProjectProduct(cycle, pmcClient, project, prodId);
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

    private CocoboxCordinatorClient getCocoboxCordinatorClient(RequestCycle cycle) {
        return Clients.getClient(cycle, CocoboxCordinatorClient.class);
    }

    private RequestTarget toProjectPage(long projectId) {
        return NavigationUtil.toProjectPage(projectId);
    }

    private void activateProjectDesign(RequestCycle cycle, OrgProject project, NewProjectSession nps) {
        CourseDesignClient cdClient =
                Clients.getClient(cycle, CourseDesignClient.class);

        long userId = LoginUserAccountHelper.getUserId(cycle);

        String techInfo = DesignTechInfo.createStageTechInfo(project.getProjectId());

        long newDesignId = cdClient.copyDesign(nps.getDesignId(), userId, techInfo);
        long newDatabank = getCocoboxCordinatorClient(cycle).createDatabank(userId, project.
                getProjectId());

        project.setStageDesignId(newDesignId);
        project.setStageDatabank(newDatabank);

        CourseDesign design = cdClient.getDesign(newDesignId);
        CourseDesignDefinition cdd =
                CddCodec.decode(cycle, design.getDesign());

        String userTitle = cdd.getInfo().getUserTitle();
        String userDesc = cdd.getInfo().getUserDescription();

        UpdateProjectRequest upr =
                new UpdateProjectRequest(project.getProjectId(), project.getName(), project.
                getLocale(), userId, project.getCountry(), project.getTimezone(), null, newDesignId,
                null, newDatabank, project.getNote(), project.getInvitePassword(), project.
                getInviteLimit(), project.isSelfRegistrationEnabled(), userTitle, userDesc, project.
                isAutoIcal(), project.isSocial());

        getCocoboxCordinatorClient(cycle).updateOrgProject(upr);
    }

    private void addProjectProduct(RequestCycle cycle, ProjectMaterialCoordinatorClient pmcClient,
            OrgProject project, String prodId)
            throws DeniedException, ProjectProductException {

        try {
            pmcClient.addProjectProduct(project.getProjectId(), prodId);
        } catch (Exception ex) {
            LOGGER.warn("Unable to add product {} to project {}",
                    project.getProjectId(), prodId, ex);
            if (failures == null) {
                failures = new ArrayList<>();
            }
            ProjectProductFailure failure
                    = new ProjectProductFailureFactory(cycle).newFailure(prodId, ex);
            failures.add(failure);
        }
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
}
