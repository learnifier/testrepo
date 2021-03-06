package se.dabox.cocobox.cpweb.module.project.command;

import net.unixdeveloper.druwa.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.MissingProjectProductException;
import se.dabox.service.common.ccbc.project.NewProjectRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.material.AddProjectProductRequest;
import se.dabox.service.common.ccbc.project.material.AddProjectProductRequestBuilder;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.common.coursedesign.ComponentUtil;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.CreateDesignRequest;
import se.dabox.service.common.coursedesign.CreateDesignResponse;
import se.dabox.service.common.coursedesign.ResourceUtil;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class CopyProjectCommand extends AbstractCopyCommand {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CopyProjectCommand.class);

    public CopyProjectCommand(RequestCycle cycle) {
        super(cycle);
    }

    public Long execute(Project project) {

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);

        final ProjectType type = project.getType();
        if(type != ProjectType.DESIGNED_PROJECT) {
            throw new UnsupportedOperationException("Can only copy projects of type designed");
        }
        OrgProject orgProj = (OrgProject)project; // How do i check project type?

        CourseCatalogClient ccClient = CacheClients.getClient(cycle, CourseCatalogClient.class);

        String oldProjName = orgProj.getName();

        // 1. Create project w/o cdd, databank and course session.

        Locale locale = orgProj.getLocale();
        Locale country = orgProj.getCountry();

        TimeZone timezone = orgProj.getTimezone();

        final Integer sessionId = orgProj.getCourseSessionId();
        final CatalogCourseSession oldCourseSession;
        if(sessionId != null) {
            oldCourseSession = CollectionsUtil.singleItemOrNull(getCourseCatalogClient(cycle)
                    .listSessions(new ListCatalogSessionRequestBuilder()
                            .withId(CatalogCourseSessionId.valueOf(sessionId)).build()));
        } else {
            oldCourseSession = null;
        }

        final CocoboxCoordinatorClient ccbc = getCocoboxCoordinatorClient(cycle);

        // Try creating "oldname (copy 1)" ... "oldname (copy N)", "oldname randomUUID"
        final Stream<String> names = Stream.concat(IntStream.range(1, 6).mapToObj(i -> oldProjName + " (copy " + i + ")"), Stream.of(oldProjName + " " + UUID.randomUUID().toString()));

        final OrgProject newProject = names
                .map (name -> {
                    try {
                        final NewProjectRequest npr = NewProjectRequest.newDesignedProject(name,
                                orgProj.getOrgId(),
                                locale, caller, country, timezone, null, name, null, false);
                        return ccbc.newProject(caller, npr);
                    } catch (AlreadyExistsException ex) {
                        return null;
                    }
                }).filter(p -> p != null)
                .findFirst()
                .orElseThrow(() -> new AlreadyExistsException("Could not find a free project name to use.")); // Should not happen with random uuid

        // 2. Extract cdd from old project
        CourseDesignClient cdc = getCourseDesignClient(cycle);
        final CourseDesign design = cdc.getDesign(orgProj.getDesignId());
        final CourseDesignDefinition decodedDesign = CddCodec.decode(cycle, design.getDesign());
        final MutableCourseDesignDefinition mutableCdd = decodedDesign.toMutable();


        // 3. Get a partitioned list of local/other products
        Map<Boolean, List<Product>> partProducts = getPartitionedProducts(mutableCdd);
        List<Product> localProducts = partProducts.get(true);
        List<Product> otherProducts = partProducts.get(false);

        // 4.1 Copy local products
        HashMap<ProductId, ProductId> replaceHash = copyProjectProducts(caller, localProducts, newProject);

        // 4.2 Activate other products
        otherProducts.forEach(p -> {
            AddProjectProductRequestBuilder reqBuilder = new AddProjectProductRequestBuilder();
            reqBuilder.setProductId(p.getId().getId());
            reqBuilder.setProjectId(newProject.getProjectId());
            reqBuilder.setSkipActivation(true);

            try {
                getProjectMaterialCoordinatorClient(cycle).addProjectProduct(reqBuilder.build());
            } catch (MissingProjectProductException mppe) {
                LOGGER.warn("Product {} is not available for client. Ignoring it.", p.getId().
                        getId());
            } catch (Exception ex) {
                LOGGER.warn("Failed to add product, ignoring error.", ex);
            }
        });

        // 5. Replace old products with the new copies in cdd
        new ReplaceCddMaterials(mutableCdd).replaceProducts(replaceHash);

        // 6. Create the new design
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
            csr = csr.withName(newProject.getName());
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

        upr.setStageDatabank(databankId);
//        upr.setDesignId(newDesignId);
        upr.setStageDesignId(newDesignId);
        ccbc.updateOrgProject(upr.createUpdateProjectRequest());

        return newProject.getProjectId();
    }

    private void processComponent(MutableComponent component, Set<ProductId> candidates) {
        final ProductId id = ComponentUtil.getProductId(component);
        if(id != null) {
            candidates.add(id);
        }

        final List<MutableComponent> children = component.getChildren();
        if(children != null) {
            children.forEach(c -> processComponent(c, candidates));
        }
    }

    private void processResource(MutableResource resource, Set<ProductId> candidates) {
        final ProductId id = ResourceUtil.getProductId(resource);
        if(id != null) {
            candidates.add(id);
        }
    }

    private Map<Boolean, List<Product>> getPartitionedProducts(MutableCourseDesignDefinition mutableCdd) {
        Set<ProductId> candidates = new HashSet<>();
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
        List<Product> ps = pdClient.getProducts(candidates.stream().map(ProductId::getId).collect(Collectors.toList()));

        return ps.stream().collect(Collectors.partitioningBy(Product::isProjectProduct));
    }

    private HashMap<ProductId, ProductId> copyProjectProducts(long caller, List<Product> ps, OrgProject toProject) {
        return ps.stream().collect(HashMap<ProductId, ProductId>::new,
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

                    m.put(p.getId(), productId);
                },
                (m, u) -> {});
    }

    private void updateProductOwner(long caller, Product product, long projectId) {
        Product p = product.copy();
        p.setOwnerProjectId(projectId);

        UpdateProductRequest upr = new UpdateProductRequest(caller, p, false, false, false);
        getProductDirectoryClient(cycle).updateProduct(upr);
    }

    private ProductId createId() {
        return new AnonymousProductIdFactory().newId();
    }

}
