/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.report;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.proddir.data.Product;

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
}
