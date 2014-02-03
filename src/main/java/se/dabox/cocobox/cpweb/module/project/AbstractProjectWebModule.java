/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project;

import java.util.Collections;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.cocosite.project.GetIdProjectProductIdCommand;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public abstract class AbstractProjectWebModule extends AbstractWebAuthModule {

    protected void addCommonMapValues(Map<String, Object> map, OrgProject project, RequestCycle cycle) {
        map.put("prj", project);
        map.put("org", secureGetMiniOrg(cycle, project.getOrgId()));

        OrgProject masterProject = null;
        ProjectParticipation participationOwner = null;

        if (project.getMasterProject() != null) {
            masterProject =
                    getCocoboxCordinatorClient(cycle).getProject(project.getMasterProject());

            checkPermission(cycle, masterProject);

            map.put("masterProject", masterProject);
        }

        if (project.getParticipationOwner() != null) {
            participationOwner =
                    getCocoboxCordinatorClient(cycle).getProjectParticipation(project.
                    getParticipationOwner());

            map.put("participationOwner", participationOwner);
        }

        Product product = getProductFromParticipationProjectState(cycle, participationOwner,
                project, masterProject);

        if (product != null) {

            map.put("product", product);

            MaterialListFactory mlf = new MaterialListFactory(cycle, CocositeUserHelper.
                    getUserLocale(cycle));
            mlf.addProducts(Collections.singletonList(product));

            Material material = mlf.getList().get(0);
            map.put("material", material);
        }
    }

    private Product getProductFromParticipationProjectState(RequestCycle cycle,
            ProjectParticipation participationOwner, OrgProject project, OrgProject masterProject) {

        if (participationOwner == null || masterProject == null) {
            return null;
        }

        ProductId productId = new GetIdProjectProductIdCommand(cycle).forIdProject(project);

        if (productId == null) {
            return null;
        }

        return ProductFetchUtil.getExistingProduct(getProductDirectoryClient(cycle), productId);
    }
}
