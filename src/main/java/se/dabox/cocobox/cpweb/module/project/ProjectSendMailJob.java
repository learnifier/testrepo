/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.ServiceApplication;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.cocobox.cpweb.state.SendMailTemplate;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.RealmBackgroundCallable;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ajaxlongrun.StatusSource;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.ProjectParticipantMailRequest;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProjectSendMailJob extends RealmBackgroundCallable<Integer> implements StatusSource {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectSendMailJob.class);

    private final long projectId;
    private final long orgId;
    private final long caller;

    private final SendMailTemplate template;
    private final List<Long> receivers;
    public final List<Object> extraDatas;

    private volatile Status status = new Status("Preparing mail");
    private Map<Long, UserAccount> userMap;
    private int failedSend;

    public ProjectSendMailJob(long projectId, long orgId, long caller, SendMailTemplate template,
            List<Long> receivers, List<Object> extraDatas, ServiceApplication app) {
        super(app);
        this.projectId = projectId;
        this.orgId = orgId;
        this.caller = caller;
        this.template = template;
        this.receivers = receivers;
        this.extraDatas = extraDatas;
    }

    @Override
    protected Integer callInCycle(ServiceRequestCycle cycle) throws Exception {
        CocoboxCoordinatorClient ccbc = AbstractModule.getCocoboxCordinatorClient(cycle);

        getUserMap(cycle);

        int count = 0;
        for (Object extraData : extraDatas) {
            String name = null;

            if (count < receivers.size()) {
                Long userId = receivers.get(count);
                UserAccount account = userMap.get(userId);
                if (account != null) {
                    if (!StringUtils.isBlank(account.getDisplayName())) {
                        name = String.format("%s (%d/%d)",
                                account.getDisplayName(),
                                count,
                                extraDatas.size()
                                );
                    } else {
                        name = String.format("%s (%d/%d)",
                                account.getPrimaryEmail(),
                                count,
                                extraDatas.size()
                                );
                    }
                }
            }

            if (name == null) {
                name = "";
            }

            status = new Status("Sending email to " + name, (long) extraDatas.size(), (long) count);

            ProjectParticipantMailRequest req = new ProjectParticipantMailRequest(caller, projectId,
                    "dws/mail-template", template.
                    getSubject(), template.getBody());

            req.addParticipationId((Long) extraData);

            try {
                ccbc.sendProjectParticipantMail(req);
            } catch (Exception ex) {
                failedSend++;
                LOGGER.error("Exception when sending invitation", ex);
            }

            count++;
        }

        return failedSend;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    private void getUserMap(ServiceRequestCycle cycle) {
        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        List<UserAccount> users = uaClient.getUserAccounts(receivers);

        userMap = CollectionsUtil.createMap(users, UserAccount::getUserId);
    }

}
