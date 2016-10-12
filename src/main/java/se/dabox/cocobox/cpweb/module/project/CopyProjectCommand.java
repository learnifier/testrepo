package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.NewProjectRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.material.AddProjectProductRequest;
import se.dabox.service.common.ccbc.project.material.AddProjectProductRequestBuilder;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.CreateDesignRequest;
import se.dabox.service.common.coursedesign.CreateDesignResponse;
import se.dabox.service.common.coursedesign.techinfo.CpDesignTechInfo;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.mutable.MutableComponent;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseScene;
import se.dabox.service.common.coursedesign.v1.mutable.MutableResource;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.UpdateProductRequest;
import se.dabox.service.common.proddir.id.AnonymousProductIdFactory;
import se.dabox.service.coursecatalog.client.CocoboxCourseSourceConstants;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSessionId;
import se.dabox.service.coursecatalog.client.session.create.CreateSessionRequest;
import se.dabox.service.coursecatalog.client.session.impl.StandardCourseSessionSource;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class CopyProjectCommand {

    private final RequestCycle cycle;
    CopyProjectCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    RequestTarget execute(Project project, String projName) {

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);

        final ProjectType type = project.getType();
        if(type != ProjectType.DESIGNED_PROJECT) {
            throw new UnsupportedOperationException("Can only copy projects of type designed");
        }
        OrgProject orgProj = (OrgProject)project; // How do i check project type?

        CourseCatalogClient ccClient = CacheClients.getClient(cycle, CourseCatalogClient.class);

        if(projName == null) {
            projName = orgProj.getUserTitle() + " (copy)"; // TODO: Add counter if not available...
        }

        Locale locale = orgProj.getLocale();
        Locale country = orgProj.getCountry();

        TimeZone timezone = orgProj.getTimezone();

        Integer courseIdRaw = null;

        final Integer sessionId = orgProj.getCourseSessionId();
        final CatalogCourseSession oldCourseSession;
        if(sessionId != null) {
            oldCourseSession = CollectionsUtil.singleItemOrNull(getCourseCatalogClient(cycle)
                    .listSessions(new ListCatalogSessionRequestBuilder()
                            .withId(CatalogCourseSessionId.valueOf(sessionId)).build()));
        } else {
            oldCourseSession = null;
        }

        NewProjectRequest npr = NewProjectRequest.newDesignedProject(projName,
                orgProj.getOrgId(),
                locale, caller, country, timezone, null, projName, null, false);

        CocoboxCoordinatorClient ccbc
                = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        OrgProject newProject = ccbc.newProject(caller, npr);

        CourseDesignClient cdc = getCourseDesignClient(cycle);
        final CourseDesign design = cdc.getDesign(orgProj.getDesignId());
        final CourseDesignDefinition decodedDesign = CddCodec.decode(cycle, design.getDesign());
        final MutableCourseDesignDefinition mutableCdd = decodedDesign.toMutable();

        // 0. We have created a project with empty cdd; have not set up CourseSession/Course yet

        // 1. Get list of local products we should copy from old cdd.
        Set<Product> localComponents = getLocalComponents(mutableCdd);

        // 2. Copy them
        HashMap<String, String> replaceHash = copyProjectProducts(caller, localComponents, newProject);

        // 3. Replace them in cdd with generic operation
        replaceProducts(mutableCdd, replaceHash);

        // 4. Create the new design
        final CourseDesignDefinition cdd = mutableCdd.toCourseDesignDefinition();

        String cddXml = CddCodec.encode(cdd);
        CreateDesignRequest cdr = new CreateDesignRequest(caller, null, "", cddXml, "");
        String techInfo = CpDesignTechInfo.createStageTechInfo(project.getProjectId());
        cdr.setTechInfo(techInfo);
        CreateDesignResponse des = cdc.createDesign(cdr);

        final long newDesignId = des.getDesignId();

        // 5. Create session
        UpdateProjectRequestBuilder upr
                = new UpdateProjectRequestBuilder(caller, newProject.getProjectId());

        if(oldCourseSession != null) {
            CreateSessionRequest csr = new CreateSessionRequest(caller, oldCourseSession.getCourseId());
            csr = csr.withName(projName);
            UpdateSessionRequest update
                    = UpdateSessionRequestBuilder.newCreateBuilder(caller).setSource(
                    new StandardCourseSessionSource(CocoboxCourseSourceConstants.PROJECT, Long.toString(
                            newProject.getProjectId()))).
                    createUpdateSessionRequest();

            csr = csr.withUpdate(update);

            CatalogCourseSession session = ccClient.createSession(csr);
            upr.setCourseSessionId(session.getId().getId());
        }

        long databankId = ccbc.createDatabank(0, newProject.getProjectId());

        // 6. Set data bank + design ids.
        upr.setStageDatabank(databankId);
        upr.setDesignId(newDesignId);
        upr.setStageDesignId(newDesignId);
        ccbc.updateOrgProject(upr.createUpdateProjectRequest());

        return null;
    }

    private void replaceComponent(MutableComponent component, HashMap<String, String> replaceHash) {
        final String productId = getProductIdFromType(component.getType());
        if(productId != null && replaceHash.containsKey(productId)) {
            component.setType(getTypeFromProductId(replaceHash.get(productId)));
        }
        final List<MutableComponent> children = component.getChildren();
        if(children != null) {
            children.forEach(c -> replaceComponent(c, replaceHash));
        }
    }

    private void replaceResource(MutableResource resource, HashMap<String, String> replaceHash) {
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

    private void replaceProducts(MutableCourseDesignDefinition mutableCdd, HashMap<String, String> replaceHash) {
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


    // ----- start this will probably be moved to own class

    private void processComponent(MutableComponent component, Set<String> candidates) {
        final String type = component.getType();
        if(type != null) {
            String id = getProductIdFromType(type);
            if(id != null) {
                candidates.add(id);
            }
        }

        final List<MutableComponent> children = component.getChildren();
        if(children != null) {
            children.forEach(c -> processComponent(c, candidates));
        }
    }

    private void processResource(MutableResource resource, Set<String> candidates) {
        final String materialId = resource.getMaterialId();
        final String id = getProductIdFromResource(materialId);
        if(id != null) {
            candidates.add(materialId);
        }
    }

    private String getProductIdFromType(String type) {
        if(type.contains("material|proddir|")) {
            return type.substring("material|proddir|".length());
        } else {
            return null;
        }
    }

    private String getTypeFromProductId(String id) {
        return "material|proddir|" + id;
    }

    private String getProductIdFromResource(String resource) {
        if(resource.contains("proddir|")) {
            return resource.substring("proddir|".length());
        } else {
            return null;
        }
    }

    private String getResourceFromProductId(String id) {
        return "proddir|" + id;
    }

    private Set<Product> getLocalComponents(MutableCourseDesignDefinition mutableCdd) {
        Set<String> candidates = new HashSet<>();
        final List<MutableComponent> components = mutableCdd.getComponents();
        components.forEach(c -> processComponent(c, candidates));
        final List<MutableCourseScene> scenes = mutableCdd.getScenes();
        if (scenes != null) { // Can it be null?
            scenes.forEach(s ->
                    s.getComponents().forEach(c -> processComponent(c, candidates))
            );
        }
        final List<MutableResource> resources = mutableCdd.getResources();
        if (resources != null) {
            resources.forEach(r -> processResource(r, candidates));
        }
        final ProductDirectoryClient pdClient = CacheClients.getClient(cycle, ProductDirectoryClient.class);
        List<Product> ps = pdClient.getProducts(new ArrayList<>(candidates));

        return ps.stream().filter(Product::isProjectProduct).collect(Collectors.toSet());
    }

    private HashMap<String, String> copyProjectProducts(long caller, Set<Product> ps, OrgProject toProject) {
        return ps.stream().collect(HashMap<String, String>::new,
                (m, p) -> {
                    Product copied = p.copy();
                    final ProductId productId = createId();
                    copied.setId(productId);

                    getProductDirectoryClient(cycle).addProduct(caller, copied);
                    getCocoboxCoordinatorClient(cycle).addOrgProduct(caller, toProject.getOrgId(), productId.getId());

                    AddProjectProductRequest addreq
                            = new AddProjectProductRequestBuilder().
                            setProductId(productId.getId()).
                            setProjectId(toProject.getProjectId()).
                            setSkipActivation(true).
                            build();

                    getProjectMaterialCoordinatorClient(cycle).addProjectProduct(addreq);

                    updateProductOwner(caller, copied, toProject.getProjectId()); // Hmm, do we really need to explicitly set owner when we already created a ProjectProduct?

                    m.put(p.getId().getId(), productId.getId());
                },
                (m, u) -> {});
    }

    private void updateProductOwner(long caller, Product product, long projectId) {
        Product p = product.copy();
        p.setOwnerProjectId(projectId);

        UpdateProductRequest upr = new UpdateProductRequest(caller, p, false, false, false);
        getProductDirectoryClient(cycle).updateProduct(upr);
    }



    // ----- end this will probably be moved to own class

    private static CourseCatalogClient getCourseCatalogClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseCatalogClient.class);
    }

    private static CourseDesignClient getCourseDesignClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

    private static CocoboxCoordinatorClient getCocoboxCoordinatorClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    private static ProductDirectoryClient getProductDirectoryClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }

    private static ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
    }

    private ProductId createId() {
        return new AnonymousProductIdFactory().newId();
    }

}
