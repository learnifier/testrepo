/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.credit;

import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.credit.CreditCheckResult;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;
import se.dabox.util.collections.ValueUtils;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class CpCreditCheck {

    private final RequestCycle cycle;
    private CocoboxCordinatorClient cachedCcbc;
    private final long projectId;

    public CpCreditCheck(RequestCycle cycle, OrgUnitInfo org, long projectId) {
        this.projectId = projectId;
        this.cycle = cycle;
    }

    public List<CreditAllocationFailure> getCreditFailures(List<Long> participationIds) {
        CocoboxCordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient();
        final CreditCheckResult ccc
                = cocoboxCordinatorClient.
                participationCreditCheck(projectId, participationIds);

        return CollectionsUtil.transformList(ccc.getMissingMap().entrySet(),
                new Transformer<Map.Entry<String, Long>, CreditAllocationFailure>() {

                    @Override
                    public CreditAllocationFailure transform(
                            Map.Entry<String, Long> item) {
                                final String productId = item.getKey();

                                final long alloced = ValueUtils.coalesce(
                                        ccc.getAllocationMap().get(productId),
                                        0L);

                                final long avail = ValueUtils.coalesce(
                                        ccc.getAvailableMap().get(productId),
                                        0L);

                                return new CreditAllocationFailure(productId,
                                        getProductName(productId),
                                        alloced,
                                        avail);
                            }

                            private String getProductName(String productId) {
                                final ServiceRequestCycle cycle = DruwaService.
                                getCurrentCycle();
                                ProductDirectoryClient pdClient
                                = CacheClients.getClient(cycle,
                                        ProductDirectoryClient.class);

                                Product product = pdClient.getProduct(productId);

                                if (product == null) {
                                    return productId;
                                }

                                return product.getTitle();
                            }
                });
    }

    private CocoboxCordinatorClient getCocoboxCordinatorClient() {

        if (cachedCcbc == null) {
            cachedCcbc = CacheClients.getClient(cycle, CocoboxCordinatorClient.class);
        }

        return cachedCcbc;
    }

}
