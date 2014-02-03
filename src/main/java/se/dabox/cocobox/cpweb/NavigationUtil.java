/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.module.WebModuleInfo;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRequestTarget;
import se.dabox.cocobox.cpweb.module.CpMainModule;
import se.dabox.cocobox.cpweb.module.OrgSelectorModule;
import se.dabox.cocobox.cpweb.module.account.AccountSettingsModule;
import se.dabox.cocobox.cpweb.module.coursedesign.DesignModule;
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
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public final class NavigationUtil {

    public static RequestTarget toProjectPage(long projectId) {
        return new WebModuleRedirectRequestTarget(ProjectModule.class,
                ProjectModule.ROSTER_ACTION, Long.toString(projectId));
    }

    
    public static String toProjectPageUrlPrefix(RequestCycle cycle) {
        return cycle.urlFor(ProjectModule.class, ProjectModule.ROSTER_ACTION);
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

    public static String toProjectPageUrl(RequestCycle cycle, long projectId) {
        return cycle.urlFor(ProjectModule.class,
                ProjectModule.ROSTER_ACTION, Long.toString(projectId));
    }

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

    public static RequestTarget toOrgSelector() {
        return new WebModuleRedirectRequestTarget(OrgSelectorModule.class);
    }

    private NavigationUtil() {
    }
}
