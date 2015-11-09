/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.event;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.FragmentInitializer;
import se.dabox.cocosite.event.LoginUserAccountListener;
import se.dabox.service.login.client.UserAccount;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CpLoginUserAccountListener implements LoginUserAccountListener {

    @Override
    public void loginAccountUpdated(RequestCycle cycle, UserAccount account) {
        FragmentInitializer.setActivatedOrgFragment(cycle, null);
    }

}
