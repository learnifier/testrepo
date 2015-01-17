/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.text;

import java.util.Locale;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.lang.JsTextBundleRequestTarget;
import se.dabox.util.HybridLocaleUtils;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/text")
public class JsTextModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JsTextModule.class);

    @WebAction
    public RequestTarget onDefault(RequestCycle cycle, String strLocale) {
        return getBundle(cycle, CocoSiteConstants.DEFAULT_LANG_BUNDLE, strLocale);
    }

    private RequestTarget getBundle(RequestCycle cycle, String bundleName, String strLocale) throws IllegalStateException, IllegalArgumentException {
        Locale locale;

        try {
            locale = HybridLocaleUtils.toLocale(strLocale);
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Invalid locale specified. Falling back to default. Input: {}", strLocale);
            locale = CocoSiteConstants.DEFAULT_LOCALE;
        }

        return new JsTextBundleRequestTarget(bundleName, locale);
    }
}
