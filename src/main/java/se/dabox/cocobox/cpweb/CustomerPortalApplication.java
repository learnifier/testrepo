/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb;

import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.automounter.Automounter;
import net.unixdeveloper.druwa.formbean.FormBeanContext;
import net.unixdeveloper.druwa.freemarker.DruwaFreemarker;
import net.unixdeveloper.druwa.mountpoint.RootRequestMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.converter.ColorPickerConverter;
import se.dabox.cocobox.cpweb.converter.SimpleEmailConverter;
import se.dabox.cocobox.cpweb.druwa.AlternativeCpwebListener;
import se.dabox.cocobox.cpweb.freemarker.CpwebModifyViewHandler;
import se.dabox.cocobox.cpweb.freemarker.LearnifierBootstrap;
import se.dabox.cocobox.cpweb.module.OrgSelectorModule;
import se.dabox.cocosite.converter.DateConverter;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DefaultCocositeInitialization;
import se.dabox.cocosite.login.LoginHandler;
import se.dabox.cocosite.module.account.picture.PictureModule;
import se.dabox.cocosite.portalswitch.PortalSwitchAjaxModule;
import se.dabox.service.common.ccbc.mailfilter.MailFilterTarget;
import se.dabox.service.webutils.druwa.DwsWebsiteConstants;
import se.dabox.service.webutils.druwa.DwsWebsiteHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CustomerPortalApplication extends DruwaApplication {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CustomerPortalApplication.class);
    
    @Override
    protected void init() {
        LOGGER.info("init");

        DwsWebsiteHelper.registerDefaultServices(this, CocoSiteConstants.DEFAULT_LANG_BUNDLE).
                freemarkerDirectives("cocosite").register();
        DefaultCocositeInitialization.init(this);

        addRequestBeginEventListener(new AlternativeCpwebListener());

        setAttribute(LoginHandler.ATTRIBUTE_NAME, new LoginHandler());

        initConverters();

        DruwaFreemarker.getDruwaFreemarker(this).getConfig().addAutoInclude("/autoinclude.ftl");
        DruwaFreemarker.getDruwaFreemarker(this).addModifyViewDataHandler(new CpwebModifyViewHandler());
        DruwaFreemarker.getDruwaFreemarker(this).getConfig().
                setSharedVariable("learnifierBootstrap", LearnifierBootstrap.getInstance());
        
        getWebModuleRegistry().
                addSearchPrefix("se.dabox.cocobox.cpweb.module");
        getWebModuleRegistry().
                addSearchPrefix("se.dabox.cocobox.cpweb.module.project");
        getWebModuleRegistry().
                addSearchPrefix("se.dabox.cocobox.cpweb.module.mail");
        getWebModuleRegistry().
                addSearchPrefix("se.dabox.cocobox.cpweb.module.user");
        getWebModuleRegistry().addSearchPrefix("se.dabox.cocosite.module");

        setAttribute(PictureModule.AFTER_IMAGE_UPLOAD_ATTRIBUTE,
                new CpPostUpdateImageAction());
        
        Automounter.autoMount(this);

        addRequestMountpoint(new RootRequestMountpoint(OrgSelectorModule.class));
        PortalSwitchAjaxModule.registerModule(this);

    }

    private void initConverters() {
        FormBeanContext formBeanContext = getAttribute(
                DwsWebsiteConstants.FORMBEAN_CONTEXT);

        formBeanContext.getConverterSession().addConverter(String.class,
                MailFilterTarget.class,
                new MailFilterTarget.MailFilterTargetConverter());

        formBeanContext.getConverterSession().addNamedConverter("iso8601date",
                DateConverter.getIso8601Converter());

        formBeanContext.getConverterSession().addNamedConverter("colorpicker",
                new ColorPickerConverter());

        formBeanContext.getConverterSession().addNamedConverter("simpleEmail",
                new SimpleEmailConverter());
    }

}
