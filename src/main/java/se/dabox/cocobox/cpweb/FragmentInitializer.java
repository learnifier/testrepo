/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

import java.util.HashMap;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.webutils.fragment.FragmentClient;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class FragmentInitializer {
    private static final String NAME = FragmentInitializer.class.getName();

    public void initFragments(RequestCycle cycle, long orgId) {
        HashMap<String, String> fragParams = new HashMap<>();
        fragParams.put("baseurl", cycle.getRequest().getContextUrl().toString()+'/');
        fragParams.put("userid", getUserId(cycle));
        fragParams.put("locale", CocositeUserHelper.getUserLocale(cycle).toString());
        
        String strOrgId = Long.toString(orgId);
        fragParams.put("orgId", strOrgId);

        getFragmentClient().
                getFragment(cycle, "cpUserInfoFlorida", fragParams);

        getFragmentClient().
                getFragment(cycle, "cpUserInfoMobileFlorida", fragParams);

        getFragmentClient().
                getFragment(cycle, "cpsearch", fragParams);

        getFragmentClient().
                getFragment(cycle, "cpMenuFlorida", fragParams);

        getFragmentClient().
                getFragment(cycle, "cpMenuMobileFlorida", fragParams);

        getFragmentClient().
                getFragment(cycle, "cpsearch2", fragParams);

        getFragmentClient().
                getFragment(cycle, "portalSwitch", fragParams);

        getFragmentClient().
                getFragment(cycle, "portalSwitchMobile", fragParams);

        setActivatedOrgFragment(cycle, orgId);
    }

    public static boolean hasOrgFragments(RequestCycle cycle, long orgId) {
        Long fragmentOrgId = (Long) cycle.getSession().getAttribute(NAME);

        return fragmentOrgId != null && fragmentOrgId.longValue() == orgId;
    }

    public static void setActivatedOrgFragment(RequestCycle cycle, Long orgId) {
        if (orgId == null) {
            cycle.getSession().removeAttribute(NAME);
        } else {
            cycle.getSession().setAttribute(NAME, orgId);
        }
    }

    private String getUserId(RequestCycle cycle) {
        return Long.toString(LoginUserAccountHelper.getUserId(cycle));
    }

    private FragmentClient getFragmentClient() {
        return DruwaApplication.get().getAttribute(CocoSiteConstants.FRAGMENT_CLIENT_ATTRIBUTE);
    }

}
