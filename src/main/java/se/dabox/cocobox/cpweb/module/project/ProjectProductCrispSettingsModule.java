/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import se.dabox.cocobox.cpweb.module.project.productconfig.GetCrispProjectProductConfig;
import se.dabox.cocobox.cpweb.module.project.productconfig.ExtraProductConfig;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.StringRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocobox.crisp.datasource.OrgUnitSource;
import se.dabox.cocobox.crisp.datasource.PdProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProductInfoSource;
import se.dabox.cocobox.crisp.datasource.ProjectInfoSource;
import se.dabox.cocobox.crisp.datasource.StandardOrgUnitInfoSource;
import se.dabox.cocobox.crisp.method.SetProjectConfiguration;
import se.dabox.cocobox.crisp.response.config.ProjectConfigResponse;
import se.dabox.cocobox.crisp.runtime.CrispContext;
import se.dabox.cocobox.crisp.runtime.CrispErrorException;
import se.dabox.cocobox.crisp.runtime.DwsCrispContextHelper;
import se.dabox.cocobox.crisp.runtime.DwsCrispExecutionHelper;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.crisp.OrgProjectInfoSource;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.crisp.GetCrispProjectProductCollaborationId;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.prodcrisp")
public class ProjectProductCrispSettingsModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectProductCrispSettingsModule.class);

    @WebAction
    public RequestTarget onSetSetting(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);

        checkPermission(cycle, prj);
        checkProjectPermission(cycle, prj, CocoboxPermissions.CP_EDIT_PROJECT_MATERIAL);

        String fieldId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        String productId = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "pk");
        String value = cycle.getRequest().getParameter("value");

        ProductDirectoryClient pdClient = getProductDirectoryClient(cycle);
        Product product = ProductFetchUtil.getExistingProduct(pdClient, new ProductId(productId));

        OrgUnitSource orgUnitSource = getOrgUnitSource(cycle, prj);
        ProductInfoSource productSource = new PdProductInfoSource(product);
        ProjectInfoSource projectSource = new OrgProjectInfoSource(prj);
        String projectCollabId = new GetCrispProjectProductCollaborationId(cycle).
                getCollaborationId(prjId, product.getId());

        SetProjectConfiguration setCfg = SetProjectConfiguration.updateSingle(orgUnitSource,
                productSource, projectSource, projectCollabId, fieldId, value);

        CrispContext ctx = DwsCrispContextHelper.getCrispContext(cycle, product);
        DwsCrispExecutionHelper execHelper = new DwsCrispExecutionHelper(cycle, ctx);
        
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        try {
            Map<String, ?> resp = execHelper.executeJson(userLocale, setCfg);
        } catch (CrispErrorException cex) {
            
            if (cex.getError().getErrorType().equals("INVALID_SETTING")) {
                cycle.getResponse().setStatus(400);
                return new StringRequestTarget(cex.getError().getUserMessage());
            }
            
            throw cex;
        }

        //Time to figure out a good value to return here (should be the "visual" form).

        ProjectConfigResponse cfg = new GetCrispProjectProductConfig(cycle, prj, productId).get();

        ProductsValueSource source = new ProductsValueSource(userLocale,
                Collections.singletonList(new ExtraProductConfig(productId, cfg)));

        ProductsValueSource.ProductValue outputValue = source.getValue(productId, fieldId);

        return new StringRequestTarget(outputValue.getName());
    }

    private OrgUnitSource getOrgUnitSource(RequestCycle cycle, OrgProject prj) {
        OrganizationDirectoryClient odClient
                = CacheClients.getClient(cycle, OrganizationDirectoryClient.class);

        OrgUnitInfo org = odClient.getOrgUnitInfo(prj.getOrgId());

        return new StandardOrgUnitInfoSource(org);
    }

}
