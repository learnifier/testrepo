/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.cpweb.module.project.report.IdProjectStatusReport;
import se.dabox.cocobox.cpweb.module.project.report.ProductReportBuilder;
import se.dabox.cocobox.cpweb.module.project.report.ProjectReportModule;
import se.dabox.service.common.ajaxlongrun.StatusCallable;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.DwsConstants;
import se.dabox.service.common.ajaxlongrun.AjaxJob;
import se.dabox.service.common.ajaxlongrun.AppAjaxLongOp;
import se.dabox.service.common.ajaxlongrun.FutureAjaxJob;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.project.ProjectSubtypeConstants;
import se.dabox.service.common.ccbc.report.ParticipationReport;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/reporting.js")
public class ReportJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onListAvailableReport(RequestCycle cycle, String strOrgId, String productId) {
        long accountId = toAccountId(cycle, strOrgId, productId);

        final List<Map<String, Object>> reportLines = new AvailableReport(cycle).generateReport(
                accountId);

        final List<Map<String, Object>> enhancedReportLines = new LineDateEnhancer(cycle).enhance(
                reportLines);

        return jsonTarget(jsonify(enhancedReportLines));
    }

    @WebAction
    public RequestTarget onListExpiredReport(RequestCycle cycle, String strOrgId, String productId) {
        long accountId = toAccountId(cycle, strOrgId, productId);

        final List<Map<String, Object>> reportLines = new ExpiredReport(cycle).generateReport(
                accountId);

        final List<Map<String, Object>> enhancedReportLines = new LineDateEnhancer(cycle).enhance(
                reportLines);

        return jsonTarget(jsonify(enhancedReportLines));
    }

    @WebAction
    public RequestTarget onListReservedReport(RequestCycle cycle, String strOrgId, String productId) {
        long accountId = toAccountId(cycle, strOrgId, productId);

        final List<Map<String, Object>> reportLines = new ReservedReport(cycle).generateReport(
                accountId);

        final List<Map<String, Object>> enhancedReportLines = new LineDateEnhancer(cycle).enhance(
                reportLines);

        return jsonTarget(jsonify(enhancedReportLines));
    }

    @WebAction
    public RequestTarget onListUsedReport(RequestCycle cycle, String strOrgId, String productId) {
        long accountId = toAccountId(cycle, strOrgId, productId);

        final List<Map<String, Object>> reportLines = new UsedReport(cycle).
                generateReport(accountId);

        final List<Map<String, Object>> enhancedReportLines = new UsedReportLineEnhancer(cycle).
                enhance(reportLines);

        return jsonTarget(jsonify(enhancedReportLines));
    }

    @WebAction
    public RequestTarget onListPurchasedReport(RequestCycle cycle, String strOrgId, String productId) {
        long accountId = toAccountId(cycle, strOrgId, productId);

        final List<Map<String, Object>> reportLines = new PurchasedReport(cycle).generateReport(
                accountId);

        final List<Map<String, Object>> enhancedReportLines = new LineDateEnhancer(cycle).enhance(
                reportLines);

        return jsonTarget(jsonify(enhancedReportLines));
    }

    @WebAction
    public RequestTarget onListParticipationStatus(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);

        final long orgId = Long.parseLong(strOrgId);

        List<ParticipationReport> reports = getCocoboxCordinatorClient(cycle).
                getParticipationReports(orgId);

        reports = CollectionsUtil.sublist(reports, new Predicate<ParticipationReport>() {

            @Override
            public boolean evalute(ParticipationReport item) {
                return item.getSubtype() == null || item.getSubtype().equals(
                        ProjectSubtypeConstants.MAIN);
            }
        });

        return jsonTarget(new DataTablesJson<ParticipationReport>() {
            @Override
            protected void encodeItem(ParticipationReport item) throws IOException {
                generator.writeNumberField("projectId", item.getProjectId());
                generator.writeStringField("projectName", item.getProjectName());
                writeNumberField("totalCount", item.getTotalCount());
                writeNumberField("invitedCount", item.getInvitedCount());
                writeNumberField("completedCount", item.getCompletedCount());
            }
        }.encode(reports));
    }

    @WebAction
    public RequestTarget onProductReport(final RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);

        final long orgId = Long.parseLong(strOrgId);

        final Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new AppAjaxLongOp<byte[]>() {

            @Override
            protected AjaxJob<byte[]> createJob() {
                StatusCallable<byte[]> task
                        = ProductReportBuilder.getReportForOrgUnitTask(cycle, orgId, userLocale);

                Future<byte[]> future = cycle.getApplication().getScheduler().submit(task);

                return new FutureAjaxJob<>(future, task);
            }

            @Override
            protected RequestTarget completeResponse(RequestCycle cycle, AjaxJob<byte[]> job) {
                byte[] data = job.getResult();
                return jsonTarget(data);
            }

        }.process(cycle);
    }

    @WebAction
    public RequestTarget onIdProductReport(RequestCycle cycle, String strOrgId, String productId) {
        checkOrgPermission(cycle, strOrgId);

        long orgId = Long.parseLong(strOrgId);

        Product product
                = ProductFetchUtil.getExistingProduct(getProductDirectoryClient(cycle), productId);

        String linkPrefix = cycle.urlFor(ProjectReportModule.class,
                ProjectReportModule.ACTION_ACTIVITYREPORT);

        FutureTask<byte[]> task
                = IdProjectStatusReport.getReportForOrgUnitTask(cycle, product, orgId, linkPrefix);

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

    private long toAccountId(RequestCycle cycle, String strOrgId, String productId) {
        checkOrgPermission(cycle, strOrgId);

        List<OrgProduct> products = getCocoboxCordinatorClient(cycle).listOrgProducts(Long.
                parseLong(strOrgId));

        for (OrgProduct orgProduct : products) {
            if (productId.equals(orgProduct.getProdId())) {
                return orgProduct.getTokenManagerAccountId();
            }
        }

        String msg = String.format("Failed to find tokenmanager account for %s/%s", strOrgId,
                productId);

        throw new IllegalStateException(msg);
    }

    private byte[] jsonify(final List<Map<String, Object>> reportLines) {
        Map<String, List<Map<String, Object>>> map = Collections.singletonMap("aaData", reportLines);

        return JsonUtils.encode(map).getBytes(DwsConstants.UTF8_CHARSET);
    }
}
