/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocobox.cpweb.module.project.details.ExtendedComponentFieldName;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.ccbc.project.ProjectProductTransformers;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.coursedesign.ComponentFieldName;
import se.dabox.service.common.coursedesign.ComponentUtil;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ProductDurationDefaultValuePopulator {

    private static final String DURATION = "duration";
    private final RequestCycle cycle;
    private final OrgProject project;

    ProductDurationDefaultValuePopulator(RequestCycle cycle, OrgProject project) {
        this.cycle = cycle;
        this.project = project;
    }

    void populate(Map<String, String> defaultValueMap,
            Map<String, Set<ExtendedComponentFieldName>> fieldMapSet,
            List<Component> componenents) {

        List<Component> allComponents = ComponentUtil.getFlatList(componenents);

        Map<String, String> productDurationMap = getProductDurationMap();
        Map<String,Component> componentMap = getComponentMap(allComponents);

        for (Map.Entry<String, Set<ExtendedComponentFieldName>> entry : fieldMapSet.entrySet()) {
            final String name = entry.getKey() + '_' + DURATION;

            if(defaultValueMap.containsKey(name)) {
                continue;
            }

            ComponentFieldName durationField = getDurationField(entry.getValue());
            if (durationField == null) {
                continue;
            }

            Component comp = componentMap.get(entry.getKey());
            ProductId productId = null;
            if (comp != null) {
                productId = ComponentUtil.getProductId(comp);
            }

            if (productId == null) {
                continue;
            }

            String productDefaultDuration = productDurationMap.get(productId.getId());
            if (productDefaultDuration != null) {
                
                defaultValueMap.put(name, productDefaultDuration);
            }
        }

    }

    private Map<String, String> getProductDurationMap() {
        List<ProjectProduct> projProds =
                getProjectMaterialCoordinatorClient().getProjectProducts(project.getProjectId());

        List<String> productIds =
                CollectionsUtil.transformList(projProds, ProjectProductTransformers.
                getProductIdStrTransformer());

        List<Product> products = getProductDirectoryClient().getProducts(productIds);
        
        Map<String, String> durationMap = new HashMap<>();

        for (Product prod : products) {

            String duration = StringUtils.trimToNull(prod.getFieldSingleValue(DURATION));

            if (duration != null) {
                durationMap.put(prod.getId().getId(), duration);
            }
        }

        return durationMap;
    }

    private CocoboxCoordinatorClient getCocoboxCordinatorClient() {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    private ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient() {
        return CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
    }

    private ProductDirectoryClient getProductDirectoryClient() {
        return CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }

    private ComponentFieldName getDurationField(Set<? extends ComponentFieldName> fields) {
        for (ComponentFieldName componentFieldName : fields) {
            if (componentFieldName.getName().equals(DURATION)) {
                return componentFieldName;
            }
        }

        return null;
    }

    private Map<String, Component> getComponentMap(List<Component> components) {
        return CollectionsUtil.createMap(components, new Transformer<Component, String>() {

            @Override
            public String transform(Component item) {
                return item.getCid().toString();
            }
        });
    }

    private String getProductId(Component component) {
        if (component == null) {
            return null;
        }

        final String subtype = component.getSubtype();

        if (subtype == null || !subtype.startsWith("proddir|")) {
            return null;
        }

        return subtype.split("\\|")[1];
    }
}
