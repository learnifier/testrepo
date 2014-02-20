/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.command;

import java.util.Collections;
import java.util.List;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class GetProjectCompatibleProducts {

    public List<Product> getValid(OrgProject project, final List<Product> products) {
        ParamUtil.required(project,"project");
        return getValid(project.getType(), products);
    }

    public List<Product> getValid(ProjectType projectType, final List<Product> products) {
        ParamUtil.required(projectType, "projectType");

        return ProjectTypeUtil.call(projectType, new ProjectTypeCallable<List<Product>>() {
            @Override
            public List<Product> callDesignedProject() {
                return products;
            }

            @Override
            public List<Product> callMaterialListProject() {
                return CollectionsUtil.sublist(products, new Predicate<Product>() {
                    @Override
                    public boolean evalute(Product item) {
                        String setting = item.getProductType().getGlobalMetaValue(
                                "cocobox.orgmatproject.material");

                        if (setting == null) {
                            return true;
                        }

                        return Boolean.parseBoolean(setting);
                    }
                });
            }

            @Override
            public List<Product> callSingleProductProject() {
                //TODO: REMOVE THIS WHEN CREATING PROJECTS WORK
                return callMaterialListProject();
            }
        });
    }
}
