/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpAdminRoles;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.CpJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.role.CocoboxRoleUtil;
import se.dabox.cocobox.security.user.UserAccountRoleCheck;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.io.RuntimeIOException;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.json.DataTablesJson;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/user.json")
public class UserJsonModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserJsonModule.class);

    @WebAction
    public RequestTarget onListUserParticipations(RequestCycle cycle, String strOrgId, String strUserId) {
        long orgId = secureGetMiniOrg(cycle, strOrgId).getId();
        checkOrgPermission(cycle, orgId);
        checkOrgPermission(cycle, orgId, CocoboxPermissions.CP_VIEW_USER);

        long userId = Long.valueOf(strUserId);

        List<ProjectParticipation> participations =
                getCocoboxCordinatorClient(cycle).listProjectParticipationsForUserId(userId);

        return jsonTarget(toJsonResponse(cycle, participations));
    }

    @WebAction
    public RequestTarget onListRoles(final RequestCycle cycle, String strOrgId, String strUserId) {
        long userId = Long.valueOf(strUserId);
        long orgId = secureGetMiniOrg(cycle, strOrgId).getId();
        checkOrgPermission(cycle, orgId, CocoboxPermissions.CP_VIEW_USER);

        UserAccount account = getUserAccount(cycle, userId);

        Set<String> roles = UserAccountRoleCheck.getCpRoles(account, orgId);
        final Map<String, String> cpRoles = new CocoboxRoleUtil().getCpRoles(cycle);
        
        for (Iterator<String> it = roles.iterator(); it.hasNext();) {
            if (!cpRoles.containsKey(it.next())) {
                it.remove();
            }
        }

        return jsonTarget(new DataTablesJson<String>() {
            @Override
            protected void encodeItem(String uuid) throws IOException {
                generator.writeStringField("uuid", uuid);
                generator.writeStringField("name", cpRoles.get(uuid));
            }
        }.encodeToStream(roles));
    }

    @WebAction
    public RequestTarget onRemoveCpRole(RequestCycle cycle, String strOrgId) {
        
        long userId = DruwaParamHelper.getMandatoryLongParam(LOGGER, cycle.getRequest(), "userId");
        String role = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "role");

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        long orgId = org.getId();
        checkOrgPermission(cycle, orgId, CocoboxPermissions.CP_EDIT_USER);

        UserAccount user = getUserAccount(cycle, userId);

        Set<String> roles = UserAccountRoleCheck.getCpRoles(user, orgId, true);
        final Map<String, String> cpRoles = new CocoboxRoleUtil().getCpRoles(cycle);

        if (!cpRoles.containsKey(role)) {
            throw new IllegalStateException("Non existing client role: "+role);
        }

        roles.remove(role);

        String roleString;

        if (roles.isEmpty()) {
            roleString = CpAdminRoles.NONE;
        } else {
            roleString = StringUtils.join(roles, ',');
        }

        CharSequence valueName = OrgRoleName.forOrg(orgId);

        getUserAccountService(cycle).updateUserProfileValue(userId,
                CocoSiteConstants.UA_PROFILE, valueName, roleString);

        return onListRoles(cycle, strOrgId, Long.toString(userId));
    }

    private ByteArrayOutputStream toJsonResponse(
            RequestCycle cycle,
            List<ProjectParticipation> participations) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                CpJsonModule.DEFAULT_BYTE_SIZE);

        LazyProjectName projName = new LazyProjectName(getCocoboxCordinatorClient(cycle));

        try {
            try (JsonGenerator generator
                    = CpJsonModule.FACTORY.createJsonGenerator(
                            baos)) {
                generator.writeStartObject();
                
                generator.writeArrayFieldStart("aaData");
                
                for (ProjectParticipation ppart : participations) {
                    generator.writeStartObject();
                    generator.writeNumberField("id", ppart.getParticipationId());
                    generator.writeStringField("projectname", projName.forProject(ppart.getProjectId()));
                    generator.writeStringField("projectlink", NavigationUtil.toProjectPageUrl(cycle,
                            ppart.getProjectId()));

                    generator.writeEndObject();
                }

                generator.writeEndArray();

                generator.writeEndObject();
            }

            return baos;
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to encode JSON", ex);
        }
    }

    private UserAccount getUserAccount(RequestCycle cycle, long userId) {        
        return getUserAccountService(cycle).getUserAccount(userId);
    }

    private UserAccountService getUserAccountService(RequestCycle cycle) {
        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        return uaClient;
    }

}
