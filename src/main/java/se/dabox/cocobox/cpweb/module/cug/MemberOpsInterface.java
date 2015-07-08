/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.cug;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.cug.client.ClientUserGroup;

/**
 *
 * @author Magnus Andersson <magnus.andersson@learnifier.com>
 */
@FunctionalInterface
public interface MemberOpsInterface {
   RequestTarget call(RequestCycle cycle, MiniOrgInfo org, ClientUserGroup cug, int[] ids);
}
