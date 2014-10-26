/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.util.Collections;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.module.mail.MailSender;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.mail.SendMailRequestFactory;
import se.dabox.service.common.ccbc.project.ProjectDetails;
import se.dabox.service.common.mailsender.SendMailRequest;
import se.dabox.service.orgadmin.client.AdminMailRequest;
import se.dabox.service.orgadmin.client.MailInfo;
import se.dabox.service.orgadmin.client.OrgAdminClient;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
abstract class AbstractRoleProcessor implements SendMailProcessor {
    private static final long serialVersionUID = 1L;

    private final long projectId;
    private final String roleId;

    public AbstractRoleProcessor(long projectId, String roleId) {
        this.projectId = projectId;
        this.roleId = roleId;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getRoleId() {
        return roleId;
    }
    
    @Override
    public MailSender getMailSender(RequestCycle cycle) {
        SendMailRequest req =
                SendMailRequestFactory.newRequest(LoginUserAccountHelper.getUserId(cycle));
        return new MailSender(req.getFromName(), req.getFromEmail());
    }

    public void sendMail(RequestCycle cycle,
            SendMailSession sms,
            SendMailTemplate smt,
            long receiverUserId) {
        long caller = LoginUserAccountHelper.getCurrentCaller();

        sendMail(cycle, caller, sms, smt, receiverUserId);

    }

    public void sendMail(RequestCycle cycle,
            long caller,
            SendMailSession sms,
            SendMailTemplate smt,
            long receiverUserId) {

        String roleName = new CocoboxRoleUtil().getProjectRoles(cycle).get(roleId);

        Map<String,String> map = Collections.singletonMap("role", roleName);

        MailInfo mail = new MailInfo(smt.getSubject(), smt.getBody(), map);

        AdminMailRequest mailReq = new AdminMailRequest(getProject().getOrgId(),
                projectId,
                receiverUserId,
                mail);

        OrgAdminClient oaClient = CacheClients.getClient(cycle, OrgAdminClient.class);
        oaClient.sendAdminMail(caller, mailReq);
    }

    protected SendMailRequest getNewSendMailRequest(RequestCycle cycle) {
        SendMailRequest req =
                SendMailRequestFactory.newRequest(LoginUserAccountHelper.getUserId(cycle));
        return req;
    }

    protected ProjectDetails getProject() {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();

        CocoboxCoordinatorClient ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        return ccbc.getProject(projectId);
    }

    protected OrgUnitInfo getOrgUnit() {

        ProjectDetails prj = getProject();
        final long ouId= prj.getOrgId();

        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();

        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        return odClient.getOrgUnitInfo(ouId);
    }

}
