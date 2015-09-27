/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.WebSession;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.Blank;
import se.dabox.cocobox.cpweb.formdata.MaterialsForm;
import se.dabox.cocobox.cpweb.formdata.account.ChangePassword;
import se.dabox.cocobox.cpweb.formdata.material.AddLinkCreditsForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.deeplink.ProductMaterialJsonModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.dws.client.ApiHelper;
import se.dabox.service.common.ccbc.ProjectStatus;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.login.client.LoginService;
import se.dabox.service.login.client.LoginServiceImpl;
import se.dabox.service.orgdir.client.OrgUnitInfo;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/site")
public class CpMainModule extends AbstractWebAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CpMainModule.class);
    public static final String LIST_USERS_ACTION = "listUsers";
    public static final String LIST_MATERIALS = "listMaterials";
    public static final String LIST_PROJECTS = "listProjects";
    public static final String LIST_EMAILS = "listEmails";
    public static final String LIST_DESIGNS = "listDesigns";
    public static final String ACCOUNT_SETTINGS = "accountSettings";

    public CpMainModule() {
        super();

        //getLoginChecker().setLoginUrlGenerator(new MainBrandingLoginUrlGenerator());
    }

    @DefaultWebAction
    @WebAction
    public RequestTarget onHome(RequestCycle cycle, String id) {
        if (id == null) {
            return NavigationUtil.toOrgSelector();
        }

        Map<String, Object> map = createMap();

        MiniOrgInfo org = secureGetMiniOrg(cycle, id);

        map.put("org", org);
        map.put("welcomeMessage", WelcomeMessageHelper.getWelcomeMessage(cycle, org.getId()));

        return new FreemarkerRequestTarget("/home.html", map);
    }
    
    @WebAction
    @WebActionMountpoint("/search")
    public RequestTarget onSearch(RequestCycle cycle, String id) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, id);

        final String query = cycle.getRequest().getParameter("q");

        if (StringUtils.isBlank(query)) {
            return NavigationUtil.toOrgMain(id);
        }

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("query", query);

        return new FreemarkerRequestTarget("/search/searchResults.html", map);
    }

    @WebAction
    public RequestTarget onViewOrgMaterial(RequestCycle cycle, String strOrgId, String strMaterialId) {
        checkOrgPermission(cycle, strOrgId);
        Long orgId = Long.valueOf(strOrgId);

        Long matId = Long.valueOf(strMaterialId);

        List<OrgMaterial> materials =
                getCocoboxCordinatorClient(cycle).listOrgMaterial(orgId);

        for (OrgMaterial orgMaterial : materials) {
            if (matId == orgMaterial.getOrgMaterialId()) {
                String url;

                if ("link".equals(orgMaterial.getType())) {
                    url = orgMaterial.getWeblink();
                } else {
                    url = getContentRepoClient().getDownloadUrl(orgMaterial.getCrlink());
                }

                return new RedirectUrlRequestTarget(url);
            }
        }

        return new ErrorCodeRequestTarget(404);
    }

    @WebAction
    public RequestTarget onListMaterials(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_LIST_ORGMATS);

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("formsess", getValidationSession(Blank.class, cycle));
        map.put("linkformsess", getValidationSession(AddLinkCreditsForm.class, cycle));
        map.put("addLinkCreditsFormLink", cycle.
                urlFor(ProductMaterialJsonModule.class, "addCredits", strOrgId));

        return new FreemarkerRequestTarget("/material/listMaterials.html", map);
    }

    @WebAction
    public RequestTarget onGridMaterials(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_LIST_ORGMATS);

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("formsess", getValidationSession(Blank.class, cycle));
        map.put("linkformsess", getValidationSession(AddLinkCreditsForm.class, cycle));
        map.put("addLinkCreditsFormLink", cycle.
                urlFor(ProductMaterialJsonModule.class, "addCredits", strOrgId));

        return new FreemarkerRequestTarget("/material/gridMaterials.html", map);
    }
    
    @WebAction
    public RequestTarget onIsoGridMaterials(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_LIST_ORGMATS);

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("formsess", getValidationSession(Blank.class, cycle));
        map.put("linkformsess", getValidationSession(AddLinkCreditsForm.class, cycle));
        map.put("addLinkCreditsFormLink", cycle.
                urlFor(ProductMaterialJsonModule.class, "addCredits", strOrgId));

        return new FreemarkerRequestTarget("/material/isotoped-gridMaterials.html", map);
    }
    
    
    @WebAction
    public RequestTarget onListProdDeeplinks(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("formsess", getValidationSession(Blank.class, cycle));
        map.put("linkformsess", getValidationSession(AddLinkCreditsForm.class, cycle));
        map.put("addLinkCreditsFormLink", cycle.
                urlFor(ProductMaterialJsonModule.class, "addCredits", strOrgId));

        return new FreemarkerRequestTarget("/deeplink/listProdDeeplinks.html", map);
    }

    @WebAction
    public RequestTarget onListOrgMatDeeplinks(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("formsess", getValidationSession(Blank.class, cycle));
        map.put("linkformsess", getValidationSession(AddLinkCreditsForm.class, cycle));
        map.put("addLinkCreditsFormLink", cycle.
                urlFor(ProductMaterialJsonModule.class, "addCredits", strOrgId));

        return new FreemarkerRequestTarget("/deeplink/listOrgMatDeeplinks.html", map);
    }    
    
    
    @WebAction
    public RequestTarget onListProjects(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("org", org);

        String projectFilter = "";
        if ("archived".equals(cycle.getRequest().getParameter("f"))) {
            projectFilter = "archived";
        }

        map.put("projectFilter", projectFilter);

        return new FreemarkerRequestTarget("/project/listProjects.html", map);
    }

    @WebAction
    public RequestTarget onListUsers(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("org", org);

        return new FreemarkerRequestTarget("/user/listUsers.html", map);
    }

    @WebAction
    public RequestTarget onListReports(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("org", org);

        return new FreemarkerRequestTarget("/report/listReports.html", map);
    }

    @WebAction
    public RequestTarget onListSettings(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(ChangePassword.class, cycle));
        map.put("formLink", "");
        map.put("org", org);

        return new FreemarkerRequestTarget("/settings/listSettings.html", map);
    }

    @WebAction
    public RequestTarget onListEmails(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_LIST_EMAILS);

        Map<String, Object> map = createMap();

        map.put("org", org);

        return new FreemarkerRequestTarget("/email/listEmails.html", map);
    }

    @WebAction
    public RequestTarget onListDesigns(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.CP_LIST_COURSEDESIGNS);

        Map<String, Object> map = createMap();

        map.put("org", org);

        return new FreemarkerRequestTarget("/design/listDesigns.html", map);
    }

    @WebAction
    public RequestTarget onNewMaterial(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("formsess", getValidationSession(MaterialsForm.class, cycle));
        map.put("org", org);
        map.put("formLink", cycle.urlFor(UploadMaterialModule.class.getName(),
                "newMaterialPost", strOrgId));
        map.put("types", OrgMaterialTypes.getTypes());

        return new FreemarkerRequestTarget("/material/createMaterial.html", map);
    }

    @WebAction
    public RequestTarget onLogout(RequestCycle cycle, String strOrgId) {
        OrgUnitInfo org = securedGetOrganization(cycle, strOrgId);

        ApiHelper helper = DwsRealmHelper.getRealmApiHelper(cycle);
        String url = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                "loginservice.url");
        LoginService ls = new LoginServiceImpl(helper, url);
        String logoutTarget = getLogoutTargetUrl(cycle, org);

        String link = ls.logout(logoutTarget);

        cycle.getSession().invalidate();
        return new RedirectUrlRequestTarget(link);
    }

    @WebActionMountpoint("/tobo")
    @WebAction
    public RequestTarget onTobo(RequestCycle cycle) {

        String baseurl = getConfValue(cycle, "boweb.baseurl");

        WebSession session = cycle.getSession(false);
        if (session != null) {
            LOGGER.debug("Invalidating session before jumping to back office");
            session.invalidate();
        }

        return new RedirectUrlRequestTarget(baseurl);
    }

    private String getLogoutTargetUrl(RequestCycle cycle, OrgUnitInfo org) {
        return getConfValue(cycle, "cpweb.logout.url");
    }

    private String getProjectStatus(WebRequest request) {
        String type = request.getParameter("type");

        if (type == null) {
            return ProjectStatus.ACTIVE.name();
        } else {
            try {
                return ProjectStatus.valueOf(type).name();
            } catch (IllegalArgumentException ex) {
                return ProjectStatus.ACTIVE.name();
            }
        }
    }
    
}
