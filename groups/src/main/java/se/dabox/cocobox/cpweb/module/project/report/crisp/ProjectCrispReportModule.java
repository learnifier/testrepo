/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report.crisp;

import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.cocobox.crisp.datasource.OrgUnitSource;
import se.dabox.cocobox.crisp.datasource.PdProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProductInfoSource;
import se.dabox.cocobox.crisp.datasource.StandardOrgUnitInfoSource;
import se.dabox.cocobox.crisp.method.GetProjectReportUrl;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocobox.crisp.runtime.DwsCrispExecutionHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.dws.client.JacksonHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.crisp.OrgProjectInfoSource;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.crisp.GetCrispProjectProductCollaborationId;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.crispreport")
public class ProjectCrispReportModule extends AbstractProjectWebModule {

    @WebAction
    public RequestTarget onReport(RequestCycle cycle, String strProjectId, String productId, String reportId) {
        final OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        Product product = ProductFetchUtil.getExistingProduct(pdClient, productId);

        CrispContext crispCtx = DwsCrispContextHelper.getCrispContext(cycle, product);

        if (crispCtx == null || crispCtx.getDescription().getMethods().getGetProjectReportUrl() == null) {
            return new ErrorCodeRequestTarget(404);
        }

        GetProjectReportUrl req = createCrispRequest(cycle, project, product, reportId);

        String url = getUrlFromCrisp(cycle, crispCtx, req);

        return new RedirectUrlRequestTarget(url);
    }

    private GetProjectReportUrl createCrispRequest(RequestCycle cycle, OrgProject project,
            Product product, String reportId) {

        OrgUnitSource orgUnit = getOrgUnit(cycle, project);
        ProductInfoSource productInfo = new PdProductInfoSource(product);
        OrgProjectInfoSource projectInfo = new OrgProjectInfoSource(project);
        String projectCollabId = new GetCrispProjectProductCollaborationId(cycle).
                getCollaborationId(project.getProjectId(), product.getId());

        GetProjectReportUrl req = new GetProjectReportUrl(orgUnit, productInfo, projectInfo,
                projectCollabId, reportId);

        return req;
    }

    private OrgUnitSource getOrgUnit(RequestCycle cycle, OrgProject project) {
        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        OrgUnitInfo ou = odClient.getOrgUnitInfo(project.getOrgId());

        return new StandardOrgUnitInfoSource(ou);
    }

    private String getUrlFromCrisp(RequestCycle cycle, CrispContext crispCtx,
            GetProjectReportUrl req) {
        DwsCrispExecutionHelper execHelper = new DwsCrispExecutionHelper(cycle, crispCtx);

        Locale locale = CocositeUserHelper.getUserLocale(cycle);
        Map<String, ?> resp = execHelper.executeJson(locale, req);

        String url = JacksonHelper.getString(resp,"url");

        return url;
    }
}
