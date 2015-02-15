/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import se.dabox.service.common.ajaxlongrun.StatusCallable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.module.OrgMaterialJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocobox.cpweb.module.project.ProjectJsonModule;
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
import se.dabox.service.common.ajaxlongrun.AjaxJob;
import se.dabox.service.common.ajaxlongrun.AppAjaxLongOp;
import se.dabox.service.common.ajaxlongrun.FutureAjaxJob;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.crisp.OrgProjectInfoSource;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.crisp.GetCrispProjectProductCollaborationId;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.common.proddir.material.ProductMaterial;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.json.DataTablesJson;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.reportjs")
public class ProjectReportJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onAvailableReports(final RequestCycle cycle, String strProjectId) {
        final OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        List<ReportInfo> infos = new ArrayList<>();

        ProjectMaterialCoordinatorClient pmcClient
                = CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
        List<Material> mats
                = ProjectJsonModule.getProjectMaterials(pmcClient, project.getProjectId(), cycle);

        for (Material mat : mats) {
            if ("idproject".equals(mat.getNativeType())) {
                String url = cycle.urlFor(ProjectReportModule.class, "idProductReport", strProjectId, mat.getId());
                String title = mat.getTitle() + " - report for current project";
                infos.add(new ReportInfo(url, title));
            } else if (ProductMaterialConstants.NATIVE_SYSTEM.equals(mat.getNativeSystem()) && "EL0873".equals(mat.getId())) {
                String url = cycle.urlFor(ProjectReportModule.class, "sliiChallengeReport", strProjectId);
                String title = "SLII Challenge";
                infos.add(new ReportInfo(url, title));
            } else if (ProductMaterialConstants.NATIVE_SYSTEM.equals(mat.getNativeSystem())) {
                Product product = ((ProductMaterial)mat).getProduct();
                CrispContext crispCtx = DwsCrispContextHelper.getCrispContext(cycle, product);

                if (crispCtx != null) {
                    List<ReportInfo> productReports = new GetProjectCrispReports(cycle, product,
                            project).getReports();
                    infos.addAll(productReports);
                }
            }
        }

        return jsonTarget(toDatatables(infos));
    }

    private ByteArrayOutputStream toDatatables(List<ReportInfo> reports) {
        return new DataTablesJson<ReportInfo>() {

            @Override
            protected void encodeItem(ReportInfo item) throws IOException {
                generator.writeStringField("title", item.getTitle());
                generator.writeStringField("link", item.getUrl());
            }
        }.encodeToStream(reports);
    }

    @WebAction
    public RequestTarget onActivityReport(final RequestCycle cycle, String strProjectId) {
        final OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        final Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new AppAjaxLongOp<byte[]>() {

            @Override
            protected AjaxJob<byte[]> createJob() {
                StatusCallable<byte[]> task = new ReportCallable<byte[]>(cycle.getApplication()) {
                    private ActivityReportBuilder builder;

                    @Override
                    protected byte[] callInCycle(ServiceRequestCycle cycle) throws Exception {
                        builder
                                = new ActivityReportBuilder(cycle, project, userLocale);
                        return builder.getReport();
                    }

                    @Override
                    public Status getStatus() {
                        return builder == null ? null : builder.getStatus();
                    }
                };
                Future<byte[]> future = cycle.getApplication().getScheduler().submit(task);

                return new FutureAjaxJob<>(future, task);
            }

            @Override
            protected RequestTarget completeResponse(RequestCycle cycle, AjaxJob<byte[]> job) {
                byte[] data = job.getResult();
                return jsonTarget(data);
            }
        }.process(cycle);

//        StatusCallable<byte[]> task = null;
//
//        byte[] data = new ActivityReportBuilder(cycle, project).getReport();
//
//        return jsonTarget(data);
//
//
    }

    @WebAction
    public RequestTarget onProductReport(RequestCycle cycle, String strProjectId) {
        OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        final Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        checkPermission(cycle, project);
        StatusCallable<byte[]> task
                = ProductReportBuilder.getReportForProjectTask(cycle, project, userLocale);

        Future<byte[]> future = cycle.getApplication().getScheduler().submit(task);

        byte[] data;
        try {
            data = future.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException("Failed to generate report", ex);
        }

        return jsonTarget(data);
    }

    @WebAction
    public RequestTarget onIdProductReport(RequestCycle cycle, String strProjectId, String productId) {
        OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        Product product
                = ProductFetchUtil.getExistingProduct(getProductDirectoryClient(cycle), productId);

        String linkPrefix = cycle.urlFor(ProjectReportModule.class,
                ProjectReportModule.ACTION_ACTIVITYREPORT);

        FutureTask<byte[]> task
                = IdProjectStatusReport.getReportForProjectTask(cycle, product, project, linkPrefix);

        cycle.getApplication().getScheduler().submit(task);

        //byte[] data = new ProductReportBuilder(cycle).getReportForProject(project);
        byte[] data;
        try {
            data = task.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException("Failed to generate report", ex);
        }

        return jsonTarget(data);
    }

}
