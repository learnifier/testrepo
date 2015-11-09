/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import se.dabox.service.branding.client.Branding;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class OrgBranding {
    private final long orgId;
    private final Branding branding;
    private final String logo;

    public OrgBranding(long orgId, Branding branding, String logo) {
        this.orgId = orgId;
        this.branding = branding;
        this.logo = logo;
    }

    public long getOrgId() {
        return orgId;
    }

    public Branding getBranding() {
        return branding;
    }

    public String getLogo() {
        return logo;
    }

}
