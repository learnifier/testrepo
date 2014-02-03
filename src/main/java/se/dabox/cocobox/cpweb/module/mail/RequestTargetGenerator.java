/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import java.io.Serializable;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public interface RequestTargetGenerator extends Serializable {

    public RequestTarget generateTarget(RequestCycle cycle);

}
