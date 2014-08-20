/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.branding;

import se.dabox.service.common.cache.CacheIdFactory;
import se.dabox.util.MultiKey;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class BrandingCacheUtil {
    public static final Integer BRANDING_CACHE = CacheIdFactory.newId();

    @SuppressWarnings("unchecked")
    public static MultiKey getBrandingCacheKey(long orgId) {
        return new MultiKey(BRANDING_CACHE, orgId);
    }

    private  BrandingCacheUtil() {
    }

}
