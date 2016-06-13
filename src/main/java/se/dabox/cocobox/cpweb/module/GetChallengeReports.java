/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocobox.cpweb.module.report.ReportEntry;
import se.dabox.cocobox.cpweb.module.report.ReportModule;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.ClientFactoryException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.proddir.CocoboxProductTypeConstants;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductTypeUtil;
import se.dabox.service.common.proddir.material.ProductMaterialConverter;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
class GetChallengeReports {

    private final RequestCycle cycle;
    private final ProductMaterialConverter converter;
    private String strOrgId;

    public GetChallengeReports(RequestCycle cycle) {
        this.cycle = cycle;
        ProductDirectoryClient pdClient = CacheClients.
                getClient(cycle, ProductDirectoryClient.class);
        converter = new ProductMaterialConverter(cycle, pdClient);
    }

    List<ReportEntry> forOrgUnit(long orgId) {
        this.strOrgId = Long.toString(orgId);
        List<Product> products = getChallengeProducts(orgId);

        return CollectionsUtil.transformList(products, this::toReportEntry);
    }

    private ReportEntry toReportEntry(Product product) {
        String name = String.format("%s status report", converter.convert(product).getTitle());
        String url = cycle.urlFor(ReportModule.class, "subprojectStatus", strOrgId, product.getId().
                getId());

        return new ReportEntry(name, url);
    }

    private List<Product> getChallengeProducts(long orgId) throws ClientFactoryException {
        List<Product> allProducts
                = getProducts(orgId);

        List<Product> products
                = CollectionsUtil.sublist(allProducts, p -> p.getProductType().isInstance(
                        CocoboxProductTypeConstants.CHALLENGE));

        return products;
    }

    private List<Product> getProducts(long orgId) throws ClientFactoryException {
        CocoboxCoordinatorClient ccbc
                = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
        List<OrgProduct> orgProds = ccbc.listOrgProducts(orgId);
        List<String> productIds = CollectionsUtil.transformList(orgProds, OrgProduct::getProdId);
        ProductDirectoryClient pdClient = CacheClients.
                getClient(cycle, ProductDirectoryClient.class);
        List<Product> allProducts = pdClient.getProducts(productIds);
        ProductTypeUtil.setTypes(pdClient, allProducts);
        return allProducts;
    }

}
