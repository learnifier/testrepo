/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import java.util.HashMap;
import java.util.Map;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.common.context.Configuration;
import se.dabox.service.common.mailsender.mailtemplate.CommonMailVariableConstants;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AdminRegistrationMailVariables {

    public Map<String,String> produceFor(Configuration config, UserAccount account, OrgUnitInfo org) {
        ParamUtil.required(account,"account");
        ParamUtil.required(org,"org");
        Map<String, String> map = new HashMap<>();

        map.put(CommonMailVariableConstants.NAME, account.getDisplayName());
        map.put(CommonMailVariableConstants.GIVEN_NAME, account.getGivenName());
        map.put(CommonMailVariableConstants.SURNAME, account.getSurname());
        map.put(CommonMailVariableConstants.EMAIL, account.getPrimaryEmail());

        map.put(CommonMailVariableConstants.ORGNAME, org.getDisplayName());

        map.put("registrationlink", config.getValue(CocoSiteConstants.CPWEB_ADMIN_REGISTRATION));

        return map;
    }

}
