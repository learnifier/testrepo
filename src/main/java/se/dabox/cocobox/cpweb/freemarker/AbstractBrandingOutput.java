/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.freemarker;

import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.orgdir.client.OrgUnitInfo;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public abstract class AbstractBrandingOutput {

    protected long getOrgId(Object unwrappedObject) {
        if (unwrappedObject instanceof MiniOrgInfo) {
            return ((MiniOrgInfo)unwrappedObject).getId();
        } else {
            return ((OrgUnitInfo)unwrappedObject).getId();
        }
    }
}
