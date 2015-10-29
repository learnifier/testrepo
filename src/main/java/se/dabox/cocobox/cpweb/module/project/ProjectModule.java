/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.StringRequestTarget;
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
import se.dabox.service.common.coursedesign.techinfo.CpDesignTechInfo;
import se.dabox.cocobox.cpweb.module.coursedesign.GotoDesignBuilder;
import se.dabox.cocobox.cpweb.module.mail.TemplateLists;
import se.dabox.cocobox.cpweb.module.util.ProductNameMapFactory;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.coursedesign.GetProjectCourseDesignCommand;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.role.CocoboxRoleUtil;
import se.dabox.cocosite.selfreg.GetProjectSelfRegLink;
import se.dabox.cocosite.upweb.linkaction.ImpersonateParticipationLinkAction;
import se.dabox.cocosite.upweb.linkaction.LinkActionUrlHelper;
import se.dabox.cocosite.upweb.linkaction.cpreview.CoursePreviewLinkAction;
import se.dabox.cocosite.upweb.linkaction.cpreview.PreviewParticipationSource;
import se.dabox.cocosite.upweb.linkaction.cpreview.ProjectCddSource;
import se.dabox.cocosite.upweb.linkaction.cpreview.ProjectDatabankSource;
import se.dabox.cocosite.upweb.linkaction.cpreview.RealProjectSource;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
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
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignInfo;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.common.material.Material;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project")
public class ProjectModule extends AbstractProjectWebModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectModule.class);
    public static final String OVERVIEW_ACTION = "overview";
    public static final String ROSTER_ACTION = "roster";
    public static final String TASK_ACTION = "task";
    public static final String MATERIAL_ACTION = "materials";
    public static final String ROLES_ACTION = "roles";
    public static final String CREATE_TASK_ACTION = "createTask";

    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

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
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(AddMemberForm.class, cycle));

        map.put("rosterformsess", getValidationSession(UploadRosterForm.class, cycle));
        map.put("uploadRosterFormLink", cycle.urlFor(ProjectModificationModule.class,
                ProjectModificationModule.UPLOAD_ROSTER_ACTION,
                projectId));
        addCommonMapValues(map, project, cycle);
        initSelfReg(cycle, project, map);

        return new FreemarkerRequestTarget("/project/projectRoster.html", map);
    }
    
     @WebAction
    public RequestTarget onKevin(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(AddMemberForm.class, cycle));

        map.put("rosterformsess", getValidationSession(UploadRosterForm.class, cycle));
        map.put("uploadRosterFormLink", cycle.urlFor(ProjectModificationModule.class,
                ProjectModificationModule.UPLOAD_ROSTER_ACTION,
                projectId));
        addCommonMapValues(map, project, cycle);
        initSelfReg(cycle, project, map);

        return new FreemarkerRequestTarget("/project/kevin.html", map);
    }
    
    @WebAction
    public RequestTarget onDetails(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/projectDetails.html", map);
    }
    

    public void initSelfReg(RequestCycle cycle, OrgProject project, Map<String,Object> map) {
        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT_SELFREG);

        DruwaFormValidationSession<SetRegPasswordForm> pwformsess = getPwFormsess(cycle, project);
        DruwaFormValidationSession<SetRegCreditLimitForm> credlimsess =
                getCredLimitFormsess(cycle, project);

        map.put("passwordformsess", pwformsess);
        map.put("creditlimitformsess", credlimsess);
        map.put("reglink", getProjectRegistrationLink(cycle, project));

    }

    @WebAction
    public RequestTarget onMaterials(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

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
    public RequestTarget onAddProjectProductWithSettings(final RequestCycle cycle,
            String strProjectId, String strProductId) {

        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);

        checkPermission(cycle, prj);
        checkProjectPermission(cycle, prj, CocoboxPermissions.CP_CREATE_PROJECT_MATERIAL);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(AddMaterialForm.class, cycle));
        addCommonMapValues(map, prj, cycle);

        GetCrispProjectProductConfig response
                = new GetCrispProjectProductConfig(cycle, prj.getOrgId(), strProductId);

        final ExtraProductConfig extraProductConfig
                = new ExtraProductConfig(strProductId, response.get());

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        map.put("productConfig", extraProductConfig);
        map.put("productValueSource",
                new ProductsValueSource(userLocale, Collections.singletonList(extraProductConfig)));

        map.put("project", prj);
        map.put("productId", strProductId);
        map.put("productNameMap", new ProductNameMapFactory().
                create(Collections.singletonList(extraProductConfig)));

        return new FreemarkerRequestTarget("/project/addProjectProductWithSettings.html", map);
    }

    @WebAction
    public RequestTarget onEditProjectProductSettings(final RequestCycle cycle,
            String strProjectId, String strProductId) {

        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);

        checkPermission(cycle, prj);
        checkProjectPermission(cycle, prj, CocoboxPermissions.CP_EDIT_PROJECT);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(AddMaterialForm.class, cycle));
        addCommonMapValues(map, prj, cycle);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        GetCrispProjectProductConfig response
                = new GetCrispProjectProductConfig(cycle, prj, strProductId);

        final ExtraProductConfig extraProductConfig
                = new ExtraProductConfig(strProductId, response.get());

        map.put("productConfig", extraProductConfig);
        map.put("productValueSource",
                new ProductsValueSource(userLocale, Collections.singletonList(extraProductConfig)));

        map.put("project", prj);
        map.put("productId", strProductId);
        map.put("productNameMap", new ProductNameMapFactory().
                create(Collections.singletonList(extraProductConfig)));

        return new FreemarkerRequestTarget("/project/editProjectProductSettings.html", map);
    }

    /**
     * Shows technical information about the user
     *
     */
    @WebAction
    public RequestTarget onRaps(RequestCycle cycle, String projectId, String strParticipationId) {

        if (projectId == null || strParticipationId == null) {
            return new ErrorCodeRequestTarget(404);
        }

        OrgProject project =
                getProject(cycle, projectId);

        if (project == null) {
            return new StringRequestTarget("Project not found");
        }

        long participationId = Long.parseLong(strParticipationId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);

        List<ParticipationProgress> progress = null;
        try {
            progress
                    = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class).
                    getParticipationProgress(participationId);
        } catch (NotFoundException notFoundException) {
            Collections.emptyList();
        }

        DatabankFacade databankFacade = new GetDatabankFacadeCommand(cycle).get(project);
        CourseDesignDefinition cdd = new GetProjectCourseDesignCommand(cycle).forProject(project);
        
        ProjectParticipationState state
                = getCocoboxCordinatorClient(cycle).getParticipationState(participationId);

        MultiPageActivityCourse actCourse
                = new MultiPageCourseCddActivityCourseFactory().
                setAllowTemporalProgressTracking(true).
                newActivityCourse(project, progress, databankFacade, cdd);

        map.put("actCourse", actCourse);
        map.put("participantState", state);

        return new FreemarkerRequestTarget("/project/participationStatusRaw.html", map);
    }

    @WebAction
    public RequestTarget onDiscussion(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

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
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);


        return new FreemarkerRequestTarget("/project/projectDesign.html", map);
    }

    @WebAction
    public RequestTarget onGotoDesignBuilder(final RequestCycle cycle, String projectId) {
        final OrgProject project =
                getCocoboxCordinatorClient(cycle).getProject(Long.valueOf(projectId));

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_EDIT_PROJECT_COURSEDESIGN);

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

            @Override
            public Long callChallengeProject() {
                return project.getDesignId();
            }

            @Override
            public Long callLinkedSubproject() {
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
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

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
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

        long mailBucket = new GetOrgMailBucketCommand(cycle).forOrg(project.
                getOrgId());

        map.put("formsess", getValidationSession(AddTaskForm.class, cycle));
        map.put("templateLists", getLists(cycle, mailBucket));
        addCommonMapValues(map, project, cycle);        

        return new FreemarkerRequestTarget("/project/projectSchedule.html", map);
    }

    @WebAction
    public RequestTarget onCreateTask(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);
        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

        long mailBucket = new GetOrgMailBucketCommand(cycle).forOrg(project.
                getOrgId());

        map.put("formsess", getValidationSession(AddTaskForm.class, cycle));
        map.put("templateLists", getLists(cycle, mailBucket));
        addCommonMapValues(map, project, cycle);
        map.put("formlink", cycle.urlFor(ProjectModificationModule.class,
                ProjectModificationModule.ADD_TASK, projectId));

        return new FreemarkerRequestTarget("/project/projectCreateSchedule.html", map);
    }

    @WebAction
    public RequestTarget onSettings(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_EDIT_PROJECT);

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
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        map.put("projectRoles", new CocoboxRoleUtil().getProjectRoles(cycle));
        map.put("userAccount", LoginUserAccountHelper.getUserAccount(cycle));

        return new FreemarkerRequestTarget("/project/projectRoles.html", map);
    }

    @WebAction
    public RequestTarget onReports(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

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

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        try {
            String link = pmcClient.getCrispProductAdminLink(userId, prjId, productId);
            return new RedirectUrlRequestTarget(link);
        } catch (Exception ex) {
            Product product = getProductDirectoryClient(cycle).getProduct(productId);
            ErrorState state = new ErrorState(project.getOrgId(), product, ex, prjId);
            return NavigationUtil.getIntegrationErrorPage(cycle, state);
        }
    }

    @WebAction
    public RequestTarget onImpersonate(RequestCycle cycle, String strParticipationId) {
        ProjectParticipation part
                = getCocoboxCordinatorClient(cycle).getProjectParticipation(Long.valueOf(
                                strParticipationId));
        OrgProject project = getProject(cycle, Long.toString(part.getProjectId()));
        checkProjectPermission(cycle, project, CocoboxPermissions.PRJ_IMPERSONATE_PARTICIPANT);

        long caller = LoginUserAccountHelper.getCurrentCaller();

        ImpersonateParticipationLinkAction action = new ImpersonateParticipationLinkAction(
                caller,
                part.getParticipationId());

        String url = LinkActionUrlHelper.getUrl(cycle, action);
        
        return new RedirectUrlRequestTarget(url);
    }

    @WebAction
    public RequestTarget onPreviewDesign(RequestCycle cycle, String projectId) {
        OrgProject project =
                getProject(cycle, projectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);

        RealProjectSource projSource = new RealProjectSource(project.getProjectId());
        ProjectCddSource cddSource = new ProjectCddSource(project.getProjectId());

        CoursePreviewLinkAction action = new CoursePreviewLinkAction();
        action.setCddSource(cddSource);
        action.setProjectSource(projSource);
        action.setParticipationSource(new PreviewParticipationSource());
        action.setDatabankSource(new ProjectDatabankSource(project.getProjectId()));

        if (!action.isValid()) {
            throw new IllegalStateException("CoursePreviewLinkAction is not setup correctly");
        }
        
        String url = LinkActionUrlHelper.getUrl(cycle, action);

        return new RedirectUrlRequestTarget(url);
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

        String techInfo = CpDesignTechInfo.createStageTechInfo(project.getProjectId());

        long userId = LoginUserAccountHelper.getUserId(cycle);
        long stageId = cdClient.copyDesign(project.getDesignId(), userId, techInfo);

        CocoboxCoordinatorClient ccbc =
                Clients.getClient(cycle, CocoboxCoordinatorClient.class);

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
            newInfo
                    = new MutableCourseDesignInfo(newInfo).withUserTitle(project.getDisplayUserTitle()).
                    toCourseDesignInfo();
        }

        if (cdd.getInfo().getUserDescription() == null) {
            newInfo = new MutableCourseDesignInfo(newInfo).withUserDescription(project.
                    getDisplayUserDescription()).toCourseDesignInfo();
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
