/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Collections;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.util.CpwebParameterUtil;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.cocosite.project.GetIdProjectProductIdCommand;
import se.dabox.cocosite.project.UpdateRecentProjectList;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectSubtypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public abstract class AbstractProjectWebModule extends AbstractWebAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProjectWebModule.class);

    protected void addCommonMapValues(Map<String, Object> map, OrgProject project,
            RequestCycle cycle) {
        map.put("prj", project);
        map.put("org", secureGetMiniOrg(cycle, project.getOrgId()));

        OrgProject masterProject = null;
        ProjectParticipation participationOwner = null;

        if (project.getMasterProject() != null) {
            masterProject = getCocoboxCordinatorClient(cycle).getProject(project.getMasterProject());

            checkPermission(cycle, masterProject);

            map.put("masterProject", masterProject);
        }

        if (project.getParticipationOwner() != null) {
            participationOwner = getCocoboxCordinatorClient(cycle).getProjectParticipation(project.
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

        map.put("projectThumbnail", new LazyProjectThumbnail(cycle, project));

        map.put("canDeleteProject", isDeleteProjectPossible(project));
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

    protected OrgProject getProject(RequestCycle cycle, String strProjectId) throws NumberFormatException {
        final CocoboxCoordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient(cycle);
        Long projectId = CpwebParameterUtil.stringToLong(strProjectId);
        OrgProject project = null;
        if (projectId != null) {
            project = cocoboxCordinatorClient.getProject(projectId);
        }
        if (project == null) {
            LOGGER.info("Project {} missing. Redirecting to cpweb main page",
                    strProjectId);
            throw new RetargetException(NavigationUtil.toMain(cycle));
        }
        if (WebFeatures.getFeatures(cycle).hasFeature(CocositeWebFeatureConstants.FLIRT)) {
            if (project.getFlirtId() == null || project.getNewsFlirtId() == null) {
                LOGGER.debug("Flirt ids are missing from project {}. Resyncing project",
                        strProjectId);
                try {
                    cocoboxCordinatorClient.syncProjectState(project.getProjectId());
                    project = cocoboxCordinatorClient.getProject(project.getProjectId());
                } catch (NotFoundException nfe) {
                    LOGGER.info("Project {} missing (stage 2). Redirecting to cpweb main page",
                            strProjectId);
                    throw new RetargetException(NavigationUtil.toMain(cycle));
                }
            }
        }
        new UpdateRecentProjectList(cycle).addProject(project.getProjectId());
        return project;
    }

    private Boolean isDeleteProjectPossible(OrgProject project) {
        return ProjectTypeUtil.callSubtype(project, new ProjectSubtypeCallable<Boolean>() {

            @Override
            public Boolean callMainProject() {
                return true;
            }

            @Override
            public Boolean callIdProjectProject() {
                return false;
            }

            @Override
            public Boolean callChallengeProject() {
                return false;
            }
        });
    }
}
