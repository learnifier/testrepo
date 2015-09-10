/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Locale;
import net.unixdeveloper.druwa.RequestCycle;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.crisp.datasource.OrgUnitSource;
import se.dabox.cocobox.crisp.datasource.PdProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProductInfoSource;
import se.dabox.cocobox.crisp.datasource.StandardOrgUnitInfoSource;
import se.dabox.cocobox.crisp.desc.EventMethod;
import se.dabox.cocobox.crisp.method.GetProjectConfiguration;
import se.dabox.cocobox.crisp.response.ProjectConfigResponse;
import se.dabox.cocobox.crisp.response.json.ProjectConfigResponseJson;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocobox.crisp.runtime.DwsCrispExecutionHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class GetCrispProjectProductConfig {
    private final RequestCycle cycle;
    private final long orgId;
    private final String productId;

    GetCrispProjectProductConfig(RequestCycle cycle, long orgId, String productId) {
        this.cycle = cycle;
        this.orgId = orgId;
        this.productId = productId;
    }

    ProjectConfigResponse get() {

        ProductDirectoryClient pdClient = getProductDirectoryClient(cycle);
        Product product = ProductFetchUtil.getExistingProduct(pdClient, productId);

        CrispContext ctx = DwsCrispContextHelper.getCrispContext(cycle, product);

        if (ctx == null) {
            return null;
        }

        EventMethod projectConfigMethod
                = ctx.getDescription().getMethods().getGetProjectConfiguration();

        if (projectConfigMethod == null) {
            return null;
        }

        OrgUnitInfo ou = getOrganization();

        OrgUnitSource ouSource = new StandardOrgUnitInfoSource(ou);
        ProductInfoSource prodSource = new PdProductInfoSource(product);

        GetProjectConfiguration request
                = GetProjectConfiguration.newCreateGetProjectConfiguration(ouSource, prodSource);

        DwsCrispExecutionHelper execHelper = new DwsCrispExecutionHelper(cycle, ctx);
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        ProjectConfigResponse response
                = execHelper.executeRequest(userLocale, request, new ProjectConfigResponseJson());

        return response;
    }

    private OrgUnitInfo getOrganization() {
        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        return odClient.getOrgUnitInfo(orgId);
    }



}
