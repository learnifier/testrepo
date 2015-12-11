/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.report;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.webutils.freemarker.text.JavaCocoText;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.report")
public class ProjectReportModule extends AbstractProjectWebModule {
    public static final String ACTION_ACTIVITYREPORT = "activityReport";

    @WebAction
    public RequestTarget onActivityReport(RequestCycle cycle, String strProjectId) {
        OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        String reportJsonUrl = cycle.urlFor(ProjectReportJsonModule.class, "activityReport",
                strProjectId);

        JavaCocoText jct = new JavaCocoText();
        String title = jct.get("cpweb.project.report.activity.title");

        map.put("reportJsonUrl", reportJsonUrl);
        map.put("title", title);

        return new FreemarkerRequestTarget("/project/report/activityReport.html", map);
    }

    @WebAction
    public RequestTarget onInnerActivityReport(RequestCycle cycle, String strProjectId, String innerProjectId) {
        OrgProject project = getProject(cycle, strProjectId);
        OrgProject innerProject = getProject(cycle, innerProjectId);

        checkPermission(cycle, project);
        checkPermission(cycle, innerProject);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        String reportJsonUrl = cycle.urlFor(ProjectReportJsonModule.class, "activityReport",
                innerProjectId);

        map.put("reportJsonUrl", reportJsonUrl);
        map.put("title", new GetProjectAdministrativeName(cycle).getName(innerProject));

        return new FreemarkerRequestTarget("/project/report/activityReport.html", map);
    }

    @WebAction
    public RequestTarget onIdProductReport(RequestCycle cycle, String strProjectId, String productId) {
        OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));
		
        checkPermission(cycle, project);

        Product product
                = ProductFetchUtil.getExistingProduct(getProductDirectoryClient(cycle), productId);

        Map<String, Object> map = createMap();

        
        addCommonMapValues(map, project, cycle);

        map.put("product", product);
        map.put("productId", productId);
		
        return new FreemarkerRequestTarget("/project/report/idProductReport.html", map);
    }
    
    @WebAction
    public RequestTarget onSliiChallengeReport(RequestCycle cycle, String strProjectId) {
        OrgProject project = getCocoboxCordinatorClient(cycle).
                getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        Map<String, Object> map = createMap();

        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/report/sliiChallengeReport.html", map);
    }
    
}
