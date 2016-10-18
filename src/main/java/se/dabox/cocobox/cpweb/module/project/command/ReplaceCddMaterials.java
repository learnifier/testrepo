package se.dabox.cocobox.cpweb.module.project.command;

import se.dabox.service.common.coursedesign.ComponentUtil;
import se.dabox.service.common.coursedesign.ResourceUtil;
import se.dabox.service.common.coursedesign.v1.mutable.MutableComponent;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseScene;
import se.dabox.service.common.coursedesign.v1.mutable.MutableResource;
import se.dabox.service.proddir.data.ProductId;

import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
class ReplaceCddMaterials {

    private MutableCourseDesignDefinition mutableCdd;

    public ReplaceCddMaterials(MutableCourseDesignDefinition mutableCdd) {
        this.mutableCdd = mutableCdd;
    }

    /**
     * Replaces material ids in a course design with supplied ids.
     *
     * This replaces material in direct components, resources and any components found in scenes. It also
     * updates resource constraints if they refer to a material that is being replaced.
     *
     * @param replaceHash Hash from old material ID -> new material ID.
     */
    void replaceProducts(HashMap<ProductId, ProductId> replaceHash) {
        final List<MutableComponent> components = mutableCdd.getComponents();
        if(components != null) {
            components.forEach(c -> replaceComponent(c, replaceHash));
        }

        final List<MutableResource> resources = mutableCdd.getResources();
        if(resources != null) {
            resources.forEach(r -> replaceResource(r, replaceHash));
        }

        final List<MutableCourseScene> scenes = mutableCdd.getScenes();
        if(scenes != null) {
            scenes.forEach(s -> {
                final List<MutableComponent> sceneComponents = s.getComponents();
                if(sceneComponents != null) {
                    sceneComponents.forEach(c -> replaceComponent(c, replaceHash));
                }
            });
        }
    }

    private static void replaceComponent(MutableComponent component, HashMap<ProductId, ProductId> replaceHash) {
        final ProductId productId = ComponentUtil.getProductId(component);
        if(productId != null && replaceHash.containsKey(productId)) {
            component.setType(ComponentUtil.getTypeFromProductId(replaceHash.get(productId)));
        }
        final List<MutableComponent> children = component.getChildren();
        if(children != null) {
            children.forEach(c -> replaceComponent(c, replaceHash));
        }
    }

    private static void replaceResource(MutableResource resource, HashMap<ProductId, ProductId> replaceHash) {
        final ProductId id = ResourceUtil.getProductId(resource);
        if(id != null && replaceHash.containsKey(id)) {
            resource.setMaterialId(ResourceUtil.getResourceMaterialIdFromProductId(replaceHash.get(id)));
        }

        final ProductId constraintId = ResourceUtil.getConstraintProductId(resource);
        if(constraintId != null && replaceHash.containsKey(constraintId)) {
            resource.setConstraint(ResourceUtil.getResourceMaterialIdFromProductId(replaceHash.get(constraintId)));
        }
    }
}
