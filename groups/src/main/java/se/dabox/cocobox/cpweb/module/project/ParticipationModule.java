/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebActionMountpoint;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.crisppart.ParticipationCrispProductReport;
import se.dabox.service.common.ccbc.participation.crisppart.ReportScopes;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.part")
public class ParticipationModule extends AbstractWebAuthModule {

    @WebActionMountpoint("/preport")
    @WebAction
    public RequestTarget onParticipationReport(RequestCycle cycle, String strParticipationId,
            String strReportId) {

        long partId = Long.valueOf(strParticipationId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        ProjectParticipation participation =
                ccbc.getProjectParticipation(partId);

        OrgProject project = ccbc.getProject(participation.getProjectId());
        checkPermission(cycle, project);

        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);

        final long userId = LoginUserAccountHelper.getUserId(cycle);
        final long reportId = Long.valueOf(strReportId);

        String url;
        try {
            url = pmcClient.getParticipationCrispProductReportUrl(userId, partId, reportId,
                    ReportScopes.ADMIN);            
        } catch (Exception ex) {
            Product product = getReportProductOrNull(cycle, reportId);
            
            ErrorState state = new ErrorState(project.getOrgId(), product, ex, project.getProjectId());
            return NavigationUtil.getIntegrationErrorPage(cycle, state);
        }

        try {
            URI parsedUrl = new URL(url).toURI();

            if (!parsedUrl.isAbsolute()) {
                throw new URISyntaxException(url,
                        "URL from integration partner is not a valid url (not absolute)");
            }

        } catch (MalformedURLException | URISyntaxException ex) {
            Product product = getReportProductOrNull(cycle, reportId);

            ErrorState state = new ErrorState(project.getOrgId(), product, ex, project.
                    getProjectId());
            return NavigationUtil.getIntegrationErrorPage(cycle, state);
        }

        return new RedirectUrlRequestTarget(url);
    }

    private Product getReportProductOrNull(RequestCycle cycle, long reportId) {
        ParticipationCrispProductReport report =
                getProjectMaterialCoordinatorClient(cycle).getParticipationCrispProductReport(reportId);

        if (report == null) {
            return null;
        }

        return getProductDirectoryClient(cycle).getProduct(report.getProductId());
    }
}
