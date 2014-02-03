/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.command;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.branding.BrandingCacheUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.branding.client.BrandingClient;
import se.dabox.service.common.cache.GlobalCacheManager;
import se.dabox.service.common.cache.LazyGlobalCacheCommand;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class GetOrgBrandingCommand {
    private final RequestCycle cycle;

    public GetOrgBrandingCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    public Branding forOrg(long orgId) {
        return new LazyGlobalCacheCommand<Long, Branding>(BrandingCacheUtil.BRANDING_CACHE).get(
                GlobalCacheManager.getCache(cycle), orgId, new Transformer<Long, Branding>() {

            @Override
            public Branding transform(Long obj) {
                long brandingId = new GetOrgBrandingIdCommand(cycle).forOrg(obj);

                return getBrandingClient().getBranding(brandingId);
            }
        });
        
    }

    private BrandingClient getBrandingClient() {
        return AbstractModule.getBrandingClient(cycle);
    }
}
