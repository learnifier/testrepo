/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.project.report.crisp.ProjectCrispReportModule;
import se.dabox.cocobox.crisp.datasource.OrgUnitSource;
import se.dabox.cocobox.crisp.datasource.PdProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProjectInfoSource;
import se.dabox.cocobox.crisp.datasource.StandardOrgUnitInfoSource;
import se.dabox.cocobox.crisp.method.GetProjectReports;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocobox.crisp.runtime.DwsCrispExecutionHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.dws.client.JacksonHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.crisp.OrgProjectInfoSource;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.crisp.GetCrispProjectProductCollaborationId;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class GetProjectCrispReports {

    private final RequestCycle cycle;
    private final Product product;
    private final CrispContext crispCtx;
    private final OrgProject project;
    private final List<ReportInfo> reports = new ArrayList<>();


    public GetProjectCrispReports(RequestCycle cycle, Product product, OrgProject project) {
        this.cycle = cycle;
        this.product = product;
        this.project = project;
        crispCtx = DwsCrispContextHelper.getCrispContext(cycle, product);
    }

    List<ReportInfo> getReports() {
        if (crispCtx != null) {
            getProductReports();
        }

        return reports;
    }

    private void getProductReports() {
        if (crispCtx.getDescription().getMethods().getGetProjectReports() == null) {
            return;
        }

        GetProjectReports req = createRequest();
        if (req == null) {
            return;
        }

        List<ReportInfo> decoded = executeCrispCall(cycle, crispCtx, req);
        reports.addAll(decoded);
    }


    private List<ReportInfo> executeCrispCall(RequestCycle cycle, CrispContext crispCtx, GetProjectReports req)
            throws CrispException, IllegalStateException, IllegalArgumentException {
        DwsCrispExecutionHelper execHelper = new DwsCrispExecutionHelper(cycle, crispCtx);
        Locale locale = CocositeUserHelper.getUserLocale(cycle);
        Map<String, ?> resp = execHelper.executeJson(locale, req);

        return decodeResponse(resp);
    }

    private List<ReportInfo> decodeResponse(Map<String, ?> resp) {
        List<Map> reports = JacksonHelper.getList(resp, "reports", Map.class);

        List<ReportInfo> list = new ArrayList<>();

        for (Map report : reports) {
            String id = JacksonHelper.getString(report,"id");
            String title = JacksonHelper.getString(report,"title");
            String scope = JacksonHelper.getString(report,"scope");

            if (scope == null || !scope.contains("admin")) {
                continue;
            }

            String url = cycle.urlFor(ProjectCrispReportModule.class, "report", product.getId().
                    getId(), id);
            list.add(new ReportInfo(url, title));
        }

        return list;
    }

    private GetProjectReports createRequest() {
        OrgUnitSource orgUnit = getOrgUnitInfo(project.getOrgId());
        ProductInfoSource pdInfo = new PdProductInfoSource(product);
        ProjectInfoSource projectInfo = new OrgProjectInfoSource(project);
        String projectCollabId = new GetCrispProjectProductCollaborationId().getCollaborationId(
                project.getProjectId(), product.getId());
        if (projectCollabId == null) {
            return null;
        }
        GetProjectReports req = new GetProjectReports(orgUnit, pdInfo, projectInfo, projectCollabId);
        return req;
    }

    private OrgUnitSource getOrgUnitInfo(long orgId) {
        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);
        final OrgUnitInfo orgUnitInfo = odClient.getOrgUnitInfo(orgId);

        return new StandardOrgUnitInfoSource(orgUnitInfo);
    }

}
