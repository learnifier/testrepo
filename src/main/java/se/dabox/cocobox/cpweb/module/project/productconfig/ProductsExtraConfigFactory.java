/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.productconfig;

import java.util.ArrayList;
import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.state.ErrorState;
import se.dabox.cocobox.crisp.response.config.ProjectConfigResponse;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class ProductsExtraConfigFactory {
    private final RequestCycle cycle;
    private final long orgId;

    public ProductsExtraConfigFactory(RequestCycle cycle, long orgId) {
        this.cycle = cycle;
        this.orgId = orgId;
    }

    public List<ExtraProductConfig> getExtraConfigItems(List<Product> products) {
        final List<ExtraProductConfig> configList = new ArrayList<>();

        List<Product> crispProducts
                = CollectionsUtil.sublist(products, p -> ProductUtils.isCrispProduct(p));

        for (Product product : crispProducts) {

            ProjectConfigResponse response = null;
            try {
                response = new GetCrispProjectProductConfig(cycle, orgId,
                        product.getId().getId()).get();
            } catch (CrispException crispException) {
                ErrorState state = new ErrorState(orgId, product, crispException, null);
                throw new RetargetException(NavigationUtil.getIntegrationErrorPage(
                        cycle, state));
            }

            if (response == null || response.isEmpty()) {
                continue;
            }

            configList.add(new ExtraProductConfig(product.getId().getId(), response));
        }

        return configList;
    }

}
