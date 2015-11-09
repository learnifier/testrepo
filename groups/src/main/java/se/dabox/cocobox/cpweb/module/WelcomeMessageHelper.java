/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.DwsDevelopmentMode;
import se.dabox.service.common.cache.CacheIdFactory;
import se.dabox.service.common.cache.GlobalCache;
import se.dabox.service.common.cache.GlobalCacheManager;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.WelcomeMessage;
import se.dabox.service.orgadmin.client.AdminGroupUser;
import se.dabox.service.orgadmin.client.OrgAdminClient;
import se.dabox.util.MultiKey;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class WelcomeMessageHelper {

    private static final Integer CACHE_ID = CacheIdFactory.newId();
    private static final long CACHE_TIME = TimeUnit.MINUTES.toMillis(30);

    public static List<WelcomeMessage> getWelcomeMessages(RequestCycle cycle, long orgId) {

        MultiKey<Object> key = new MultiKey<Object>(CACHE_ID, orgId);
        GlobalCache cache = GlobalCacheManager.getCache(cycle);
        @SuppressWarnings("unchecked")
        List<WelcomeMessage> messages = cache.getEntry(key, List.class);

        if (messages != null) {
            return messages;
        }

        messages = getWelcomeMessagesDirect(cycle, orgId);

        if (!DwsDevelopmentMode.isDevelopmentMode()) {
            cache.putEntry(key, messages, CACHE_TIME);
        }

        return messages;
    }
    
    public static WelcomeMessage getWelcomeMessage(RequestCycle cycle, long orgId) {
        List<WelcomeMessage> messages = getWelcomeMessages(cycle, orgId);

        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(new Random().nextInt(messages.size()));
    }

    private static List<WelcomeMessage> getWelcomeMessagesDirect(RequestCycle cycle, long orgId) {
        OrgAdminClient oaClient = CacheClients.getClient(cycle, OrgAdminClient.class);
        List<AdminGroupUser> agUsers = oaClient.listOrgAdminGroupUsers(orgId);

        agUsers = CollectionsUtil.sublist(agUsers, new Predicate<AdminGroupUser>() {
            @Override
            public boolean evalute(AdminGroupUser item) {
                return "leader".equals(item.getRoleName()) && "lscontact".equals(item.
                        getGroupName());
            }
        });

        if (agUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = CollectionsUtil.transformList(agUsers,
                new Transformer<AdminGroupUser, Long>() {
                    @Override
                    public Long transform(AdminGroupUser item) {
                        return item.getUserId();
                    }
                });

        CocoboxCoordinatorClient ccbc =
                CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        List<WelcomeMessage> messages = ccbc.getWelcomeMessages(userIds);

        if (messages.size() == userIds.size()) {
            return messages;
        }

        Set<Long> userSet = new HashSet<>(userIds);
        for (WelcomeMessage welcomeMessage : messages) {
            userSet.remove(welcomeMessage.getUserId());
        }

        for (Long userId : userSet) {
            messages.add(new WelcomeMessage(userId, null));
        }

        return messages;
    }

    private WelcomeMessageHelper() {
    }
}
