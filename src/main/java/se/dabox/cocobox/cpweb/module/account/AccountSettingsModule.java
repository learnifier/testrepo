/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.account;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.module.account.AccountSettingsBlock;
import se.dabox.cocosite.org.MiniOrgInfo;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/account")
public class AccountSettingsModule extends AbstractWebAuthModule {

    @WebAction(name = "settings")
    public RequestTarget onAccountSettings(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        AccountSettingsBlock.activateChangeName(cycle, map);
        AccountSettingsBlock.activateChangeLanguage(cycle, map);
        AccountSettingsBlock.activateChangeEmail(cycle, map);
        AccountSettingsBlock.activateChangePassword(cycle, map);
        AccountSettingsBlock.activateChangePicture(cycle, map, strOrgId);
        AccountSettingsBlock.activateSocial(cycle, map);

        map.put("org", org);

        return new FreemarkerRequestTarget("/account/accountSettingsGeneral.html", map);
    }

}
