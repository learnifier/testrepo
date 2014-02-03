/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.freemarker;

import net.unixdeveloper.druwa.freemarker.ModifyViewDataHandler;
import net.unixdeveloper.druwa.freemarker.ViewDataEvent;
import se.dabox.cocosite.infocache.InfoCacheHelper;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CpwebModifyViewHandler implements ModifyViewDataHandler {
    public static final String FREEMARKER_ATTRIBUTE_NAME_INFOHELPER = "infoHelper";

    @Override
    public void modifyViewData(ViewDataEvent event) {
        event.getMap().put("branding", new BrandingOutput());

        final InfoCacheHelper infoHelper =
                InfoCacheHelper.getInstance(event.getCycle());

        event.getMap().put(FREEMARKER_ATTRIBUTE_NAME_INFOHELPER, infoHelper);
    }

}
