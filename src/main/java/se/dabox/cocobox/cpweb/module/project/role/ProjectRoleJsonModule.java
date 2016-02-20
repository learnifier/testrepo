/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.security.CocoboxSecurityConstants;
import se.dabox.cocobox.security.user.UserAccountRoleCheck;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.CacheClients;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.user.MiniUserInfo;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.rolejson")
public class ProjectRoleJsonModule extends AbstractJsonAuthModule {
    private static final int MAX_RESULT = 20;

    @WebAction
    public RequestTarget onSearchUser(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo miniOrg = secureGetMiniOrg(cycle, strOrgId);

        String query = StringUtils.trimToNull(cycle.getRequest().getParameter("term"));

        List<UserAccount> matching;

        SearchContext ctx = new SearchContext(cycle, query, miniOrg);
            matching = getJsonUsers(ctx);

        return toJson(matching);
    }

    private List<UserAccount> getJsonUsers(SearchContext ctx) {
        List<UserAccount> users = getSortedMatching(ctx);

        return users.subList(0, Math.min(users.size(), MAX_RESULT));
    }

    private List<UserAccount> getMatching(SearchContext ctx) {
        Set<UserAccount> all = new HashSet<>();

        all.addAll(findClientMatching(ctx));
        all.addAll(findBoMatching(ctx));

        return new ArrayList<>(all);
    }

    private List<UserAccount> findClientMatching(SearchContext ctx) {
        final long userId = LoginUserAccountHelper.getUserId(ctx.cycle);
        final String orgRole = OrgRoleName.forOrg(ctx.org.getId()).toString();

        List<UserAccount> uas = getUserAccountService(ctx).searchUserAccounts(userId,
                ctx.term,
                CocoSiteConstants.UA_PROFILE,
                orgRole,
                CocoboxSecurityConstants.USER_ROLE, 0, MAX_RESULT);

        return uas;
    }

    private UserAccountService getUserAccountService(SearchContext ctx) {
        UserAccountService uaClient = CacheClients.getClient(ctx.cycle, UserAccountService.class);

        return uaClient;
    }

    private RequestTarget toJson(List<UserAccount> users) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        InfoCacheHelper infoHelper = InfoCacheHelper.getInstance(cycle);
        Set<Long> userIds = CollectionsUtil.transform(users, UserAccount::getUserId);
        Map<Long, MiniUserInfo> infoMap = infoHelper.getMiniUserInfos(userIds);
        final DataTablesJson<UserAccount> dataTablesJson
                = new DataTablesJson<UserAccount>() {

                    @Override
                    protected void encodeItem(UserAccount item) throws IOException {
                        generator.writeStringField("id", item.getPrimaryEmail());
                        generator.writeStringField("text", item.getDisplayName());
                        generator.writeStringField("email", item.getPrimaryEmail());
                        generator.writeStringField("type", "clientadmin");
                        MiniUserInfo info = infoMap.get(item.getUserId());
                        String tnUrl = info == null ? null : info.getThumbnail();
                        generator.writeStringField("thumbnail", tnUrl);
                    }

                    @Override
                    protected void writeExtraDataBeginning() throws IOException {
                        super.writeExtraDataBeginning();
                        generator.writeBooleanField("more", false);
                        generator.writeStringField("context", "cocobox");
                    }

                };
        dataTablesJson.setArrayName("results");

        ByteArrayOutputStream json = dataTablesJson.encodeToStream(users);

        return jsonTarget(json);
    }

    private Collection<? extends UserAccount> findBoMatching(SearchContext ctx) {
        UserAccount account = LoginUserAccountHelper.getUserAccount(ctx.cycle);

        if (!UserAccountRoleCheck.isBoAdmin(account)) {
            return Collections.emptyList();
        }

        List<UserAccount> uas = getUserAccountService(ctx).
                searchUserAccounts(account.getUserId(), ctx.term,
                null, null, CocoboxSecurityConstants.BOADMIN_LOGIN_ROLE, 0,
                MAX_RESULT);

        return uas;

    }

    private List<UserAccount> getSortedMatching(SearchContext ctx) {
        List<UserAccount> users = getMatching(ctx);

        Collections.sort(users, (UserAccount o1, UserAccount o2) ->
                new CompareToBuilder().append(o1.getDisplayName(), o2.getDisplayName()).
                        append(o1.getPrimaryEmail(), o2.getPrimaryEmail()).
                        append(o1.getUserId(), o2.getUserId()).
                        build());

        return users;
    }

    private static class SearchContext {

        final RequestCycle cycle;
        final String term;
        final MiniOrgInfo org;

        public SearchContext(RequestCycle cycle, String term, MiniOrgInfo org) {
            this.cycle = cycle;
            this.term = term;
            this.org = org;
        }

    }
}
