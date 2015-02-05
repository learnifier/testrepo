/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import net.unixdeveloper.druwa.module.WebModuleInfo;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.excel.Contact;
import se.dabox.cocobox.cpweb.excel.RosterError;
import se.dabox.cocobox.cpweb.excel.RosterException;
import se.dabox.cocobox.cpweb.excel.RosterReader;
import se.dabox.cocobox.cpweb.formdata.project.AddMemberForm;
import se.dabox.cocobox.cpweb.formdata.project.AddTaskForm;
import se.dabox.cocobox.cpweb.formdata.project.UploadRosterForm;
import se.dabox.cocobox.cpweb.model.project.task.CreateMailTaskSendMailProcessor;
import se.dabox.cocobox.cpweb.model.project.task.EditMailTaskSendMailProcessor;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocobox.cpweb.module.mail.RequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.mail.UrlRequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.project.role.AssignRoleCommand;
import se.dabox.cocobox.cpweb.module.project.roster.ActivateParticipant;
import se.dabox.cocobox.cpweb.module.project.roster.PermissionListformCommand;
import se.dabox.cocobox.cpweb.module.project.roster.ProjectParticipantSendMail;
import se.dabox.cocobox.cpweb.module.project.roster.RosterDeleteParticipant;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.modal.ModalParamsHelper;
import se.dabox.cocosite.module.core.AbstractCocositeJsModule;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.cocosite.security.Permission;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.dws.client.DwsServiceErrorCodeException;
import se.dabox.dws.client.langservice.LangBundle;
import se.dabox.service.client.ClientFactoryException;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.OverlimitException;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectTask;
import se.dabox.service.common.ccbc.project.role.ProjectUserRoleModification;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplateCodec;
import se.dabox.service.common.tx.OperationFailureException;
import se.dabox.service.common.tx.UTComplexTxOperation;
import se.dabox.service.common.tx.ValidationFailureException;
import se.dabox.service.common.tx.VerificationStatus;
import se.dabox.service.login.client.AlreadyExistsException;
import se.dabox.service.login.client.CocoboxUserAccount;
import se.dabox.service.login.client.CreateBasicUserAccountRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.druwa.FormbeanJsRequestTargetFactory;
import se.dabox.service.webutils.druwa.JsonRequestUtil;
import se.dabox.service.webutils.freemarker.text.LangServiceClientFactory;
import se.dabox.service.webutils.listform.ListformCommand;
import se.dabox.service.webutils.listform.ListformContext;
import se.dabox.service.webutils.listform.LongListformProcessor;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.email.SimpleEmailValidator;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.mod")
public class ProjectModificationModule extends AbstractJsonAuthModule {

    public static final String ADD_TASK = "addTask";
    public static final String EDIT_TASK = "editTask";
    public static final String UPLOAD_ROSTER_ACTION = "uploadRoster";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectModificationModule.class);
    private final LongListformProcessor rosterProcessor;

    public ProjectModificationModule() {
        rosterProcessor = new LongListformProcessor() {

            @Override
            protected void beforeProcess(ListformContext context, ListformCommand<Long> cmd) {
                super.beforeProcess(context, cmd);

                if (cmd instanceof PermissionListformCommand<?>) {
                    PermissionListformCommand<?> plc = (PermissionListformCommand<?>) cmd;
                    List<Permission> perms = plc.getPermissionsRequired();
                    RequestCycle cycle = context.getCycle();
                    OrgProject project = context.getAttribute("project", OrgProject.class);

                    for (Permission permission : perms) {
                        checkProjectPermission(cycle, project, permission);
                    }
                }
            }

        };
        rosterProcessor.addCommand("delete", new RosterDeleteParticipant());
        rosterProcessor.addCommand("send", new ProjectParticipantSendMail());
        rosterProcessor.addCommand("activate", new ActivateParticipant());
        rosterProcessor.addCommand("setExpiration", new SetParticipantExpiration());
        rosterProcessor.addCommand("adjustExpiration", new AdjustParticipantExpiration());
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onAddMember(final RequestCycle cycle, String projectId) {
        final CocoboxCoordinatorClient ccbcClient = getCocoboxCordinatorClient(
                cycle);

        final OrgProject prj = ccbcClient.getProject(Long.valueOf(
                projectId));
        checkPermission(cycle, prj);

        DruwaFormValidationSession<AddMemberForm> sess = getValidationSession(AddMemberForm.class,
                cycle);

        sess.setTransferToViewSession(true);

        if (!sess.process()) {
            return toRoster(projectId);
        }

        AddMemberForm form = sess.getObject();

        if (!SimpleEmailValidator.getInstance().isValidEmail(form.getMemberemail())) {
            sess.addError(new ValidationError(ValidationConstraint.EMAIL_FORMAT, "memberemail", "email.invalid.input"));
        }

        if (!sess.process()) {
            LOGGER.info("Invalid email: {}", form.getMemberemail());
            return toRoster(projectId);
        }

        try {
            
            addMember(cycle, prj, form.getMemberfirstname(), form.getMemberlastname(), form.
                    getMemberemail());
        } catch (ValidationFailureException vfe) {
            if (vfe.getVerificationStatus() == VerificationStatus.DUPLICATE) {
                sess.addError(new ValidationError(
                        ValidationConstraint.CONSISTENCY, "memberemail",
                        "duplicate"));
            } else {
                sess.addError(new ValidationError(
                        ValidationConstraint.INVALID, "memberemail",
                        "unknownerror"));
            }
        } catch (OperationFailureException opex) {
            LOGGER.warn("Unknown OperationFailureException", opex);

            sess.addError(new ValidationError(
                    ValidationConstraint.INVALID, "memberemail", "unknownerror"));
        } catch (OverlimitException ex) {
            LOGGER.debug("Overlimit reached", ex);

            sess.addError(new ValidationError(
                    ValidationConstraint.INVALID, "memberemail", "overlimit"));
        }

        return toRoster(projectId);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onListCommand(RequestCycle cycle) {

        final String strProjectId = cycle.getRequest().getParameter("projectId");

        return onListCommand(cycle, strProjectId);
    }

    public RequestTarget onListCommand(RequestCycle cycle, String strProjectId) {

        Long projectId = Long.valueOf(strProjectId);

        OrgProject prj = getCocoboxCordinatorClient(cycle).getProject(projectId);

        if (prj == null) {
            LOGGER.warn("Project not found: {}", strProjectId);
            return NavigationUtil.toProjectPage(projectId);
        }

        checkPermission(cycle, prj);

        ListformContext ctx = new ListformContext(cycle);
        ctx.setAttribute("project", prj);
        ctx.setAttribute("projectId", projectId);
        ctx.setAttribute("ccbcClient", getCocoboxCordinatorClient(cycle));

        RequestTarget resp = rosterProcessor.process(ctx);

        if (resp == null) {
            return NavigationUtil.toProjectPage(projectId);
        }

        return resp;
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onDeleteProject(RequestCycle cycle, String strOrgId, String strProjectId) {
        checkOrgPermission(cycle, strOrgId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        List<ProjectParticipation> participants;
        try {
            participants = ccbc.listProjectParticipations(Long.valueOf(
                    strProjectId));
        } catch (NotFoundException ex) {
            //Ignore this exception
            participants = Collections.emptyList();
        }

        Map<String, Object> map = createMap();

        if (!participants.isEmpty()) {
            map.put("participations", participants.size());
        } else {
            //Delete project

            try {
                ccbc.deleteOrgProject(
                        Long.valueOf(strProjectId),
                        LoginUserAccountHelper.getUserId(cycle));
            } catch (NotFoundException nfe) {
                LOGGER.debug("Project {} doesn't exist. Ignoring error.", strProjectId);
            }

            map.put("location", NavigationUtil.toOrgProjectsUrl(cycle, strOrgId));
        }

        return AbstractCocositeJsModule.jsonTarget(map);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onAddTask(RequestCycle cycle, String strProjectId) {
        Long projectId = Long.valueOf(strProjectId);
        OrgProject prj = getCocoboxCordinatorClient(cycle).getProject(projectId);
        checkPermission(cycle, prj);

        DruwaFormValidationSession<AddTaskForm> formsess = getValidationSession(AddTaskForm.class,
                cycle);

        if (!formsess.process()) {
            return new WebModuleRedirectRequestTarget(ProjectModule.class,
                    ProjectModule.TASK_ACTION, strProjectId);
        }

        Date taskDate = getTaskdate(prj, formsess.getObject().getTaskdate());
        if (taskDate == null) {
            formsess.addError(new ValidationError(ValidationConstraint.DATE_FORMAT, "taskdate",
                    "datetime.invalid"));
            return new WebModuleRedirectRequestTarget(ProjectModule.class,
                    ProjectModule.TASK_ACTION, strProjectId);
        }

        AddTaskForm atf = formsess.getObject();

        CreateMailTaskSendMailProcessor processor = new CreateMailTaskSendMailProcessor(
                prj.getProjectId(),
                atf.getTasktarget(),
                taskDate);

        RequestTargetGenerator taskPage = new UrlRequestTargetGenerator(cycle.urlFor(
                ProjectModule.class, ProjectModule.TASK_ACTION, strProjectId));

        SendMailSession session = new SendMailSession(processor, taskPage, taskPage);

        session.storeInSession(cycle);

        return session.getPreSendTarget(prj.getOrgId());
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onRemoveTask(RequestCycle cycle, String strProjectId) {
        Long projectId = Long.valueOf(strProjectId);
        OrgProject prj = getCocoboxCordinatorClient(cycle).getProject(projectId);
        checkPermission(cycle, prj);

        long taskId = Long.valueOf(cycle.getRequest().getParameter("taskId"));
        long userId = LoginUserAccountHelper.getUserId(cycle);

        try {
            getCocoboxCordinatorClient(cycle).deleteScheduledProjectTask(userId, projectId, taskId);
        } catch (DwsServiceErrorCodeException ex) {
            LOGGER.debug("Ignoring exception ", ex);
        }

        WebModuleInfo webModule = cycle.getApplication().getWebModuleRegistry().getModuleInfo(
                ProjectJsonModule.class.getName());
        return new WebModuleRequestTarget(webModule, "projectTasks", strProjectId);
    }

    @WebAction()
    public RequestTarget onEditTask(RequestCycle cycle, String strProjectId, String taskId) {
        Long projectId = Long.valueOf(strProjectId);
        OrgProject prj = getCocoboxCordinatorClient(cycle).getProject(projectId);
        checkPermission(cycle, prj);

        ProjectTask task = getProjectTask(cycle, prj.getProjectId(), Long.parseLong(taskId));

        if (task == null) {
            LOGGER.warn("Task doesn't exist: {}", taskId);
            return NavigationUtil.toProjectTaskPage(prj.getProjectId());
        } else if (task.getPortableMailTemplate() == null) {
            LOGGER.warn("Task doesn't contain a mail template: {}", taskId);
            return NavigationUtil.toProjectTaskPage(prj.getProjectId());
        }

        RequestTargetGenerator afterTarget = new UrlRequestTargetGenerator(NavigationUtil.
                toProjectTaskPageUrl(cycle, prj.getProjectId()));

        EditMailTaskSendMailProcessor processor
                = new EditMailTaskSendMailProcessor(task.getTaskId());

        SendMailSession session = new SendMailSession(processor, afterTarget, afterTarget);

        PortableMailTemplate pmt = PortableMailTemplateCodec.decode(task.getPortableMailTemplate());
        session.setPortableMailTemplate(pmt);

        session.storeInSession(cycle);

        return session.getPreSendTarget(prj.getOrgId());
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onUploadRoster(final RequestCycle cycle, String strProjectId) {
        Long projectId = Long.valueOf(strProjectId);
        OrgProject prj = getCocoboxCordinatorClient(cycle).getProject(projectId);
        checkPermission(cycle, prj);
        checkProjectPermission(cycle, prj, CocoboxPermissions.CP_EDIT_PROJECT_ROSTER);

        DruwaFormValidationSession<UploadRosterForm> formsess = getValidationSession(
                UploadRosterForm.class, cycle);

        LangBundle langBundle = getLangBundle(cycle, CocoSiteConstants.DEFAULT_LANG_BUNDLE);

        final FormbeanJsRequestTargetFactory jsResponse = new FormbeanJsRequestTargetFactory(cycle,
                langBundle, "cpweb.uploadroster");

        if (!formsess.process()) {
            return jsResponse.getRequestTarget(formsess);
        }

        //We have a file, now process it
        RosterReader rr = new RosterReader();
        List<Contact> contacts = null;
        boolean readOk = false;

        try {
            contacts = rr.readContacts(formsess.getObject().getFile().getInputStream());

            if (rr.isErrorsAvailable()) {
                for (RosterError rosterError : rr.getErrors()) {
                    String errText = langBundle.getKey(rosterError.getKey());
                    if (errText == null) {
                        errText = rosterError.getKey();
                    }
                    String msg = MessageFormat.format(errText, rosterError.getCoordinate().
                            toString());
                    ValidationError err = new ValidationError(ValidationConstraint.INVALID, "file",
                            "invalid.roster.line");
                    err.setMessage(msg);
                    formsess.addError(err);
                }
            } else {
                readOk = true;
            }
        } catch (RosterException rex) {
            ValidationError err = new ValidationError(ValidationConstraint.INVALID, "file",
                    "excel.processing.error");
            formsess.addError(err);
        }

        if (formsess.isInError()) {
            return jsResponse.getRequestTarget(formsess);
        }

        ArrayList<ValidationError> errors = addContacts(cycle, prj, contacts);

        for (ValidationError validationError : errors) {
            formsess.addError(validationError);
        }

        if (formsess.isInError()) {
            return jsResponse.getRequestTarget(formsess);
        }

        //Skip the errors, there might be a partial update
        String url = NavigationUtil.toProjectPageUrl(cycle, projectId);
        return jsResponse.getSuccessfulRedirect(url);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onAssignRoleEmail(RequestCycle cycle, String strProjectId) {
        String email = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "email");
        String roleId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "role");

        if (!new CocoboxRoleUtil().getProjectRoles(cycle).containsKey(roleId)) {
            String msg = String.format("Invalid project role id: %s", roleId);
            return new ErrorCodeRequestTarget(400, msg);
        }

        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);
        checkProjectPermission(cycle, prj, CocoboxPermissions.CP_ASSIGN_PROJECT_ROLE);

        if (!SimpleEmailValidator.getInstance().isValidEmail(email)) {
            LOGGER.info("Email is invalid. Not assigning any roles. ({})", email);

            return NavigationUtil.toProjectRoles(cycle, prjId);
        }

       return new AssignRoleCommand(cycle, prj, email, roleId).assign();
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onRevokeRole(RequestCycle cycle, String strProjectId) {

        long userId = DruwaParamHelper.getMandatoryLongParam(LOGGER, cycle.getRequest(), "userId");
        String roleId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "role");

        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);
        checkProjectPermission(cycle, prj, CocoboxPermissions.CP_ASSIGN_PROJECT_ROLE);

        long caller = LoginUserAccountHelper.getCurrentCaller();        

        ProjectUserRoleModification mod = new ProjectUserRoleModification(caller, prjId, userId,
                roleId);

        boolean response = ccbc.revokeProjectUserRole(mod);

        LOGGER.info("Revoking role {} in project {} for {} (caller {}): {}",
                roleId, strProjectId, userId, caller, response);

        if (JsonRequestUtil.isModernJsonCall(cycle)) {
            return new JsonRequestTarget("{\"status\": \"OK\"}");
        } else {
            return toProjectRolePage(cycle, strProjectId);
        }
    }

    private WebModuleRedirectRequestTarget toRoster(String projectId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class, "roster",
                projectId);
    }

    private LangBundle getLangBundle(RequestCycle cycle, String bundleName) {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        return LangServiceClientFactory.getInstance(cycle).getLangBundle(bundleName, userLocale.
                toString(),
                true);
    }

    private ArrayList<ValidationError> addContacts(RequestCycle cycle, OrgProject prj,
            List<Contact> contacts) {

        ArrayList<ValidationError> errors = new ArrayList<>();

        for (Contact contact : contacts) {
            try {
                addMember(cycle, prj, contact.getGivenName(), contact.getSurname(), contact.
                        getEmail());
            } catch (ValidationFailureException vfe) {
                if (vfe.getVerificationStatus() == VerificationStatus.DUPLICATE) {
                    //Ignore
                } else {
                    errors.add(new ValidationError(
                            ValidationConstraint.INVALID, null,
                            "unknownerror"));
                }
            } catch (OperationFailureException opex) {
                LOGGER.warn("Unknown OperationFailureException", opex);

                errors.add(new ValidationError(
                        ValidationConstraint.INVALID, null, "unknownerror"));
            } catch (OverlimitException opex) {
                LOGGER.warn("Project limit reached", opex);

                errors.add(new ValidationError(
                        ValidationConstraint.INVALID, null, "cpweb.uploadroster.overlimit"));
            }
        }

        return errors;
    }

    private void addMember(final RequestCycle cycle, final OrgProject prj, final String givenName,
            final String surname,
            final String email) {
        final CocoboxCoordinatorClient ccbcClient = getCocoboxCordinatorClient(cycle);
        UTComplexTxOperation<Void, Void> op = new UTComplexTxOperation<Void, Void>() {
            private UserAccount ua;
            private List<ProjectParticipation> participants;

            @Override
            protected void lock() {
            }

            @Override
            protected void releaseLock() {
            }

            @Override
            protected VerificationStatus verifyState() {
                if (ua == null) {
                    return null;
                }

                for (ProjectParticipation projectParticipation : participants) {
                    if (projectParticipation.getUserId()
                            == ua.getUserId()) {
                        return VerificationStatus.DUPLICATE;
                    }
                }

                return null;
            }

            @Override
            protected void preVerification() {
                participants = ccbcClient.listProjectParticipations(prj.getProjectId());
                ua = findUserWithEmail(email);
            }

            /**
             * Returns the user with the specified email or null.
             *
             * @param userEmailAddress
             * @return
             * @throws ClientFactoryException
             */
            private UserAccount findUserWithEmail(String userEmailAddress) throws ClientFactoryException {
                List<UserAccount> uas = Clients.getClient(cycle, UserAccountService.class).
                        getUserAccountsByProfileSetting("email", "email",
                                userEmailAddress);

                return CollectionsUtil.singleItemOrNull(uas);
            }

            @Override
            protected boolean verifyVersion() {
                return true;
            }

            @Override
            protected Void performOperation() {
                if (ua == null) {
                    CreateBasicUserAccountRequest create = CreateBasicUserAccountRequest.
                            createAccountWithoutPw(
                                    givenName,
                                    surname,
                                    email);
                    final UserAccountService uaClient
                            = Clients.getClient(cycle, UserAccountService.class);

                    try {
                        ua = uaClient.createBasicUserAccount(create);
                    } catch (AlreadyExistsException ex) {
                        LOGGER.debug("Account already exists for email {}. Trying to fetch it instead.", email);
                        ua = findUserWithEmail(email);
                        if (ua == null) {
                            String msg = String.format(
                                    "Inconsistency situation. We can't create an account with email %s but we can't find it either",
                                    email);
                            throw new IllegalStateException(msg);
                        }
                    }

                    
                    uaClient.updateUserProfileValue(ua.getUserId(),
                            CocoboxUserAccount.PROFILE_COCOBOX, 
                            CocoboxUserAccount.LOCALE,
                            prj.getLocale().toLanguageTag());

                }

                long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
                ccbcClient.newProjectParticipant(caller, prj.getProjectId(), ua.getUserId());

                return null;
            }
        };

        op.call(null);
    }

    private Date getTaskdate(OrgProject project, String taskdate) {
        SimpleDateFormat sdf = VerifyProjectDesignModule.getDatePickerSimpleDateFormat(project);
        try {
            return sdf.parse(taskdate);
        } catch (ParseException ex) {
            return null;
        }
    }

    private ProjectTask getProjectTask(RequestCycle cycle, long projectId, long taskId) {
        List<ProjectTask> tasks = getCocoboxCordinatorClient(cycle).listProjectTasks(projectId);

        for (ProjectTask task : tasks) {
            if (task.getTaskId() == taskId) {
                return task;
            }
        }

        return null;
    }

    private RequestTarget toProjectRolePage(RequestCycle cycle, String strProjectId) {
        String params = ModalParamsHelper.getParameterString(cycle);
        WebModuleRedirectRequestTarget target
                = new WebModuleRedirectRequestTarget(ProjectModule.class, "roles", strProjectId);
        target.setExtraTargetParameterString(params);

        return target;
    }
}
