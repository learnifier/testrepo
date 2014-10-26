/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.security.CocoboxSecurityConstants;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.dws.client.JacksonHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.role.ProjectRoleAdminTokenGenerator;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.orgadmin.client.AdminMailRequest;
import se.dabox.service.orgadmin.client.MailInfo;
import se.dabox.service.orgadmin.client.OrgAdminClient;
import se.dabox.service.randdata.client.RandomDataClient;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ActivateNewProjectAdmin {
    private final RequestCycle cycle;
    private final String randDataId;
    private final Map<String, ?> map;
    private final Long userId;

    public ActivateNewProjectAdmin(RequestCycle cycle, String id,
            Map<String, ?> map) {
        this.cycle = cycle;
        this.randDataId = id;
        this.map = map;
        this.userId = JacksonHelper.getLong(map, "userId");
    }

    public String getTargetUrl() {
        activateUserRoles();
        sendWelcomeMail();
        markMapCompleted();

        return getFirstAccessUrl();
    }

    /**
     * Activate the normal roles for the user so he/she can login and access the platform.
     *
     */
    private void activateUserRoles() {
        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);
        uaClient.addUserRole(userId, CocoboxSecurityConstants.USER_ROLE);
    }

    /**
     * Send the pre-authored welcome mail to the user
     *
     */
    private void sendWelcomeMail() {
        CocoboxCoordinatorClient ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        String roleId = (String) map.get(ProjectRoleAdminTokenGenerator.FIELD_ROLE_ID);

        String roleName = new CocoboxRoleUtil().getProjectRoles(cycle).get(roleId);
        String subject = JacksonHelper.getString(map,
                ProjectRoleAdminTokenGenerator.FIELD_MAIL_SUBJECT);
        String mailBody = JacksonHelper.getString(map,
                ProjectRoleAdminTokenGenerator.FIELD_MAIL_BODY);
        long projectId = JacksonHelper.getLong(map, ProjectRoleAdminTokenGenerator.FIELD_PROJECT_ID);
        long caller = JacksonHelper.getLong(map, ProjectRoleAdminTokenGenerator.FIELD_CALLER);

        OrgProject prj = ccbc.getProject(projectId);

        Map<String,String> mailMap = Collections.singletonMap("role", roleName);

        MailInfo mail = new MailInfo(subject, mailBody, mailMap);

        AdminMailRequest mailReq = new AdminMailRequest(prj.getOrgId(),
                projectId,
                userId,
                mail);

        OrgAdminClient oaClient = CacheClients.getClient(cycle, OrgAdminClient.class);
        oaClient.sendAdminMail(caller, mailReq);
    }

    /**
     * Mark the registration session as consumed
     *
     */
    private void markMapCompleted() {
        Map<String,Object> copy = new HashMap<>(map);
        copy.put("consumed", Boolean.TRUE);

        String copyData = ProjectRoleAdminTokenGenerator.encodeMap(copy);

        RandomDataClient rdClient = CacheClients.getClient(cycle, RandomDataClient.class);
        rdClient.updateRandomData(UserAccount.USERID_UNKNOWN, randDataId, copyData);
    }

    /**
     * Returns the page the user should see the first time he/she accesses
     * the platform "for real".
     *
     * @return A url
     */
    private String getFirstAccessUrl() {
        return DwsRealmHelper.getRealmConfiguration(cycle).getValue("boweb.baseurl");
    }

}
