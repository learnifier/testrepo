/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getProductDirectoryClient;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.project.publish.IsProjectPublishingCommand;
import se.dabox.cocobox.cpweb.module.util.CpwebParameterUtil;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.project.ProjectPermissionCheck;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.common.ccbc.project.GetIdProjectProductIdCommand;
import se.dabox.cocosite.project.UpdateRecentProjectList;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectSubtypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.publish.PublishTaskTypeFactory;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.common.scheduler.SchedulerServiceClient;
import se.dabox.service.common.scheduler.TaskInfo;
import se.dabox.service.common.scheduler.filter.TaskFilterBuilder;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.collections.CollectionsUtil;

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
            map.put("isSubproject", true);
        } else {
            map.put("isSubproject", false);
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

        if(WebFeatures.getFeatures(cycle).hasFeature(CocositeWebFeatureConstants.ALT_COURSE_CATALOG)) {
            final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
            final CatalogCourseSession courseSession = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withSourceId(Long.toString(project.getProjectId())).build()));
            if(courseSession != null) {
                map.put("courseSession", courseSession);
                if (courseSession.getCourseId() != null) {
                    final CatalogCourse course = CollectionsUtil.singleItemOrNull(ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseSession.getCourseId()).build()));
                    if (course != null) {
                        map.put("course", course);
                    }
                }
            }
        }

        map.put("projectThumbnail", new LazyProjectThumbnail(cycle, project));

        map.put("canDeleteProject", isDeleteProjectPossible(project));

        map.put("isDesignDetailsAvailable", isDesignDetailsAvailable(project));

        map.put("projectName", new GetProjectAdministrativeName(cycle).getName(project));
        map.put("isPublishing", isPublishing(project));

        map.put("isPublishing", isPublishing(project));

    }

    private Product getProductFromParticipationProjectState(RequestCycle cycle,
            ProjectParticipation participationOwner, OrgProject project, OrgProject masterProject) {

        if (project.getProductId() != null) {
            return ProductFetchUtil.getProduct(getProductDirectoryClient(cycle), project.
                    getProductId());
        }

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

            @Override
            public Boolean callLinkedSubproject() {
                return false;
            }
        });
    }

    private boolean isDesignDetailsAvailable(OrgProject project) {
        return ProjectTypeUtil.call(project, new ProjectTypeCallable<Boolean>() {
            @Override
            public Boolean callMaterialListProject() {
                return false;
            }

            @Override
            public Boolean callDesignedProject() {
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

                    @Override
                    public Boolean callLinkedSubproject() {
                        return false;
                    }
                });
            }

            @Override
            public Boolean callSingleProductProject() {
                return false;
            }
        });
    }

    protected boolean isMoveEnabled(RequestCycle cycle, OrgProject project) {
        ProjectPermissionCheck ppc = ProjectPermissionCheck.fromCycle(cycle);
        if (!ppc.checkPermission(project, CocoboxPermissions.CP_MOVE_PARTICIPANT)) {
            return false;
        }
        return ProjectTypeUtil.callSubtype(project,
                new ProjectSubtypeCallable<Boolean>() {
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

            @Override
            public Boolean callLinkedSubproject() {
                return false;
            }
        });
    }

    protected boolean isPublishing(OrgProject project) {
        return new IsProjectPublishingCommand().isPublishing(project);
    }
}
