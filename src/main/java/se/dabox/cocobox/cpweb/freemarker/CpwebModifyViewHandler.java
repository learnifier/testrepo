/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.freemarker;

import net.unixdeveloper.druwa.freemarker.ModifyViewDataHandler;
import net.unixdeveloper.druwa.freemarker.ViewDataEvent;
import se.dabox.cocobox.security.project.ProjectPermissionCheck;
import se.dabox.cocosite.druwa.security.ClientPortalSecurityNamespaceFactory;
import se.dabox.cocosite.druwa.security.ProjectSecurityNamespaceFactory;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.portalswitch.PortalSwitchInfoImpl;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.orgdir.client.OrgUnitInfo;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CpwebModifyViewHandler implements ModifyViewDataHandler {
    public static final String FREEMARKER_ATTRIBUTE_NAME_INFOHELPER = "infoHelper";

    @Override
    public void modifyViewData(ViewDataEvent event) {
        event.getMap().put("branding", new BrandingOutput());

        final InfoCacheHelper infoHelper =
                InfoCacheHelper.getInstance(event.getCycle());

        event.getMap().put(FREEMARKER_ATTRIBUTE_NAME_INFOHELPER, infoHelper);

        event.getMap().put(PortalSwitchInfoImpl.ATTRIBUTE_NAME,
                new PortalSwitchInfoImpl(event.getCycle()));

        activateClientPortalSecurityNamespace(event);
        activateProjectSecurityNamespace(event);
    }

    private void activateProjectSecurityNamespace(ViewDataEvent event) {
        OrgProject project = (OrgProject) event.getMap().get("project");
        if (project == null) {
            project = (OrgProject) event.getMap().get("prj");
        }
        if (project != null) {
            ProjectPermissionCheck check = ProjectPermissionCheck.fromCycle(event.getCycle());
            event.getMap().put("projectSecurity",
                    ProjectSecurityNamespaceFactory.createNamespace(project, check));
        }
    }

    private void activateClientPortalSecurityNamespace(ViewDataEvent event) {
        Long orgId = getOrgId(event);

        if (orgId != null) {
            event.getMap().put("portalSecurity",
                    ClientPortalSecurityNamespaceFactory.createNamespace(orgId));
        }

    }

    private Long getOrgId(ViewDataEvent event) {
        Object obj = event.getMap().get("org");
        if (obj instanceof MiniOrgInfo) {
            return ((MiniOrgInfo)obj).getId();
        } else if (obj instanceof OrgUnitInfo) {
            return ((OrgUnitInfo)obj).getId();
        }

        OrgProject project = (OrgProject) event.getMap().get("prj");

        if (project != null) {
            return project.getOrgId();
        }

        return null;
    }

}
