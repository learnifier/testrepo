/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

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
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ajaxlongrun.AjaxJob;
import se.dabox.service.common.ajaxlongrun.AppAjaxLongOp;
import se.dabox.service.common.ajaxlongrun.FutureAjaxJob;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.reportjs")
public class ProjectReportJsonModule extends AbstractJsonAuthModule {

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
