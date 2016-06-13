/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.command;

import java.util.List;
import javax.annotation.Nonnull;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocosite.messagepage.GenericMessagePageFactory;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 * Gets a Product from the product directory and verifies that it is granted for the client.
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class GetGrantedCpProductCommand implements Transformer<ProductId, Product> {
    private final long orgId;

    private Transformer<ProductId,Product> productFetcher;
    private Transformer<Long,List<OrgProduct>> orgProductsFetcher;

    public GetGrantedCpProductCommand(long orgId) {
        this.orgId = orgId;
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        ProductDirectoryClient pdClient
                = CacheClients.getClient(cycle, ProductDirectoryClient.class);

        CocoboxCoordinatorClient ccbc
                = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        productFetcher = (id) -> ProductFetchUtil.getProduct(pdClient, id);
        orgProductsFetcher = ccbc::listOrgProducts;
    }

    public void setProductFetcher(Transformer<ProductId, Product> productFetcher) {
        this.productFetcher = productFetcher;
    }

    public void setOrgProductsFetcher(Transformer<Long, List<OrgProduct>> orgProductsFetcher) {
        this.orgProductsFetcher = orgProductsFetcher;
    }

    /**
     * Gets a product with the specified id. If the product is not granted as an orgproduct
     * or the product doesn't exist a RetargetException will be thrown.
     *
     * @param productId A ProductId
     * @return A product
     * @throws RetargetException Thrown with a target to a generic message page with not found
     *  information.
     */
    @Override
    public @Nonnull Product transform(ProductId productId) throws RetargetException {
        if (productId == null) {
            String text = String.format("Product not specified: (null)");
            RequestTarget page
                    = GenericMessagePageFactory.newNotFoundPage().withMessageText(text).build();
            throw new RetargetException(page);
        }

        List<OrgProduct> orgProducts = orgProductsFetcher.transform(orgId);

        final String strProductId = productId.getId();
        int matches = CollectionsUtil.countMatching(orgProducts, (op) -> op.getProdId().equals(
                strProductId));

        if (matches == 0) {
            String text = String.format("Product not found in organization: %s", productId);
            RequestTarget page
                    = GenericMessagePageFactory.newNotFoundPage().withMessageText(text).build();
            throw new RetargetException(page);
        }

        Product product = productFetcher.transform(productId);
        if (product == null) {
            String text = String.format("Product not found: %s", productId);
            RequestTarget page
                    = GenericMessagePageFactory.newNotFoundPage().withMessageText(text).build();
            throw new RetargetException(page);
        }

        return product;
    }

}
