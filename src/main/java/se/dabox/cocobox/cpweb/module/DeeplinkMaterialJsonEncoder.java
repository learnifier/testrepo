/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterialMaterial;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductLink;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.material.ProductMaterial;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class DeeplinkMaterialJsonEncoder {
    private final RequestCycle cycle;
    private final CocoboxCoordinatorClient ccbc;
    private final Map<String, OrgProduct> orgProductMap;
    private final Map<Long, Set<OrgProductLink>> orgProductLinkMap;

    public DeeplinkMaterialJsonEncoder(RequestCycle cycle, long orgId) {
        this.cycle = cycle;
        this.ccbc = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        orgProductMap = createOrgProductMap(orgId);
        orgProductLinkMap = createOrgProductLinkSet();
    }

    ByteArrayOutputStream encode(List<Material> materials) {
        ByteArrayOutputStream baos = new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();

                generator.writeArrayFieldStart("aaData");

                for (Material material : materials) {
                    generator.writeStartObject();

                    generator.writeStringField("materialId", material.getCompositeId());
                    generator.writeStringField("title", material.getTitle());
                    generator.writeStringField("desc", material.getDescription());
                    generator.writeStringField("thumbnail", material.getThumbnail(64));

                    if (material instanceof OrgMaterialMaterial) {
                        OrgMaterialMaterial omm = (OrgMaterialMaterial) material;
                        final OrgMaterial orgMat = omm.getOrgMat();
                        generator.writeNumberField(ACTIVE_LINKS, orgMat.getActiveLinks());
                        generator.writeNumberField(INACTIVE_LINKS, orgMat.getInactiveLinks());
                    } else if (material instanceof ProductMaterial) {
                        ProductMaterial pmat = (ProductMaterial) material;

                        generator.writeNumberField(ACTIVE_LINKS, getActiveLinks(pmat));
                        generator.writeNumberField(INACTIVE_LINKS, getInactiveLinks(pmat));
                    } else {
                        throw new IllegalStateException("Invalid material type: " + material.
                                getClass().getName());
                    }



                    generator.writeEndObject();
                }

                generator.writeEndArray();

                generator.writeEndObject();
            }

            private static final String INACTIVE_LINKS = "inactiveLinks";
            private static final String ACTIVE_LINKS = "activeLinks";

            private int getActiveLinks(ProductMaterial pmat) {
                OrgProduct orgProd = orgProductMap.get(pmat.getProduct().getId().getId());
                if (orgProd == null) {
                    return 0;
                }

                Set<OrgProductLink> links = orgProductLinkMap.get(orgProd.getOrgProductId());

                return CollectionsUtil.countMatching(links, OrgProductLink::isActive);
            }

            private int getInactiveLinks(ProductMaterial pmat) {
                OrgProduct orgProd = orgProductMap.get(pmat.getProduct().getId().getId());
                if (orgProd == null) {
                    return 0;
                }

                Set<OrgProductLink> links = orgProductLinkMap.get(orgProd.getOrgProductId());

                return CollectionsUtil.countMatching(links, link -> !link.isActive());
            }
        }.encodeToStream();

        return baos;
    }

    private Map<String, OrgProduct> createOrgProductMap(long orgId) {
        List<OrgProduct> orgProducts = ccbc.listOrgProducts(orgId);

        return CollectionsUtil.createMap(orgProducts, OrgProduct::getProdId);
    }

    private Map<Long, Set<OrgProductLink>> createOrgProductLinkSet() {
        Set<Long> orgProdIds
                = CollectionsUtil.transform(orgProductMap.values(), OrgProduct::getOrgProductId);

        List<OrgProductLink> links = ccbc.getOrgProductsLinks(orgProdIds);

        return CollectionsUtil.createMapSet(links, OrgProductLink::getOrgProductId);
    }

}
