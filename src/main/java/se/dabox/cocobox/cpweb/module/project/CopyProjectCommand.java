package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.CreateDesignRequest;
import se.dabox.service.common.coursedesign.CreateDesignResponse;
import se.dabox.service.common.coursedesign.techinfo.CpDesignTechInfo;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.CourseDesignInfo;
import se.dabox.service.common.coursedesign.v1.CourseDesignXmlMutator;
import se.dabox.service.common.coursedesign.v1.mutable.MutableComponent;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignInfo;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseScene;
import se.dabox.service.common.coursedesign.v1.mutable.MutableResource;
import se.dabox.service.common.coursedesign.validator.CourseDesignDefinitionValidator;
import se.dabox.service.common.coursedesign.validator.ValidationException;
import se.dabox.service.common.duration.DurationString;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSessionId;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.apache.poi.poifs.crypt.CipherAlgorithm.des;
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

        final ProjectType type = project.getType();
        if(type != ProjectType.DESIGNED_PROJECT) {
            throw new UnsupportedOperationException("Can only copy projects of type designed");
        }
        final String subtype = project.getSubtype();
        OrgProject orgProj = (OrgProject)project; // How do i check project type?

        final Long designId = orgProj.getDesignId();
        List<String> products = products = ProductsHelper.getDesignProducts(cycle, designId, orgProj.getOrgId());
        List<Long> orgmats = getDesignOrgmats(cycle, designId);


        CreateProjectSessionProcessor processor = new CreateProjectSessionProcessor(orgProj.getOrgId());

        String cancelUrl = null;

        final NewProjectSession nps = new NewProjectSession(type.toString(), orgmats, products, processor,
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
            if(courseId != null) {
                courseIdRaw = courseId.getId();
            }
        }
        nps.setCourseId(courseIdRaw);

        final CreateProjectGeneral input = new CreateProjectGeneral(); // TODO: Remove form object from NewProjectSession
        input.setProjectname(newName==null?"zzz":newName); // TODO: Remove conditional when properly called.
        input.setCountry(orgProj.getCountry());


        CourseDesignClient cdc = getCourseDesignClient(cycle);
        final CourseDesign design = cdc.getDesign(orgProj.getDesignId());
        final String strDesign = design.getDesign();

        final CourseDesignDefinition decodedDesign = CddCodec.decode(cycle, design.getDesign());
        final MutableCourseDesignDefinition mutableCdd = decodedDesign.toMutable();

        // 0. We have created a project with empty cdd

        // 1. Get list of local products we should copy from old cdd.
        List<UUID> localComponents = getLocalComponents(mutableCdd);

        // 2. Copy them

        // 3. Replace them in cdd with generic operation

        // 4. Set cdd in project


        final CourseDesignDefinition cdd = mutableCdd.toCourseDesignDefinition();

        String cddXml = CddCodec.encode(cdd);
        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
//        CreateDesignRequest cdr = new CreateDesignRequest(caller, null, "", cddXml, "");
//        String techInfo = CpDesignTechInfo.createStageTechInfo(project.getProjectId());
//        cdr.setTechInfo(techInfo);
//        CreateDesignResponse des = cdc.createDesign(cdr);
//
//        final long newDesignId = des.getDesignId();
//
//        input.setDesign(String.valueOf(newDesignId));


        input.setProjectlang(orgProj.getLocale());
        input.setTimezone(orgProj.getTimezone());
        nps.setCreateProjectGeneral(input);

        nps.storeInSession(cycle.getSession());

//        return nps.process(cycle, null);
        return null;
    }

    private List<UUID> getLocalComponents(MutableCourseDesignDefinition mutableCdd) {
        final List<MutableComponent> components = mutableCdd.getComponents();
        components.stream().forEach(c -> {
            final String type1 = c.getType();
            final UUID cid = c.getCid();

        });

        final List<MutableResource> resources = mutableCdd.getResources();
        resources.stream().forEach(r -> {
            final String materialId = r.getMaterialId();
            // TODO: Handle resources
        });

        final List<MutableCourseScene> scenes = mutableCdd.getScenes();
        scenes.stream().forEach(s -> {
            s.getComponents();
        });

        return Collections.emptyList();
    }

    public static CourseCatalogClient getCourseCatalogClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseCatalogClient.class);
    }


    public static CourseDesignClient getCourseDesignClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

}
