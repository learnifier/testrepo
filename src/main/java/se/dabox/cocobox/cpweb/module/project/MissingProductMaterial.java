/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project;

import java.util.Locale;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.common.ccbc.material.OrgMaterialConverter;
import se.dabox.service.common.config.ConfigCacheCdnImage;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class MissingProductMaterial implements Material {
    private final String productId;

    public MissingProductMaterial(String productId) {
        this.productId = productId;
    }

    @Override
    public String getNativeSystem() {
        return ProductMaterialConstants.NATIVE_SYSTEM;
    }

    @Override
    public String getNativeType() {
        return "missing";
    }

    @Override
    public String getId() {
        return productId;
    }

    @Override
    public String getTitle() {
        return "Missing product "+productId;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    @Override
    public String getThumbnail(int heightWidth) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        return new ConfigCacheCdnImage(OrgMaterialConverter.DEFAULT_THUMBNAIL).getUrl(cycle);
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getCompositeId() {
        return getNativeSystem() + '|' + productId;
    }

    @Override
    public String getWebsafeId() {
        return getId();
    }

}
