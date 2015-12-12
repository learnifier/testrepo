/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.module.WebModuleInfo;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRequestTarget;
import se.dabox.cocobox.cpweb.module.CpMainModule;
import se.dabox.cocobox.cpweb.module.OrgSelectorModule;
import se.dabox.cocobox.cpweb.module.account.AccountSettingsModule;
import se.dabox.cocobox.cpweb.module.branding.BrandingModule;
import se.dabox.cocobox.cpweb.module.coursedesign.DesignModule;
import se.dabox.cocobox.cpweb.module.cug.ClientUserGroupModule;
import se.dabox.cocobox.cpweb.module.integration.IntegrationErrorModule;
import se.dabox.cocobox.cpweb.module.mail.MailModule;
import se.dabox.cocobox.cpweb.module.project.NewProjectModule;
import se.dabox.cocobox.cpweb.module.project.ProjectModule;
import se.dabox.cocobox.cpweb.module.project.VerifyProjectDesignModule;
import se.dabox.cocobox.cpweb.module.user.UserModule;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.service.common.ccbc.project.OrgProject;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public final class NavigationUtil {

    public static RequestTarget toProjectPage(long projectId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class,
                ProjectModule.OVERVIEW_ACTION, Long.toString(projectId));
    }

    
    public static String toProjectPageUrlPrefix(RequestCycle cycle) {
        return cycle.urlFor(ProjectModule.class, ProjectModule.OVERVIEW_ACTION);
    }
    
    public static String toProjectPageUrl(RequestCycle cycle, long projectId) {
        return cycle.urlFor(ProjectModule.class,
                ProjectModule.OVERVIEW_ACTION, Long.toString(projectId));
    }

    public static RequestTarget toProjectTaskPage(long projectId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class,
                ProjectModule.TASK_ACTION, Long.toString(projectId));
    }

    public static String toProjectTaskPageUrl(RequestCycle cycle, long projectId) {
        return cycle.urlFor(ProjectModule.class,
                ProjectModule.TASK_ACTION, Long.toString(projectId));
    }

    public static RequestTarget toOrgMain(String strOrgId) {
        return new WebModuleRedirectRequestTarget(CpMainModule.class, "home",
                strOrgId);
    }

    public static String toOrgMainUrl(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(CpMainModule.class, "home",
                strOrgId);
    }

    /**
     * Url to the main page of the cpweb. The processing will redirect the user
     * to the first matching rule.
     *
     * <ul>
     * <li>If the user is a Back Office admin a direct will be made to the back office main
     * page.</li>
     * <li>If the user has access to only one client they are redirected to the main page for that
     * client.</li>
     * <li>If the user has access to multiple clients they will be redirected to the client picker.
     * </li>
     * <li>If the current user doesn't have access to any clients a redirect will be made to the
     * userweb main page. </li>
     * </ul>
     *
     * @param cycle The current request cycle
     * @return A url to the main page
     */
    public static String toMainUrl(RequestCycle cycle) {
        return cycle.urlFor(CpMainModule.class, "home");
    }

    /**
     * Returns a RequestTarget that redirects to the main page.
     *
     * @param cycle The current request cycle
     * @return A url to the main page
     *
     * @see #toMainUrl(net.unixdeveloper.druwa.RequestCycle)
     */
    public static RequestTarget toMain(RequestCycle cycle) {
        return new RedirectUrlRequestTarget(toMainUrl(cycle));
    }

    /**
     * Returns an url to the user overview (details) page.
     *
     * @param cycle
     * @param strOrgId
     * @param userId
     * @return
     */
    public static String toUserPageUrl(RequestCycle cycle, String strOrgId, long userId) {
        return cycle.urlFor(UserModule.class,
                UserModule.OVERVIEW_ACTION, strOrgId, Long.toString(userId));
    }

    public static String toEmailPageUrl(RequestCycle cycle, String strOrgId, long templateId) {
        return cycle.urlFor(MailModule.class,
                MailModule.OVERVIEW_ACTION, strOrgId, Long.toString(templateId));
    }

    public static String toEmailListPageUrl(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(CpMainModule.class,
                CpMainModule.LIST_EMAILS, strOrgId);
    }

    public static String toDesignPageUrl(RequestCycle cycle, String strOrgId, long designId) {
        return cycle.urlFor(DesignModule.class,
                DesignModule.OVERVIEW_ACTION, strOrgId, Long.toString(designId));
    }

    public static String toDesignListPageUrl(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(CpMainModule.class,
                CpMainModule.LIST_DESIGNS, strOrgId);
    }

    public static RequestTarget toOrgUsers(RequestCycle cycle, long orgId) {
        return new WebModuleRedirectRequestTarget(CpMainModule.class,
                CpMainModule.LIST_USERS_ACTION, Long.toString(orgId));
    }

    public static String toOrgUsersUrl(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(CpMainModule.class,
                CpMainModule.LIST_USERS_ACTION, strOrgId);
    }

    public static String toOrgFragmentStart(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(CpMainModule.class,
                Actions.MAIN_HOME,
                strOrgId);
    }

    public static String toOrgProjectsUrl(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(CpMainModule.class,
                CpMainModule.LIST_PROJECTS,
                strOrgId);
    }

    public static String toAccountSettingsUrl(RequestCycle cycle, String strOrgId) {
        return cycle.urlFor(AccountSettingsModule.class, "settings", strOrgId);
    }

    public static String toProjectMaterialPageUrl(RequestCycle cycle, long prjId) {
        return cycle.urlFor(ProjectModule.class, ProjectModule.MATERIAL_ACTION, Long.toString(
                prjId));
    }

    public static RequestTarget toProjectMaterialPage(RequestCycle cycle, long prjId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class,
                ProjectModule.MATERIAL_ACTION,
                Long.toString(prjId));
    }

    public static RequestTarget toCreateProject(RequestCycle cycle, String strOrgId) {
        return new WebModuleRedirectRequestTarget(NewProjectModule.class,
                NewProjectModule.SETUP,
                strOrgId);
    }

    public static RequestTarget getIntegrationErrorPage(RequestCycle cycle, ErrorState state) {
        WebModuleInfo info = cycle.getApplication().getWebModuleRegistry().
                getModuleInfo(IntegrationErrorModule.class.getName());
        cycle.setAttribute(IntegrationErrorModule.ATTRIBUTE_ERRORSTATE, state);
        return new WebModuleRequestTarget(info, "error");
    }

    public static WebModuleRedirectRequestTarget toProjectSecondaryData(RequestCycle cycle, OrgProject project) {
        return new WebModuleRedirectRequestTarget(VerifyProjectDesignModule.class,
                VerifyProjectDesignModule.ACTION_SECONDARY_DATA,
                Long.toString(project.getProjectId()));
    }

    /**
     * Target to the Project Roles (Project Admin/Team member) page.
     *
     * @param cycle
     * @param projectId
     * @return
     */
    public static WebModuleRedirectRequestTarget toProjectRoles(RequestCycle cycle, long projectId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class,
                ProjectModule.ROLES_ACTION,
                Long.toString(projectId));
    }

    public static RequestTarget toOrgSelector() {
        return new WebModuleRedirectRequestTarget(OrgSelectorModule.class);
    }

    public static String toProjectRosterPageUrl(RequestCycle cycle, long projectId) {
        return cycle.urlFor(ProjectModule.class,
                ProjectModule.ROSTER_ACTION,
                Long.toString(projectId));
    }

    public static RequestTarget toBrandingPage(String strOrgId) {
        return new WebModuleRedirectRequestTarget(BrandingModule.class, "logo", strOrgId);
    }

    /**
     * Returns a WebModuleRedirectTarget to the project roster page.
     *
     * @param projectId The project id
     * @return
     */
    public static RequestTarget toProjectRoster(long projectId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class, ProjectModule.ROSTER_ACTION,
                Long.toString(projectId));
    }

    public static RequestTarget toClientUserGroupList(RequestCycle cycle, long orgId) {
        return new WebModuleRedirectRequestTarget(ClientUserGroupModule.class,
                ClientUserGroupModule.LIST_ACTION, Long.toString(orgId));
    }

    public static String toClientUserGroupListUrl(RequestCycle cycle, long orgId) {
        return cycle.urlFor(ClientUserGroupModule.class,
                ClientUserGroupModule.LIST_ACTION, Long.toString(orgId));
    }

    public static RequestTarget toClientUserGroupOverview(RequestCycle cycle, long orgId, long groupId) {
        return new WebModuleRedirectRequestTarget(ClientUserGroupModule.class,
                ClientUserGroupModule.OVERVIEW_ACTION, Long.toString(orgId), Long.toString(groupId));
    }

    public static String toClientUserGroupOverviewUrl(RequestCycle cycle, long orgId, long groupId) {
        return cycle.urlFor(ClientUserGroupModule.class,
                ClientUserGroupModule.OVERVIEW_ACTION, Long.toString(orgId), Long.toString(groupId));
    }

    public static RequestTarget toClientUserGroupChildren(RequestCycle cycle, long orgId, long groupId) {
        return new WebModuleRedirectRequestTarget(ClientUserGroupModule.class,
                ClientUserGroupModule.CHILDREN_ACTION, Long.toString(orgId), Long.toString(groupId));
    }

    public static String toClientUserGroupChildrenUrl(RequestCycle cycle, long orgId, long groupId) {
        return cycle.urlFor(ClientUserGroupModule.class,
                ClientUserGroupModule.CHILDREN_ACTION, Long.toString(orgId), Long.toString(groupId));
    }

    private NavigationUtil() {
    }
}
