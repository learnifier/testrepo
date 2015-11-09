/*
* (c) Dabox AB 2015 All Rights Reserved
*/
package se.dabox.cocobox.cpweb.module.cug;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.cug.client.ClientUserGroup;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@FunctionalInterface
public interface MemberOpsInterface {
   RequestTarget call(RequestCycle cycle, MiniOrgInfo org, ClientUserGroup cug, int[] ids);
}
