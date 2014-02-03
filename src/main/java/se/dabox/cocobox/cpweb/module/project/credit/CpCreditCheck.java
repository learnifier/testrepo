/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.credit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.project.ParticipationToken;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.ccbc.project.ProjectProductTransformers;
import se.dabox.service.common.ccbc.project.credit.AbstractCreditCheck;
import se.dabox.service.common.ccbc.project.credit.CreditCheckResult;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.tokenmanager.client.AccountBalance;
import se.dabox.service.tokenmanager.client.TokenManagerClient;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CpCreditCheck extends AbstractCreditCheck {

    private final RequestCycle cycle;
    private CocoboxCordinatorClient cachedCcbc;
    private ProjectMaterialCoordinatorClient cachedPmc;
    private TokenManagerClient cachedTokenManager;
    private final OrgUnitInfo org;
    private final Map<String, Long> tokenManagerMap = new HashMap<String, Long>();

    public CpCreditCheck(RequestCycle cycle, OrgUnitInfo org, long projectId) {
        super(org.getId(), projectId);
        this.org = org;
        this.cycle = cycle;
    }

    public List<CreditAllocationFailure> getCreditFailures(List<Long> participationIds) {
        final CreditCheckResult results = check(participationIds);

        if (results.getMissingMap().isEmpty()) {
            return Collections.emptyList();
        }

        List<Material> materials = getMaterials(results.getMissingMap().keySet());

        return CollectionsUtil.transformList(materials,
                new Transformer<Material, CreditAllocationFailure>() {
                    @Override
                    public CreditAllocationFailure transform(Material item) {
                        String prodId = item.getId();
                        Long avail = results.getAvailableMap().get(prodId);
                        if (avail == null) {
                            avail = 0L;
                        }
                        CreditAllocationFailure caf = new CreditAllocationFailure(prodId,
                                item.getTitle(),
                                results.getAllocationMap().get(prodId),
                                avail);

                        return caf;
                    }
                });
    }

    @Override
    protected Set<String> getRequiredAllocationProductIds() {
        ProjectMaterialCoordinatorClient pmcClient =
                getProjectMaterialCoordinatorClient();

        List<ProjectProduct> products = pmcClient.getProjectProducts(getProjectId());

        return CollectionsUtil.transform(products, ProjectProductTransformers.
                getProductIdStrTransformer());
    }

    @Override
    protected Set<String> getExistingProductIdAllocations(Long partId) {
        List<ParticipationToken> tokens = getCocoboxCordinatorClient().
                getParticipationTokens(partId);

        return CollectionsUtil.transform(tokens, new Transformer<ParticipationToken, String>() {
            @Override
            public String transform(ParticipationToken item) {
                tokenManagerMap.put(item.getProductId(), item.getTokenManagerAccount());
                return item.getProductId();
            }
        });
    }

    private CocoboxCordinatorClient getCocoboxCordinatorClient() {

        if (cachedCcbc == null) {
            cachedCcbc = CacheClients.getClient(cycle, CocoboxCordinatorClient.class);
        }

        return cachedCcbc;
    }

    @Override
    protected Map<String, Long> getProductBalances(Set<String> keySet) {
        Map<String, Long> map = MapUtil.createHash(keySet);

        TokenManagerClient tmClient = getTokenManagerClient();

        populateTokenManagerMap();
        for (String productId : keySet) {
            Long accountId = tokenManagerMap.get(productId);

            //Missing but requested product
            if (accountId == null) {
                continue;
            }

            AccountBalance balance = tmClient.getAccountBalance(accountId);

            map.put(productId, balance.getAvailable());
        }

        return map;
    }

    private TokenManagerClient getTokenManagerClient() {
        if (cachedTokenManager == null) {
            cachedTokenManager = CacheClients.getClient(cycle, TokenManagerClient.class);
        }

        return cachedTokenManager;
    }

    private void populateTokenManagerMap() {
        List<OrgProduct> prods = getCocoboxCordinatorClient().listOrgProducts(getOrgId());

        for (OrgProduct orgProduct : prods) {
            tokenManagerMap.put(orgProduct.getProdId(), orgProduct.getTokenManagerAccountId());
        }
    }

    private List<Material> getMaterials(Set<String> productIds) {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        MaterialListFactory mlf = new MaterialListFactory(cycle, userLocale);

        List<Product> products =
                CacheClients.getClient(cycle, ProductDirectoryClient.class).getProducts(productIds);

        mlf.addProducts(products);

        return mlf.getList();
    }

    private ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient() {
        if (cachedPmc == null) {
            cachedPmc = CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
        }

        return cachedPmc;
    }
}
