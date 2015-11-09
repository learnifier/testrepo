/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.module.core.AbstractAuthModule;
import se.dabox.service.common.ccbc.mail.AdminRegistrationMailVariables;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.cocobox.security.role.CocoboxRoleUtil;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetGenericMailBucketCommand;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.mail.GetOrgProjectMailVariables;
import se.dabox.service.common.ccbc.project.ProjectDetails;
import se.dabox.service.common.ccbc.project.role.ProjectRoleAdminTokenGenerator;
import se.dabox.service.common.ccbc.project.role.ProjectUserRoleModification;
import se.dabox.service.common.context.Configuration;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.mailsender.MailSendServiceClient;
import se.dabox.service.common.mailsender.SendMailRecipient;
import se.dabox.service.common.mailsender.SendMailRequest;
import se.dabox.service.common.mailsender.mailtemplate.GetHintedMailTemplateCommand;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.login.client.AlreadyExistsException;
import se.dabox.service.login.client.CreateBasicUserAccountRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.randdata.client.RandomDataClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AssignNewUserRoleProcessor extends AbstractRoleProcessor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AssignNewUserRoleProcessor.class);
    
    private static final long serialVersionUID = 1L;

    private final String email;
    
    AssignNewUserRoleProcessor(long projectId, String email, String role) {
        super(projectId, role);
        this.email = email;
    }

    @Override
    public void processSendMail(RequestCycle cycle, SendMailSession sms, SendMailTemplate smt) {
        //Remember to not send the edited email. It should be used in the after-registration phase.

        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        CreateBasicUserAccountRequest req = CreateBasicUserAccountRequest.createEmailAccount(email);

        UserAccount account;

        try {
            account = uaClient.createBasicUserAccount(req);
        } catch (AlreadyExistsException aee) {
            account = uaClient.getSingleUserAccountByEmail(email);
        }

        addProjectRole(cycle, account);
        addClientRole(cycle, account);

        String token = createToken(cycle, account, smt);

        Configuration config = DwsRealmHelper.getRealmConfiguration(cycle);

        OrgUnitInfo org = getOrgUnit();

        Map<String, String> vars =
                new AdminRegistrationMailVariables().produceFor(config, account, org, token);

        ProjectDetails prj = getProject();

        SendMailTemplate activationTemplate = getActivationTemplate(cycle, account);

        SendMailRequest mailReq = getNewSendMailRequest(cycle);
        activationTemplate.toSendMailRequest(mailReq);
        
        SendMailRecipient recipient = new SendMailRecipient(account.getDisplayName(),
                account.getPrimaryEmail());

        recipient.addVariables(vars);
        recipient.addVariables(new GetOrgProjectMailVariables().getMap(prj, config));

        String roleName = new CocoboxRoleUtil().getProjectRoles(cycle).get(getRoleId());
        recipient.addVariable("role", roleName);

        mailReq.addRecipient(recipient);

        CacheClients.getClient(cycle, MailSendServiceClient.class).sendMail(mailReq);
    }

    private String createToken(RequestCycle cycle, UserAccount account,
            SendMailTemplate template) {
        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
        String json = new ProjectRoleAdminTokenGenerator().
                generateToken(caller, account.getUserId(), getProjectId(), getRoleId(),
                        template.getSubject(), template.getBody());

        RandomDataClient rdc = AbstractAuthModule.getRandomDataClient(cycle);

        String token = rdc.addRandomData(LoginUserAccountHelper.getUserId(cycle), json);

        return token;
    }

    private SendMailTemplate getActivationTemplate(RequestCycle cycle, UserAccount account) {

        OrgUnitInfo org = getOrgUnit();

        MailTemplateServiceClient mtClient
                = CacheClients.getClient(cycle, MailTemplateServiceClient.class);

        long realmBucket = new GetGenericMailBucketCommand(cycle).getId();
        long clientBucket = new GetOrgMailBucketCommand(cycle).forOrg(org.getId());

        Locale locale = CocositeUserHelper.getUserAccountUserLocale(account);
        MailTemplate template
                = GetHintedMailTemplateCommand.getHintedTemplate(mtClient,
                        CpwebConstants.PRJADMIN_REGISTRATION_MAIL_HINT, clientBucket, realmBucket,
                        locale);

        return new SendMailTemplate(template.getSubject(), template.getMainContent(), template.getType());
    }

    private void addProjectRole(RequestCycle cycle, UserAccount account) {
        final long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
        final long projectId = getProjectId();
        final String roleId = getRoleId();
        final long userId = account.getUserId();

        ProjectUserRoleModification mod = new ProjectUserRoleModification(caller,
                projectId, userId,
                roleId);

        CocoboxCoordinatorClient ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        boolean response = ccbc.grantProjectUserRole(mod);

        LOGGER.info("Adding role {} in project {} for {} (caller {}): {}",
                roleId, projectId, userId, caller, response);
    }

    private void addClientRole(RequestCycle cycle, UserAccount account) {
        final long orgId = getOrgUnit().getId();

        new AddMissingClientAccessRole(cycle).addOrgUnitAccess(account, orgId);
    }

}
