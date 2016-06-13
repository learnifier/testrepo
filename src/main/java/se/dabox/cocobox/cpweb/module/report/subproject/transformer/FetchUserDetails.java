/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.module.report.StatusHolder;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.ClientFactoryException;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.util.Holder;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Factory;
import se.dabox.util.collections.ListUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class FetchUserDetails implements Factory<List<SubprojectParticipant>> {

    private final StatusHolder statusHolder;
    private final Factory<List<SubprojectParticipant>> backend;

    public FetchUserDetails(StatusHolder statusHolder,
            Factory<List<SubprojectParticipant>> backend) {
        this.statusHolder = statusHolder;
        this.backend = backend;
    }

    @Override
    public List<SubprojectParticipant> create() {
        List<SubprojectParticipant> list = backend.create();

        Map<Long, UserAccount> userMap
                = CollectionsUtil.createMap(getUsers(list), UserAccount::getUserId);

        statusHolder.setStatus(new Status("Populating user details"));
        for (SubprojectParticipant part : list) {
            UserAccount user = userMap.get(part.getUserId());
            if (user != null) {
                part.setEmail(user.getPrimaryEmail());
                part.setName(user.getDisplayName());
            }
        }

        return list;
    }

    private List<UserAccount> getUsers(List<SubprojectParticipant> list) throws ClientFactoryException {
        List<Long> userIdList = getUserIds(list);

        return getUsersAccounts(userIdList);
    }

    private List<Long> getUserIds(List<SubprojectParticipant> list) {
        Set<Long> userIds = CollectionsUtil.transform(list, SubprojectParticipant::getUserId);
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        List<Long> userIdList = new ArrayList<>(userIds);
        return userIdList;
    }

    private List<UserAccount> getUsersAccounts(List<Long> userIdList) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);

        Holder<Long> processedHolder = new Holder<>(0L);

        return ListUtil.processPartitions(100, userIdList, (subl) -> {
            statusHolder.setStatus(new Status("Fetching user details",
                    processedHolder.getValue(),
                    (long) userIdList.size()));

            List<UserAccount> users = uaClient.getUserAccounts(subl);
            processedHolder.setValue(processedHolder.getValue() + users.size());

            return users;
        });
    }

}
