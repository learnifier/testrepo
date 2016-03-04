/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import se.dabox.service.common.ajaxlongrun.StatusCallable;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocobox.cpweb.module.project.ProjectJsonModule;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ajaxlongrun.AjaxJob;
import se.dabox.service.common.ajaxlongrun.AppAjaxLongOp;
import se.dabox.service.common.ajaxlongrun.FutureAjaxJob;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.filter.FilterProjectRequest;
import se.dabox.service.common.ccbc.project.filter.FilterProjectRequestBuilder;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.common.proddir.material.ProductMaterial;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.freemarker.text.JavaCocoText;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.reportjs")
public class ProjectReportJsonModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectReportJsonModule.class);

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

        infos.add(new ReportInfo(cycle.urlFor(ProjectReportModule.class, "activityReport", Long.
                toString(project.getProjectId())), "Project Status", false));

        for (Material mat : mats) {
            if ("idproject".equals(mat.getNativeType())) {
                String url = cycle.urlFor(ProjectReportModule.class, "idProductReport", strProjectId, mat.getId());
                String title = mat.getTitle() + " - report for current project";
                infos.add(new ReportInfo(url, title, false));
            } if ("challenge".equals(mat.getNativeType())) {
                infos.addAll(getChallengeReports(cycle, project.getProjectId(), mat));
            } else if (ProductMaterialConstants.NATIVE_SYSTEM.equals(mat.getNativeSystem()) && "EL0873".equals(mat.getId())) {
                String url = cycle.urlFor(ProjectReportModule.class, "sliiChallengeReport", strProjectId);
                String title = "SLII Challenge";
                infos.add(new ReportInfo(url, title, false));
            } else if (mat instanceof ProductMaterial) {
                Product product = ((ProductMaterial)mat).getProduct();
                CrispContext crispCtx = DwsCrispContextHelper.getCrispContext(cycle, product);

                if (crispCtx != null) {
                    try {
                        List<ReportInfo> productReports = new GetProjectCrispReports(cycle, mat,
                                project).getReports();
                        infos.addAll(productReports);
                    } catch (CrispException cex) {
                        LOGGER.warn("Failed to get crisp project reports for {}/{}",
                                project.getProjectId(), mat.getCompositeId());
                    }
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
                generator.writeBooleanField("ownWindow", item.isOwnWindow());
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

    private Collection<? extends ReportInfo> getChallengeReports(RequestCycle cycle, long projectId, Material mat) {
        String productId = mat.getId();

        FilterProjectRequest fpr = new FilterProjectRequestBuilder().
                setProductId(productId).
                setMasterProject(projectId).
                buildRequest();

        List<OrgProject> projects = getCocoboxCordinatorClient(cycle).listOrgProjects(fpr);

        JavaCocoText ctext = new JavaCocoText();

        final String strProjectId = Long.toString(projectId);

        List<ReportInfo> reports = CollectionsUtil.transformList(projects, p -> {
            String projectName = new GetProjectAdministrativeName(cycle).getName(p);
            String reportName = ctext.get("cpweb.project.report.challengeactivity.title", projectName);

            String url = cycle.
                    urlFor(ProjectReportModule.class, "innerActivityReport", strProjectId, Long.
                            toString(p.getProjectId()));

            return new ReportInfo(url, reportName, false);
        });

        return reports;
    }

}
