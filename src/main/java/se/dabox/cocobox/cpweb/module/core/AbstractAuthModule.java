/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.core;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.security.UserOrgSecurityCheck;
import se.dabox.cocobox.security.permission.Permission;
import se.dabox.cocobox.security.project.ProjectPermissionCheck;
import se.dabox.cocobox.security.user.UserPermissionFetcher;
import se.dabox.cocosite.branding.GetOrgBrandingCommand;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.messagepage.GenericMessagePageFactory;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.user.MiniUserAccountHelperContext;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public abstract class AbstractAuthModule extends AbstractModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractAuthModule.class);

    protected void checkPermission(RequestCycle cycle, OrgProject project) {
        if (project == null) {
            throw new IllegalStateException("AbstractAuthModule::checkPermission project is null");
        }

        checkOrgPermission(cycle, project.getOrgId());
    }

    protected void checkOrgPermission(RequestCycle cycle, String strOrgId) {
        final Boolean accessAllowed =
                UserOrgSecurityCheck.getInstance().getObject(cycle, strOrgId);

        if (!accessAllowed) {
            final long userId = LoginUserAccountHelper.getUserId(cycle);
            String msg = String.format("Access denied to %s ou=%s (uid=%d)",
                    cycle.getRequest().getRequestUrl(),
                    strOrgId,
                    userId);
            handleAccessDenied(cycle, msg);
        }

        activateOrgBranding(cycle, Long.parseLong(strOrgId));
    }

    protected void checkOrgPermission(RequestCycle cycle, long orgId) {
        checkOrgPermission(cycle, Long.toString(orgId));
    }

    protected void checkOrgPermission(RequestCycle cycle, String strOrgId, Permission permission) {
        checkOrgPermission(cycle, Long.parseLong(strOrgId), permission);
    }

    protected void checkOrgPermission(RequestCycle cycle, long orgId, Permission permission) {
        checkOrgPermission(cycle, orgId);

        boolean authorized = hasOrgPermission(cycle, orgId, permission);

        if (!authorized) {
            String msg = String.format("Access denied to %s ou=%d (uid=%d). Permission %s required.",
                    cycle.getRequest().getRequestUrl(),
                    orgId,
                    LoginUserAccountHelper.getUserId(cycle),
                    permission);
            handleAccessDenied(cycle, msg);
        }
    }

    private void activateOrgBranding(RequestCycle cycle, long orgId) {
        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);
        if (branding != null) {
            setLetterBubbleColor(branding);
        }
    }

    private void setLetterBubbleColor(Branding branding) {
        java.awt.Color color = branding.getMetadataColor("cpPrimaryColor");
        if (color != null) {
            MiniUserAccountHelperContext.getCycleContext().setLetterBubbleBgColor(color);
        }
    }

    /**
     * Checks if the user has a permission in an org unit.
     *
     * @param cycle
     * @param orgId
     * @param permission
     * @return
     */
    protected boolean hasOrgPermission(RequestCycle cycle, long orgId, Permission permission) {
        UserAccount acc = LoginUserAccountHelper.getUserAccount(cycle);
        Set<Permission> roles = new UserPermissionFetcher(cycle).getUserPermission(acc, orgId);

        return roles.contains(permission);
    }

    protected void checkProjectPermission(RequestCycle cycle, OrgProject project,
            Permission permission) {
        boolean authorized = ProjectPermissionCheck.fromCycle(cycle).checkPermission(project,
                permission);

        if (!authorized) {
            String msg = String.format("Access denied to %s prj=%d (uid=%d). Permission %s required.",
                    cycle.getRequest().getRequestUrl(),
                    project.getProjectId(),
                    LoginUserAccountHelper.getUserId(cycle),
                    permission);
            handleAccessDenied(cycle, msg);
        }
    }


    protected long getCurrentUser(RequestCycle cycle) {
        return LoginUserAccountHelper.getCurrentCaller(cycle);
    }

    protected Locale getUserLocale(RequestCycle cycle) {
        return CocositeUserHelper.getUserLocale(cycle);
    }

    /**
     * Returns a MiniOrg with the specified id.
     *
     * <p>This operation throws a RetargetException with a ErrorCode 400 response if the strOrgId
     *  parameter is not valid</p>
     *
     * <p>This operation throws a RetargetException with a ErrorCode 403 response if the current
     * user doesn't have access to the specified org id</p>
     *
     * <p>This operation throws a RetargetException with a ErrorCode 404 response if the current
     * permission check worked but the specified org id doesn't exist</p>
     *
     * @param cycle The current request cycle
     * @param strOrgId A string with an organization id
     * @return A MiniOrgInfo for the specified org id.
     */
    protected MiniOrgInfo secureGetMiniOrg(RequestCycle cycle, String strOrgId) {
        if (isInvalidOrgId(strOrgId)) {
            throw new RetargetException(new ErrorCodeRequestTarget(400, "Invalid org unit id: "+strOrgId));
        }

        checkOrgPermission(cycle, strOrgId);
        MiniOrgInfo miniOrg
                = InfoCacheHelper.getInstance(cycle).getMiniOrgInfo(Long.valueOf(strOrgId));

        if (miniOrg == null) {
            String msg = String.format("Invalid org unit: %s", strOrgId);
            LOGGER.warn(msg);

            RequestTarget page
                = GenericMessagePageFactory.newNotFoundPage().withMessageText(msg).build();

            throw new RetargetException(page);
        }

        return miniOrg;
    }

    /**
     * Returns a MiniOrg with the specified id.
     * This method always call ${@link #secureGetMiniOrg(net.unixdeveloper.druwa.RequestCycle, java.lang.String) }.
     *
     * @param cycle
     * @param orgId
     * @return
     */
    protected MiniOrgInfo secureGetMiniOrg(RequestCycle cycle, long orgId) {
        checkOrgPermission(cycle, orgId);
        return InfoCacheHelper.getInstance(cycle).getMiniOrgInfo(orgId);
    }

    /**
     * Retrieves an organization unit info with the specified id. The string is decoded to a long
     * value and the organization must exist and be accessible by the current user. If the
     * organization does not exist or access is denied an exception is thrown.
     *
     * <p>This method always return an OrganizationUnitInfo or throws an exception</p>
     *
     * @param cycle    The current request cycle
     * @param strOrgId The organization unit id in a String representation.
     *
     * @return An organization unit.
     */
    protected OrgUnitInfo securedGetOrganization(RequestCycle cycle, String strOrgId) {
        if (StringUtils.isEmpty(strOrgId)) {
            throwInvalidOrgId(strOrgId);
        }

        long orgId;

        try {
            orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException nfe) {
            throwInvalidOrgId(strOrgId);
            return null;
        }

        return securedGetOrganization(cycle, orgId);
    }

    /**
     * Retrieves an organization unit info with the specified id. The string is decoded to a long
     * value and the organization must exist and be accessible by the current user. If the
     * organization does not exist or access is denied an exception is thrown.
     *
     * <p>This method always return an OrganizationUnitInfo or throws an exception</p>
     *
     * @param cycle The current request cycle
     * @param orgId The organization unit id.
     *
     * @return An organization unit.
     */
    protected OrgUnitInfo securedGetOrganization(RequestCycle cycle, long orgId) {
        OrganizationDirectoryClient ods =
                CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        OrgUnitInfo orgUnit = ods.getOrgUnitInfo(orgId);

        if (orgUnit == null) {
            String msg = "Organization not found: " + orgId;
            LOGGER.warn(msg);

            RequestTarget page
                = GenericMessagePageFactory.newNotFoundPage().withMessageText(msg).build();

            throw new RetargetException(page);
        }

        checkOrgPermission(cycle, orgId);

        return orgUnit;
    }

    private void throwInvalidOrgId(String strOrgId) throws IllegalStateException {
        String msg = "Invalid organization id specified: " + strOrgId;
        LOGGER.warn(msg);

        RequestTarget page
                = GenericMessagePageFactory.newNotFoundPage().withMessageText(msg).build();

        throw new RetargetException(page);
    }

    protected void handleAccessDenied(RequestCycle cycle, String msg) {
        LOGGER.error("Access denied: {}", msg);

        RequestTarget page
                = GenericMessagePageFactory.newSecurityPage().withMessageText(msg).build();

        throw new RetargetException(page);
    }

    private boolean isInvalidOrgId(String strOrgId) {
        if (StringUtils.isEmpty(strOrgId)) {
            return true;
        }

        try {
            Long.parseLong(strOrgId);
            return false;
        } catch(NumberFormatException ex) {
            return true;
        }
    }

    /**
     * Returns a formatter for the current user suitable for table listings.
     *
     * @param cycle
     * @return
     */
    public static DateFormat getUserListDateFormat(RequestCycle cycle) {
        return DateFormat.getDateInstance(DateFormat.SHORT, CocositeUserHelper.getUserLocale(cycle));
    }

}
