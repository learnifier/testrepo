package se.dabox.cocobox.cpweb.module.project.command;

import se.dabox.service.common.coursedesign.v1.mutable.MutableComponent;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseScene;
import se.dabox.service.common.coursedesign.v1.mutable.MutableResource;

import java.util.HashMap;
import java.util.List;

import static se.dabox.cocobox.cpweb.module.project.command.MaterialIdUtils.getProductIdFromResource;
import static se.dabox.cocobox.cpweb.module.project.command.MaterialIdUtils.getProductIdFromType;
import static se.dabox.cocobox.cpweb.module.project.command.MaterialIdUtils.getResourceFromProductId;
import static se.dabox.cocobox.cpweb.module.project.command.MaterialIdUtils.getTypeFromProductId;

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
    void replaceProducts(HashMap<String, String> replaceHash) {
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

    private static void replaceComponent(MutableComponent component, HashMap<String, String> replaceHash) {
        final String productId = getProductIdFromType(component.getType());
        if(productId != null && replaceHash.containsKey(productId)) {
            component.setType(getTypeFromProductId(replaceHash.get(productId)));
        }
        final List<MutableComponent> children = component.getChildren();
        if(children != null) {
            children.forEach(c -> replaceComponent(c, replaceHash));
        }
    }

    private static void replaceResource(MutableResource resource, HashMap<String, String> replaceHash) {
        final String materialId = resource.getMaterialId();
        final String id = getProductIdFromResource(materialId);
        if(id != null && replaceHash.containsKey(id)) {
            resource.setMaterialId(getResourceFromProductId(replaceHash.get(id)));
        }

        final String constraint = resource.getConstraint();
        final String constraintId = getProductIdFromResource(constraint);
        if(constraintId != null && replaceHash.containsKey(constraintId)) {
            resource.setConstraint(getResourceFromProductId(replaceHash.get(constraintId)));
        }
    }
}
