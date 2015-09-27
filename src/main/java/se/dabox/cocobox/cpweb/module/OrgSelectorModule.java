/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.cocobox.cpweb.command.GetOrgBrandingCommand;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.state.OrgBranding;
import se.dabox.cocobox.security.CocoboxSecurityConstants;
import se.dabox.cocosite.druwa.CocoSiteConfKey;
import se.dabox.cocosite.login.LoginHandler;
import se.dabox.cocosite.user.UserAccountTransformers;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class OrgSelectorModule extends AbstractWebAuthModule {

    @WebActionMountpoint("/os")
    @DefaultWebAction
    @WebAction
    public RequestTarget onSelect(final RequestCycle cycle) {

        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) cycle.getSession().getAttribute(LoginHandler.USER_ROLES);
        if (roles == null) {
            roles = Collections.emptySet();
        }

        if (roles.contains(CocoboxSecurityConstants.BOADMIN_LOGIN_ROLE)) {
            return new WebModuleRedirectRequestTarget(CpMainModule.class, "tobo");
        }

        List<Long> orgIds = getOrgIds(LoginUserAccountHelper.getUserAccount(cycle));

        final int size = orgIds.size();

        if (size == 0) {
            return new RedirectUrlRequestTarget(getConfValue(cycle, CocoSiteConfKey.UPWEB_BASEURL));
        } else if (size == 1) {
            return new WebModuleRedirectRequestTarget(CpMainModule.class, "home", Long.
                    toString(orgIds.get(0)));
        }


        List<OrgBranding> ob = CollectionsUtil.transformList(orgIds,
                new Transformer<Long, OrgBranding>() {
                    @Override
                    public OrgBranding transform(Long item) {
                        return orgIdToOrgBranding(cycle, item);
                    }
                });

        Map<String, Object> map = createMap();

        map.put("orgs", ob);

        return new FreemarkerRequestTarget("/orgSelector.html", map);
    }

    private List<Long> getOrgIds(UserAccount userAccount) {
        return UserAccountTransformers.getOrgAdminOrgs().transform(userAccount);
    }

    private static OrgBranding orgIdToOrgBranding(RequestCycle cycle, Long orgId) {
        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);

        String logoRef = branding.getGeneratedData().get("cphalfbanner");
        String logo = null;
        if (logoRef != null) {
            String crpubcdn = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                    "contentrepo.cocoboxpub.puburl");
            logo = crpubcdn + "/branding/" + branding.getBrandingId() + '/' + branding.
                    getGeneratedData().get("uuid") + logoRef;
        }

        return new OrgBranding(orgId, branding, logo);
    }
}
