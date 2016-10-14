package se.dabox.cocobox.cpweb.module.project.command;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
class MaterialIdUtils {
    static String getProductIdFromType(String type) {
        if(type.contains("material|proddir|")) {
            return type.substring("material|proddir|".length());
        } else {
            return null;
        }
    }
    static String getProductIdFromResource(String resource) {
        if(resource.contains("proddir|")) {
            return resource.substring("proddir|".length());
        } else {
            return null;
        }
    }

    static String getTypeFromProductId(String id) {
        return "material|proddir|" + id;
    }

    static String getResourceFromProductId(String id) {
        return "proddir|" + id;
    }
}
