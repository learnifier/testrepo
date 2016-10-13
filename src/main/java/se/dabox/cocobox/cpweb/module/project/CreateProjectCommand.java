package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.NewProjectRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.CreateDesignRequest;
import se.dabox.service.common.coursedesign.CreateDesignResponse;
import se.dabox.service.common.coursedesign.GetCourseDesignBucketCommand;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.mutable.MutableCourseDesignInfo;
import se.dabox.service.coursecatalog.client.CocoboxCourseSourceConstants;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.create.CreateSessionRequest;
import se.dabox.service.coursecatalog.client.session.impl.StandardCourseSessionSource;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class CreateProjectCommand {

    private final RequestCycle cycle;
    public CreateProjectCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    public Long execute(CatalogCourse course) {

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);


        CourseCatalogClient ccClient = CacheClients.getClient(cycle, CourseCatalogClient.class);

        // 1. Create project w/o cdd, databank and course session.

//        Locale language = course.getLanguage(); // Slightly questionable

        TimeZone timezone = TimeZone.getDefault(); // Bad idea, just temporary

        Locale locale = CocositeUserHelper.getUserLocale(cycle);

        final CocoboxCoordinatorClient ccbc = getCocoboxCoordinatorClient(cycle);

        String wantedName = course.getName();

        // Try creating "coursename", coursename (1)" ... "coursename (N)", "coursename randomUUID"
        final Stream<String> names = Stream.concat(Stream.of(wantedName), Stream.concat(IntStream.range(1, 6).mapToObj(i -> wantedName + " (" + i + ")"), Stream.of(wantedName + " " + UUID.randomUUID().toString())));

        final OrgProject newProject = names
                .map (name -> {
                    try {
                        final NewProjectRequest npr = NewProjectRequest.newDesignedProject(name,
                                course.getOrgId(),
                                locale, caller, locale, timezone, null, name, null, false);
                        return ccbc.newProject(caller, npr);
                    } catch (AlreadyExistsException ex) {
                        return null;
                    }
                }).filter(p -> p != null)
                .findFirst()
                .orElseThrow(() -> new AlreadyExistsException("Could not find a free project name to use.")); // Should not happen with random uuid

        // 4. Create the new design
//        final CourseDesignDefinition cdd = mutableCdd.toCourseDesignDefinition();
//
//        String cddXml = CddCodec.encode(cdd);
//        CreateDesignRequest cdr = new CreateDesignRequest(caller, null, "", cddXml, "");
//        String techInfo = CpDesignTechInfo.createStageTechInfo(project.getProjectId());
//        cdr.setTechInfo(techInfo);
//        CreateDesignResponse des = cdc.createDesign(cdr);
//
//        final long newDesignId = des.getDesignId();

        // 5. Create session
        UpdateProjectRequestBuilder upr
                = new UpdateProjectRequestBuilder(caller, newProject.getProjectId());

        CreateSessionRequest csr = new CreateSessionRequest(caller, course.getId());
        csr = csr.withName(newProject.getName());
        UpdateSessionRequest update
                = UpdateSessionRequestBuilder.newCreateBuilder(caller).setSource(
                new StandardCourseSessionSource(CocoboxCourseSourceConstants.PROJECT, Long.toString(
                        newProject.getProjectId()))).
                createUpdateSessionRequest();

        csr = csr.withUpdate(update);

        CatalogCourseSession session = ccClient.createSession(csr);
        upr.setCourseSessionId(session.getId().getId());

        long databankId = ccbc.createDatabank(0, newProject.getProjectId());

        // 6. Set data bank + design ids.
        upr.setStageDatabank(databankId);
        long designId = createDesign(course.getOrgId(), course.getName(), locale);
        upr.setDesignId(designId);
        upr.setStageDesignId(designId);
        ccbc.updateOrgProject(upr.createUpdateProjectRequest());

        return newProject.getProjectId();
    }

    private long createDesign(long orgId, String designName, Locale language) {

        long userId = LoginUserAccountHelper.getUserId(cycle);
        long bucketId = new GetCourseDesignBucketCommand(cycle).forOrg(orgId);

        MutableCourseDesignDefinition cdd = CddCodec.getBlank().toMutable();
        cdd.setInfo(new MutableCourseDesignInfo(null, null, null, null, Collections.emptyList()));

        String design =
                CddCodec.encode(cdd);

        CreateDesignRequest createReq = new CreateDesignRequest(userId, bucketId, designName,
                design, "");
        //Enabled = auto-add in this context
        createReq.setEnabled(false);
        createReq.setLang(language);

        CreateDesignResponse response = getCourseDesignClient(cycle).createDesign(createReq);

        return response.getDesignId();
    }



    private static CocoboxCoordinatorClient getCocoboxCoordinatorClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    private static CourseDesignClient getCourseDesignClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }
}
