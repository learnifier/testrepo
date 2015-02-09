/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.branding;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.unixdeveloper.druwa.FileUpload;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.FileRequestTarget;
import net.unixdeveloper.druwa.request.StringRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.branding.GetOrgBrandingCommand;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.event.BrandingChangedListenerUtil;
import se.dabox.cocosite.event.OrgUnitChangedListenerUtil;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.dws.client.ApiHelper;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.cache.GlobalCacheManager;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.imageresizer.ImageHandle;
import se.dabox.service.common.imageresizer.ImageHandleBuffer;
import se.dabox.service.common.imageresizer.ImageProcessorClient;
import se.dabox.service.common.imageresizer.ImageProcessorException;
import se.dabox.service.common.imageresizer.ImageResizerClient;
import se.dabox.service.common.imageresizer.ImageResizerClientImpl;
import se.dabox.service.common.io.RuntimeIOException;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.contentrepo.util.ContentRepoUri;
import se.dabox.service.webutils.druwa.JsonRequestUtil;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/branding.logo")
public class LogoModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LogoModule.class);

    public static final String SAVE_ACTION = "saveImage";
    //Double sized half-banner (full-banner is only 60px high)
    public static final int WIDTH = 234;
    public static final int HEIGHT = 60;
    
    @WebAction
    public RequestTarget onUploadImage(RequestCycle cycle, String strOrgId) {

        checkOrgPermission(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_BRANDING);

        FileUpload image = cycle.getRequest().getFileUpload("image");

        Map<String, Object> map = createMap();
        PreviewState state = createPreview(cycle, image, WIDTH, HEIGHT);

        if (state == null) {
            map.put("status", "ERROR");
        } else {
            cycle.getSession().setAttribute(state.getSessionName(), state);
            map.put("status", "OK");
            map.put("preview", state.getUuid().toString());
            map.put("previewUrl", cycle.urlFor(LogoModule.class, "previewImage", strOrgId,
                    state.getUuid().toString()));
        }

        return fileUploadResponse(cycle, map);
    }

    @WebAction
    public RequestTarget onPreviewImage(RequestCycle cycle, String strOrgId, String previewId) {
        PreviewState state = (PreviewState) cycle.getSession().getAttribute(PreviewState.
                computeSessionName(previewId));

        return new FileRequestTarget(state.getPreviewFile(), "image/jpeg");
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onSaveImage(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_BRANDING);

        String reset = cycle.getRequest().getParameter("reset");
        if (reset != null) {
            return onResetImage(cycle, strOrgId);
        }

        String previewId = cycle.getRequest().getParameter("pid");
        PreviewState state = (PreviewState) cycle.getSession().getAttribute(PreviewState.
                computeSessionName(previewId));

        long brandingId = new GetOrgBrandingIdCommand(cycle).forOrg(org.getId());

        URI uri =
                ContentRepoUri.createUri(DwsRealmHelper.determineRequestRealm(cycle),
                CocoSiteConstants.INTERNAL_CONTENTREPO);

        uri = ContentRepoUri.addPath(uri, "/branding/" + brandingId + "/logo.image");
        try {
            getContentRepoClient().upload(uri, state.getRawFile());
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to upload logo", ex);
        }

        Map<String, Object> brandingMap = createMap();

        double x = getDouble(cycle, "x");
        double y = getDouble(cycle, "y");
        double w = getDouble(cycle, "w");
        double h = getDouble(cycle, "h");

        brandingMap.put("cpLogo", uri);
        brandingMap.put("cpLogo.x", relativize(x, 1.0));
        brandingMap.put("cpLogo.y", relativize(y, 1.0));
        brandingMap.put("cpLogo.w", relativize(w, 1.0));
        brandingMap.put("cpLogo.h", relativize(h, 1.0));

        getBrandingClient(cycle).updateBranding(brandingId, LoginUserAccountHelper.getUserId(cycle),
                brandingMap, true);
        BrandingChangedListenerUtil.triggerEvent(cycle, brandingId);

        cycle.getSession().removeAttribute(state.getSessionName());

        GlobalCacheManager.getCache(cycle).removeEntry(BrandingCacheUtil.getBrandingCacheKey(org.
                getId()));

        OrgUnitChangedListenerUtil.triggerEvent(cycle, org.getId());

        return NavigationUtil.toOrgMain(strOrgId);
    }

    @WebAction
    public RequestTarget onResetImage(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_EDIT_BRANDING);

        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(org.getId());

        Map<String, String> brandingMap = branding.getMetadata();
        Map<String, String> newBrandingMap = new HashMap<>(brandingMap);
        String oldImage = newBrandingMap.put("cpLogo", null);
        newBrandingMap.put("cpLogo.x", null);
        newBrandingMap.put("cpLogo.y", null);
        newBrandingMap.put("cpLogo.w", null);
        newBrandingMap.put("cpLogo.h", null);

        getBrandingClient(cycle).updateBranding(branding.getBrandingId(),
                LoginUserAccountHelper.getUserId(cycle), newBrandingMap, true);
        BrandingChangedListenerUtil.triggerEvent(cycle, branding.getBrandingId());

        if (oldImage != null) {
            getContentRepoClient().remove(oldImage);
        }

        GlobalCacheManager.getCache(cycle).removeEntry(BrandingCacheUtil.getBrandingCacheKey(org.
                getId()));

        OrgUnitChangedListenerUtil.triggerEvent(cycle, org.getId());

        return new WebModuleRedirectRequestTarget(BrandingModule.class, "logo", strOrgId);
    }

    private RequestTarget fileUploadResponse(RequestCycle cycle,
            Map<String, Object> map) {
         if (!JsonRequestUtil.isModernJsonCall(cycle)) {
            String json = JsonUtils.encode(map);
            String content = "<textarea>" + json + "</textarea>";
            return new StringRequestTarget("text/html", content);
        }
         
        return jsonTarget(map);
    }

    private PreviewState createPreview(RequestCycle cycle, FileUpload upload, int width, int height) {
        if (upload == null) {
            return null;
        }

        File rawFile = null;
        File previewFile = null;

        try {

            rawFile = File.createTempFile("cpweb-imagerawfile", ".tmp");
            upload.write(rawFile);
            previewFile = File.createTempFile("cpweb-imagepreview", ".png");

            ImageProcessorClient ipClient = CacheClients.
                    getClient(cycle, ImageProcessorClient.class);
            long userId = LoginUserAccountHelper.getUserId(cycle);
            ImageHandle handle = ipClient.createHandle(userId);

            try {
                ipClient.uploadImage(new ImageHandleBuffer(handle, 0), rawFile);
                ipClient.
                        createAspectBestFitThumbail(userId, new ImageHandleBuffer(handle, 0), width,
                        height, "png", 1);
                ipClient.downloadFile(new ImageHandleBuffer(handle, 1), previewFile);
            } finally {
                ipClient.disposeHandle(handle);
            }

            return new PreviewState(UUID.randomUUID(), rawFile, previewFile,
                    0, 0);
        } catch(ImageProcessorException ex) {
            LOGGER.warn("Image upload failed", ex);
            return null;
        } catch (IOException ex) {
            FileUtils.deleteQuietly(rawFile);
            FileUtils.deleteQuietly(previewFile);
            return null;
        }
    }

    private ImageResizerClient getImageResizerClient(RequestCycle cycle) {
        ApiHelper apiHelper = DwsRealmHelper.getRealmApiHelper(cycle);
        String url = DwsRealmHelper.getRealmConfiguration(cycle).getValue("imageresizer.url");

        return new ImageResizerClientImpl(apiHelper, url);
    }

    private double getDouble(RequestCycle cycle, String name) {
        String strVal = cycle.getRequest().getParameter(name);

        return Double.valueOf(strVal);
    }

    private int relativize(double point, double ratio) {
        double newSize = point * ratio;

        return (int) Math.ceil(newSize);
    }
}
