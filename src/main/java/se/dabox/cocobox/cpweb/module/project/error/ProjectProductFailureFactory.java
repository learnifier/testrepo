/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.error;

import net.unixdeveloper.druwa.RequestCycle;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.crisp.runtime.CrispErrorException;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProjectProductFailureFactory {
    private final RequestCycle cycle;

    public ProjectProductFailureFactory(RequestCycle cycle) {
        this.cycle = cycle;
    }

    public ProjectProductFailure newFailure(String productId, Exception ex) {
        String productName = "(Unknown)";
            Product product = ProductFetchUtil.getProduct(getProductDirectoryClient(cycle),
                    productId);
            if (product != null) {
                productName = product.getTitle();
            }

            String type = ex.getClass().getSimpleName();
            if (ex instanceof CrispErrorException) {
                type = "Integration";
            }
            String desc = ex.getMessage();


            ProjectProductFailure failure = new ProjectProductFailure(productId, productName,
                    type,
                    desc);

            return failure;
    }
}
