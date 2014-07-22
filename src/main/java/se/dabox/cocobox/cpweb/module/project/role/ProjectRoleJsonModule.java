/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.role;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxSecurityConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.rolejson")
public class ProjectRoleJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onSearchUser(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo miniOrg = secureGetMiniOrg(cycle, strOrgId);

        String query = cycle.getRequest().getParameter("term");

        //TOOD: Handle too short answers
        SearchContext ctx = new SearchContext(cycle, query, miniOrg);

        List<UserAccount> matching = getMatching(ctx);

        return toJson(ctx, matching);
    }

    private List<UserAccount> getMatching(SearchContext ctx) {
        return findClientMatching(ctx);
    }

    private List<UserAccount> findClientMatching(SearchContext ctx) {
        final long userId = LoginUserAccountHelper.getUserId(ctx.cycle);
        final String orgRole = OrgRoleName.forOrg(ctx.org.getId()).toString();

        List<UserAccount> uas = getUserAccountService(ctx).searchUserAccounts(userId, ctx.term,
                CocoSiteConstants.UA_PROFILE, orgRole, CocoboxSecurityConstants.USER_ROLE, 0, 20);

        return uas;
    }

    private UserAccountService getUserAccountService(SearchContext ctx) {
        UserAccountService uaClient = CacheClients.getClient(ctx.cycle, UserAccountService.class);

        return uaClient;
    }

    private RequestTarget toJson(SearchContext ctx, List<UserAccount> users) {
        final DataTablesJson<UserAccount> dataTablesJson
                = new DataTablesJson<UserAccount>() {

                    @Override
                    protected void encodeItem(UserAccount item) throws IOException {
                        generator.writeNumberField("id", item.getUserId());
                        generator.writeStringField("text", item.getDisplayName());
                        generator.writeStringField("email", item.getPrimaryEmail());
                        generator.writeStringField("type", "clientadmin");
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
