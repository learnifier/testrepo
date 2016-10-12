package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.state.NewProjectSessionNg;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.material.AddProjectProductRequest;
import se.dabox.service.common.ccbc.project.material.AddProjectProductRequestBuilder;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequest;
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
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.UpdateProductRequest;
import se.dabox.service.common.proddir.id.AnonymousProductIdFactory;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSessionId;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static se.dabox.cocobox.cpweb.module.project.ProductsHelper.getDesignOrgmats;


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class CopyProjectCommand {

    private final RequestCycle cycle;
    CopyProjectCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    RequestTarget execute(Project project, String newName) {

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);

        final ProjectType type = project.getType();
        if(type != ProjectType.DESIGNED_PROJECT) {
            throw new UnsupportedOperationException("Can only copy projects of type designed");
        }
        OrgProject orgProj = (OrgProject)project; // How do i check project type?

        final Long designId = orgProj.getDesignId();

        CreateProjectSessionProcessorNg processor = new CreateProjectSessionProcessorNg(orgProj.getOrgId());

        String cancelUrl = null;

        final NewProjectSessionNg nps = new NewProjectSessionNg(type.toString(), Collections.emptyList(), Collections.emptyList(), processor,
                cancelUrl,
                designId,
                null);

        Integer courseIdRaw = null;

        final Integer sessionId = orgProj.getCourseSessionId();
        if(sessionId != null) {
            final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(getCourseCatalogClient(cycle)
                    .listSessions(new ListCatalogSessionRequestBuilder()
                            .withId(CatalogCourseSessionId.valueOf(sessionId)).build()));
            CatalogCourseId courseId = session.getCourseId();
            courseIdRaw = courseId.getId();
        }
        nps.setCourseId(courseIdRaw);

        final CreateProjectGeneral input = new CreateProjectGeneral(); // TODO: Remove form object from NewProjectSession
        input.setProjectname(newName==null?"zzz":newName); // TODO: Remove conditional when properly called.
        input.setCountry(orgProj.getCountry());

        input.setDesign(null);


        input.setProjectlang(orgProj.getLocale());
        input.setTimezone(orgProj.getTimezone());
        nps.setCreateProjectGeneral(input);

        nps.storeInSession(cycle.getSession());
        OrgProject newProject = nps.process(cycle, null);

        CourseDesignClient cdc = getCourseDesignClient(cycle);
        final CourseDesign design = cdc.getDesign(orgProj.getDesignId());
        final CourseDesignDefinition decodedDesign = CddCodec.decode(cycle, design.getDesign());
        final MutableCourseDesignDefinition mutableCdd = decodedDesign.toMutable();

        // 0. We have created a project with empty cdd

        // 1. Get list of local products we should copy from old cdd.
        Set<Product> localComponents = getLocalComponents(mutableCdd);

        // 2. Copy them
        HashMap<String, String> replaceHash = copyProjectProducts(caller, localComponents, newProject);

        // 3. Replace them in cdd with generic operation
        replaceProducts(mutableCdd, replaceHash);

        // 4. Create design
        final CourseDesignDefinition cdd = mutableCdd.toCourseDesignDefinition();

        String cddXml = CddCodec.encode(cdd);
        CreateDesignRequest cdr = new CreateDesignRequest(caller, null, "", cddXml, "");
        String techInfo = CpDesignTechInfo.createStageTechInfo(project.getProjectId());
        cdr.setTechInfo(techInfo);
        CreateDesignResponse des = cdc.createDesign(cdr);

        final long newDesignId = des.getDesignId();

        // 5. Set design in project
        newProject.setDesignId(newDesignId);
        UpdateProjectRequestBuilder uprb = new UpdateProjectRequestBuilder(caller, newProject.getProjectId());
        uprb.setDesignId(newDesignId);
        uprb.setStageDesignId(newDesignId);
        final UpdateProjectRequest upr = uprb.createUpdateProjectRequest();
        getCocoboxCoordinatorClient(cycle).updateOrgProject(upr);

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

    private void replaceProducts(MutableCourseDesignDefinition mutableCdd, HashMap<String, String> replaceHash) {
        final List<MutableComponent> components = mutableCdd.getComponents();
        components.forEach(c -> replaceComponent(c, replaceHash));

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

    private Set<Product> getLocalComponents(MutableCourseDesignDefinition mutableCdd) {
        Set<String> candidates = new HashSet<>();
        final List<MutableComponent> components = mutableCdd.getComponents();
        components.forEach(c -> processComponent(c, candidates));

        final ProductDirectoryClient pdClient = CacheClients.getClient(cycle, ProductDirectoryClient.class);
        List<Product> ps = pdClient.getProducts(new ArrayList<>(candidates));

        final Set<Product> projectPs = ps.stream().filter(Product::isProjectProduct).collect(Collectors.toSet());

//        final List<MutableResource> resources = mutableCdd.getResources();
//        resources.forEach(r -> {
//            final String materialId = r.getMaterialId();
//            // TODO: Handle resources
//        });
//
//        final List<MutableCourseScene> scenes = mutableCdd.getScenes();
//        scenes.forEach(s -> s.getComponents()); // Can probably re-use components handling above.

        return projectPs;
    }

    private HashMap<String, String> copyProjectProducts(long caller, Set<Product> ps, OrgProject toProject) {
        Map<String, String> res = new HashMap<>();
        ProjectMaterialCoordinatorClient pmcClient
                = CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);

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

                    pmcClient.addProjectProduct(addreq);

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

    public static CourseCatalogClient getCourseCatalogClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseCatalogClient.class);
    }

    public static CourseDesignClient getCourseDesignClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

    public static CocoboxCoordinatorClient getCocoboxCoordinatorClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    public static ProductDirectoryClient getProductDirectoryClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }

    private ProductId createId() {
        return new AnonymousProductIdFactory().newId();
    }

}
