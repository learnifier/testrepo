/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.branding;

import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.ObjectUtils;
import se.dabox.cocobox.cpweb.CpBrandingConstants;
import se.dabox.cocobox.cpweb.command.GetOrgBrandingCommand;
import se.dabox.cocobox.cpweb.formdata.branding.BrandingColorForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.branding.GetCachedBrandingCommand;
import se.dabox.cocosite.branding.GetRealmBrandingId;
import se.dabox.cocosite.event.BrandingChangedListenerUtil;
import se.dabox.cocosite.event.OrgUnitChangedListenerUtil;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.common.cache.GlobalCacheManager;
import se.dabox.service.common.context.Configuration;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/branding")
public class BrandingModule extends AbstractWebAuthModule {

    @DefaultWebAction
    @WebAction
    public RequestTarget onLogo(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_BRANDING);

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("width", LogoModule.WIDTH);
        map.put("height", LogoModule.HEIGHT);
        map.put("saveurl", cycle.urlFor(LogoModule.class, LogoModule.SAVE_ACTION, strOrgId));

        DruwaFormValidationSession<BrandingColorForm> colorformsession =
                getValidationSession(BrandingColorForm.class, cycle);
        colorformsession.populateFromObject(getCurrentBrandingColors(cycle, org.getId()));
        map.put("colorformsess", colorformsession);

        return new FreemarkerRequestTarget("/branding/brandingSettings.html", map);
    }

    @WebAction
    public RequestTarget onColorChange(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_BRANDING);

        String reset = cycle.getRequest().getParameter("reset");
        if (reset != null) {
            return onResetColors(cycle, strOrgId);
        }

        DruwaFormValidationSession<BrandingColorForm> formsess =
                getValidationSession(BrandingColorForm.class, cycle);

        if (!formsess.process()) {
            return new WebModuleRedirectRequestTarget(BrandingModule.class, "logo", strOrgId);
        }

        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(org.getId());
        Map<String, String> map = new HashMap<>();
        map.put("cpPrimaryColor", formsess.getObject().getPrimarycolor());
        map.put("cpSecondaryColor", formsess.getObject().getSecondarycolor());
        map.put("cpNavColor", formsess.getObject().getNavcolor());
        map.put("upTopbarBackgroundColor", formsess.getObject().getTopbarcolor());

        getBrandingClient(cycle).updateBranding(branding.getBrandingId(),
                LoginUserAccountHelper.getUserId(cycle), map);
        BrandingChangedListenerUtil.triggerEvent(cycle, branding.getBrandingId());

        GlobalCacheManager.getCache(cycle).removeEntry(BrandingCacheUtil.getBrandingCacheKey(org.
                getId()));

        OrgUnitChangedListenerUtil.triggerEvent(cycle, org.getId());

        return new WebModuleRedirectRequestTarget(BrandingModule.class, "logo", strOrgId);
    }

    @WebAction
    public RequestTarget onResetColors(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_BRANDING);

        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(org.getId());

        Map<String, String> brandingMap = branding.getMetadata();
        brandingMap.put("cpPrimaryColor", null);
        brandingMap.put("cpSecondaryColor", null);
        brandingMap.put("cpNavColor", null);
        brandingMap.put("upTopbarBackgroundColor", null);

        getBrandingClient(cycle).updateBranding(branding.getBrandingId(),
                LoginUserAccountHelper.getUserId(cycle), brandingMap, true);
        BrandingChangedListenerUtil.triggerEvent(cycle, branding.getBrandingId());

        GlobalCacheManager.getCache(cycle).removeEntry(BrandingCacheUtil.getBrandingCacheKey(org.
                getId()));

        OrgUnitChangedListenerUtil.triggerEvent(cycle, org.getId());

        return new WebModuleRedirectRequestTarget(BrandingModule.class, "logo", strOrgId);
    }

    private BrandingColorForm getCurrentBrandingColors(RequestCycle cycle, long orgId) {
        BrandingColorForm bcf = new BrandingColorForm();
        bcf.setNavcolor("#B36666");
        bcf.setPrimarycolor("#B36666");
        bcf.setSecondarycolor("#B36666");
        bcf.setTopbarcolor("#B36666");
        
        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);

        Configuration config = DwsRealmHelper.getRealmConfiguration(cycle);

        Branding realmBranding
                = new GetCachedBrandingCommand(cycle).getBranding(new GetRealmBrandingId(cycle).
                        getBrandingId());

        bcf.setNavcolor(ObjectUtils.firstNonNull(
                branding.getMetadata().get("cpNavColor"),
                realmBranding.getMetadata().get("cpNavColor"),
                config.getValue(CpBrandingConstants.DEFAULT_NAV_COLOR)));
        bcf.setPrimarycolor(ObjectUtils.firstNonNull(
                branding.getMetadata().get("cpPrimaryColor"),
                realmBranding.getMetadata().get("cpPrimaryColor"),
                config.getValue(CpBrandingConstants.DEFAULT_PRIMARY_COLOR)));
        bcf.setSecondarycolor(ObjectUtils.firstNonNull(
                branding.getMetadata().get("cpSecondaryColor"),
                realmBranding.getMetadata().get("cpSecondaryColor"),
                config.getValue(CpBrandingConstants.DEFAULT_SECONDARY_COLOR)));
        bcf.setTopbarcolor(ObjectUtils.firstNonNull(
                branding.getMetadata().get("upTopbarBackgroundColor"),
                realmBranding.getMetadata().get("upTopbarBackgroundColor"),
                config.getValue(CpBrandingConstants.DEFAULT_TOPBAR_COLOR)));

        return bcf;
    }
}
