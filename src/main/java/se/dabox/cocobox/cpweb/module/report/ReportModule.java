/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.report;

import java.util.Collections;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.command.GetGrantedCpProductCommand;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;

/**
 *
 * @author borg321
 */
@WebModuleMountpoint("/report")
public class ReportModule extends AbstractWebAuthModule {

    @DefaultWebAction
    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String strOrgId, String reportId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();
        map.put("prj", "");
        map.put("org", org);

        return new FreemarkerRequestTarget("/report/reportOverview.html", map);
    }

    @WebAction
    public RequestTarget onCreditStatus(RequestCycle cycle, String strOrgId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_ACCOUNTBALANCE);

        Map<String, Object> map = createMap();
        map.put("org", org);

        return new FreemarkerRequestTarget("/report/credits/perStatus.html", map);
    }

    @WebAction
    public RequestTarget onCreditsPurchased(RequestCycle cycle, String strOrgId, String strProdId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        Product product = getProductDirectoryClient(cycle).getProduct(strProdId);

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("product", product);

        return new FreemarkerRequestTarget("/report/credits/purchased.html", map);
    }
    @WebAction
    public RequestTarget onCreditsAvailable(RequestCycle cycle, String strOrgId, String strProdId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        Product product = getProductDirectoryClient(cycle).getProduct(strProdId);

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("product", product);

        return new FreemarkerRequestTarget("/report/credits/available.html", map);
    }

    @WebAction
    public RequestTarget onCreditsExpired(RequestCycle cycle, String strOrgId, String strProdId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        Product product = getProductDirectoryClient(cycle).getProduct(strProdId);

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("product", product);

        return new FreemarkerRequestTarget("/report/credits/expired.html", map);
    }

    @WebAction
    public RequestTarget onCreditsUsed(RequestCycle cycle, String strOrgId, String strProdId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        Product product = getProductDirectoryClient(cycle).getProduct(strProdId);

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("product", product);

        return new FreemarkerRequestTarget("/report/credits/used.html", map);
    }

    @WebAction
    public RequestTarget onProjectStatus(RequestCycle cycle, String strOrgId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();
        map.put("org", org);

        return new FreemarkerRequestTarget("/report/projects/perStatus.html", map);
    }

    @WebAction
    public RequestTarget onProductCompletion(RequestCycle cycle, String strOrgId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();
        map.put("org", org);

        return new FreemarkerRequestTarget("/report/products/completionPerUser.html", map);
    }

    @WebAction
    public RequestTarget onSubprojectStatus(RequestCycle cycle, String strOrgId, String strProductId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        ProductId productId = strProductId == null ? null : new ProductId(strProductId);
        Product product = new GetGrantedCpProductCommand(org.getId()).transform(productId);

        MaterialListFactory factory
                = new MaterialListFactory(cycle, CocositeUserHelper.getUserLocale(cycle));
        factory.addProducts(Collections.singletonList(product));

        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("jsonUrl", cycle.urlFor(ReportJsonModule.class, "subprojectStatus", strOrgId, strProductId));
        map.put("material", factory.getList().get(0));

        return new FreemarkerRequestTarget("/report/activity/subprojectActivityStatus.html", map);
    }
}
