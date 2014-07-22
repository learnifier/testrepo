/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import se.dabox.cocosite.project.UpdateRecentProjectList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.coursebuilder.initdata.InitData;
import se.dabox.cocobox.coursebuilder.initdata.InitDataBuilder;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.account.ChangePassword;
import se.dabox.cocobox.cpweb.formdata.project.AddMaterialForm;
import se.dabox.cocobox.cpweb.formdata.project.AddMemberForm;
import se.dabox.cocobox.cpweb.formdata.project.AddTaskForm;
import se.dabox.cocobox.cpweb.formdata.project.SetRegCreditLimitForm;
import se.dabox.cocobox.cpweb.formdata.project.SetRegPasswordForm;
import se.dabox.cocobox.cpweb.formdata.project.UploadRosterForm;
import se.dabox.cocobox.cpweb.module.OrgMaterialJsonModule;
import se.dabox.cocobox.cpweb.module.coursedesign.DesignTechInfo;
import se.dabox.cocobox.cpweb.module.coursedesign.GotoDesignBuilder;
import se.dabox.cocobox.cpweb.module.mail.TemplateLists;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.coursedesign.GetProjectCourseDesignCommand;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.cocosite.selfreg.GetProjectSelfRegLink;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectSubtypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.UpdateProjectRequest;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.ccbc.project.material.ProjectProductMaterialHelper;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.UpdateDesignRequest;
import se.dabox.service.common.coursedesign.activity.MultiPageActivityCourse;
import se.dabox.service.common.coursedesign.activity.MultiPageCourseCddActivityCourseFactory;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.CourseDesignInfo;
import se.dabox.service.common.coursedesign.v1.CourseDesignXmlMutator;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project")
public class ProjectModule extends AbstractProjectWebModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectModule.class);
    public static final String OVERVIEW_ACTION = "overview";
    public static final String ROSTER_ACTION = "roster";
    public static final String TASK_ACTION = "task";
    public static final String MATERIAL_ACTION = "materials";

    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/projectOverview.html", map);
    }

    @DefaultWebAction
    @WebAction
    public RequestTarget onRoster(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(AddMemberForm.class, cycle));

        map.put("rosterformsess", getValidationSession(UploadRosterForm.class, cycle));
        map.put("uploadRosterFormLink", cycle.urlFor(ProjectModificationModule.class,
                ProjectModificationModule.UPLOAD_ROSTER_ACTION,
                projectId));
        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/projectRoster.html", map);
    }

    private OrgProject getProject(RequestCycle cycle, String projectId) throws NumberFormatException {
        final CocoboxCordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient(cycle);
        OrgProject project =
                cocoboxCordinatorClient.getProject(Long.valueOf(projectId));
        
        if (project == null) {
            LOGGER.info("Project {} missing. Redirecting to cpweb main page", projectId);
            throw new RetargetException(NavigationUtil.toMain(cycle));
        }

        if (WebFeatures.getFeatures(cycle).hasFeature(CocositeWebFeatureConstants.FLIRT)) {
            if (project.getFlirtId() == null || project.getNewsFlirtId() == null) {
                LOGGER.debug("Flirt ids are missing from project. Resyncing project");
                cocoboxCordinatorClient.syncProjectState(project.getProjectId());
                project = cocoboxCordinatorClient.getProject(project.getProjectId());
            }
        }

        new UpdateRecentProjectList(cycle).addProject(project.getProjectId());

        return project;
    }

    @WebAction
    public RequestTarget onRegistration(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();
        DruwaFormValidationSession<SetRegPasswordForm> pwformsess = getPwFormsess(cycle, project);
        DruwaFormValidationSession<SetRegCreditLimitForm> credlimsess =
                getCredLimitFormsess(cycle, project);

        map.put("passwordformsess", pwformsess);
        map.put("creditlimitformsess", credlimsess);
        addCommonMapValues(map, project, cycle);
        map.put("reglink", getProjectRegistrationLink(cycle, project));


        return new FreemarkerRequestTarget("/project/projectRegistration.html", map);
    }

    @WebAction
    public RequestTarget onMaterials(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(AddMaterialForm.class, cycle));
        List<Material> materials = OrgMaterialJsonModule.getOrgMaterials(cycle, project.getOrgId(),
                project, null);
        map.put("materials", materials);
        addCommonMapValues(map, project, cycle);
        map.put("addLink", cycle.urlFor(ProjectMaterialModule.class, "addMaterial", projectId));
        map.put("removeLink", cycle.urlFor(ProjectMaterialModule.class, "removeMaterial",
                                projectId));

        return new FreemarkerRequestTarget("/project/projectMaterials.html", map);
    }

    @WebAction
    public RequestTarget onRaps(RequestCycle cycle, String projectId, String strParticipationId) {
        OrgProject project =
                getProject(cycle, projectId);

        long participationId = Long.parseLong(strParticipationId);

        checkPermission(cycle, project);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);

        List<ParticipationProgress> progress
                = CacheClients.getClient(cycle, CocoboxCordinatorClient.class).
                getParticipationProgress(participationId);
        DatabankFacade databankFacade = new GetDatabankFacadeCommand(cycle).get(project);
        CourseDesignDefinition cdd = new GetProjectCourseDesignCommand(cycle).forProject(project);
        
        MultiPageActivityCourse actCourse
                = new MultiPageCourseCddActivityCourseFactory().newActivityCourse(project, progress,
                        databankFacade, cdd);

        map.put("actCourse", actCourse);

        return new FreemarkerRequestTarget("/project/participationStatusRaw.html", map);
    }

    @WebAction
    public RequestTarget onDiscussion(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);


        List<ParticipationProgress> progress = Collections.emptyList();
        DatabankFacade databankFacade = new GetDatabankFacadeCommand(cycle).
                setFallbackToStageDatabank(true).get(project);
        CourseDesignDefinition cdd = new GetProjectCourseDesignCommand(cycle).
                setFallbackToStageDesign(true).forProject(project);

        MultiPageActivityCourse actCourse
                = new MultiPageCourseCddActivityCourseFactory().newActivityCourse(project, progress,
                        databankFacade, cdd);
        map.put("course", actCourse);

        return new FreemarkerRequestTarget("/project/projectDiscussion.html", map);
    }

    @WebAction
    public RequestTarget onDesign(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);


        return new FreemarkerRequestTarget("/project/projectDesign.html", map);
    }

    @WebAction
    public RequestTarget onGotoDesignBuilder(final RequestCycle cycle, String projectId) {
        final OrgProject project =
                getCocoboxCordinatorClient(cycle).getProject(Long.valueOf(projectId));

        checkPermission(cycle, project);

        long designId;

        designId = ProjectTypeUtil.callSubtype(project, new ProjectSubtypeCallable<Long>() {
            @Override
            public Long callMainProject() {
                if (project.getStageDesignId() != null) {
                    return project.getStageDesignId();
                } else if (project.getDesignId() != null) {
                    return copyDesignToStage(cycle, project);
                } else {
                    throw new IllegalStateException("No design for this project: " + project);
                }
            }

            @Override
            public Long callIdProjectProject() {
                return project.getDesignId();
            }
        });

        long brandingId =
                new GetOrgBrandingIdCommand(cycle).forOrg(project.getOrgId());

        String backUrl = NavigationUtil.toProjectPageUrl(cycle, project.getProjectId());

        legacyDesignTitleHandling(cycle, project, designId);

        InitData initData = new InitDataBuilder().setProjectInitData(
                project.getOrgId(),
                brandingId,
                project.getName(),
                designId,
                project.getProjectId(),
                backUrl).createInitData();

        return new RedirectUrlRequestTarget(GotoDesignBuilder.process(cycle, initData));
    }

    @WebAction
    public RequestTarget onViewDesign(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        long designId = project.getLatestDesignId();

        long brandingId =
                new GetOrgBrandingIdCommand(cycle).forOrg(project.getOrgId());

        String backUrl = NavigationUtil.toProjectPageUrl(cycle, project.getProjectId());

        InitData initData = new InitDataBuilder().setProjectInitData(
                project.getOrgId(),
                brandingId,
                project.getName(),
                designId,
                project.getProjectId(),
                backUrl).
                setReadOnly(true).
                createInitData();

        return new RedirectUrlRequestTarget(GotoDesignBuilder.process(cycle, initData));
    }

    @WebAction
    public RequestTarget onTask(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);
        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        long mailBucket = new GetOrgMailBucketCommand(cycle).forOrg(project.
                getOrgId());

        map.put("formsess", getValidationSession(AddTaskForm.class, cycle));
        map.put("templateLists", getLists(cycle, mailBucket));
        addCommonMapValues(map, project, cycle);
        map.put("formlink", cycle.urlFor(ProjectModificationModule.class,
                ProjectModificationModule.ADD_TASK, projectId));

        return new FreemarkerRequestTarget("/project/projectSchedule.html", map);
    }

    @WebAction
    public RequestTarget onSettings(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(ChangePassword.class, cycle));
        map.put("formLink", "");
        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/projectSettings.html", map);
    }

    @WebAction
    public RequestTarget onRoles(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        map.put("projectRoles", new CocoboxRoleUtil().getProjectRoles(cycle));
        map.put("userAccount", LoginUserAccountHelper.getUserAccount(cycle));

        return new FreemarkerRequestTarget("/project/projectRoles.html", map);
    }

    @WebAction
    public RequestTarget onTmpRoles(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        map.put("projectRoles", new CocoboxRoleUtil().getProjectRoles(cycle));
        map.put("userAccount", LoginUserAccountHelper.getUserAccount(cycle));

        return new FreemarkerRequestTarget("/project/tmpProjectRoles.html", map);
    }

    @WebAction
    public RequestTarget onReports(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        List<Material> materials
                = ProjectProductMaterialHelper.getProjectMaterials(cycle, userLocale, project);

        map.put("materials", materials);
        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/projectReports.html", map);
    }
    
    @WebAction
    public RequestTarget onEdit(RequestCycle cycle, String projectId) {
        throw new UnsupportedOperationException("Not supported anymore");
    }

    @WebAction
    public RequestTarget onCrispAdmin(RequestCycle cycle, String strProjectId, String productId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        try {
            String link = pmcClient.getCrispProductAdminLink(userId, prjId, productId);
            return new RedirectUrlRequestTarget(link);
        } catch (Exception ex) {
            Product product = getProductDirectoryClient(cycle).getProduct(productId);
            ErrorState state = new ErrorState(prj.getOrgId(), product, ex, prjId);
            return NavigationUtil.getIntegrationErrorPage(cycle, state);
        }
    }

    private TemplateLists getLists(RequestCycle cycle, long mailBucket) {
        MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);

        List<MailTemplate> templates =
                mtClient.getBucketMailTemplates(mailBucket);

        return new TemplateLists(cycle.getResponse().getLocale()).
                addTemplates(templates);
    }

    private long copyDesignToStage(RequestCycle cycle, OrgProject project) {
        CourseDesignClient cdClient =
                Clients.getClient(cycle, CourseDesignClient.class);

        String techInfo = DesignTechInfo.createStageTechInfo(project.getProjectId());

        long userId = LoginUserAccountHelper.getUserId(cycle);
        long stageId = cdClient.copyDesign(project.getDesignId(), userId, techInfo);

        CocoboxCordinatorClient ccbc =
                Clients.getClient(cycle, CocoboxCordinatorClient.class);

        project.setStageDesignId(stageId);

        Long stageDatabank = project.getStageDatabank();
        if (stageDatabank == null) {
            if (project.getMasterDatabank() != null) {
                stageDatabank = ccbc.createCopyDatabank(userId, project.getMasterDatabank());
            } else {
                stageDatabank = ccbc.createDatabank(userId, project.getProjectId());
            }
        }

        ccbc.updateOrgProject(new UpdateProjectRequest(project.getProjectId(), project.getName(),
                project.getLocale(), userId, project.getCountry(), project.getTimezone(), project.
                getDesignId(), project.getStageDesignId(), project.getMasterDatabank(),
                stageDatabank, project.getNote(), project.getInvitePassword(), project.
                getInviteLimit(), project.isInvitePossible(), project.getUserTitle(), project.
                getUserDescription(),project.isAutoIcal(), project.isSocial()));

        project.setStageDatabank(stageDatabank);


        return stageId;
    }

    private DruwaFormValidationSession<SetRegCreditLimitForm> getCredLimitFormsess(
            RequestCycle cycle, OrgProject project) {
        DruwaFormValidationSession<SetRegCreditLimitForm> formsess =
                getValidationSession(SetRegCreditLimitForm.class, cycle);

        SetRegCreditLimitForm form =
                new SetRegCreditLimitForm();
        form.setCreditLimitEnabled(project.getInviteLimit() != null);
        form.setCreditLimit(project.getInviteLimit());

        formsess.populateFromObject(form);

        return formsess;
    }

    private DruwaFormValidationSession<SetRegPasswordForm> getPwFormsess(RequestCycle cycle,
            OrgProject project) {
        DruwaFormValidationSession<SetRegPasswordForm> formsess =
                getValidationSession(SetRegPasswordForm.class, cycle);

        SetRegPasswordForm form = new SetRegPasswordForm();
        form.setPassword(project.getInvitePassword());
        form.setPasswordEnabled(project.getInvitePassword() != null);

        formsess.populateFromObject(form);

        return formsess;
    }

    private String getProjectRegistrationLink(RequestCycle cycle, OrgProject project) {
        return new GetProjectSelfRegLink(cycle).forProject(project);
    }

    private void legacyDesignTitleHandling(RequestCycle cycle, OrgProject project, long designId) {
        CourseDesignClient cdClient =
                CacheClients.getClient(cycle, CourseDesignClient.class);

        CourseDesign design = cdClient.getDesign(designId);

        CourseDesignDefinition cdd =
                CddCodec.decode(cycle, design.getDesign());

        CourseDesignInfo info = cdd.getInfo();
        CourseDesignInfo newInfo = info;

        if (cdd.getInfo().getUserTitle() == null) {
            newInfo = newInfo.withUserTitle(project.getDisplayUserTitle());
        }

        if (cdd.getInfo().getUserDescription() == null) {
            newInfo = newInfo.withUserDescription(project.getDisplayUserDescription());
        }

        //No change
        if (info.equals(newInfo)) {
            return;
        }

        LOGGER.
                info(
                "Design {} had lacked the correct userTitle and userDescription. Information from project {} copied.",
                designId, project.getProjectId());

        CourseDesignXmlMutator mutator = new CourseDesignXmlMutator(design.getDesign());
        mutator.setInfo(newInfo);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateDesignRequest udr = new UpdateDesignRequest(designId, userId, mutator.toXmlString());
        cdClient.updateDesign(udr);
    }
    
}
