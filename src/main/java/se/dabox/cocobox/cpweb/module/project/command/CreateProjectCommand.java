package se.dabox.cocobox.cpweb.module.project.command;

import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.NewProjectRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.common.context.DwsRealmHelper;
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
import se.dabox.util.HybridLocaleUtils;

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
public class CreateProjectCommand extends AbstractCopyCommand {

    public CreateProjectCommand(RequestCycle cycle) {
        super(cycle);
    }

    public Long execute(CatalogCourse course) {

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);


        CourseCatalogClient ccClient = CacheClients.getClient(cycle, CourseCatalogClient.class);

        final CocoboxCoordinatorClient ccbc = getCocoboxCoordinatorClient(cycle);

        String wantedName = course.getName();

        // Try creating "coursename", coursename (1)" ... "coursename (N)", "coursename randomUUID"
        final Stream<String> names = Stream.concat(Stream.of(wantedName), Stream.concat(IntStream.range(1, 6).mapToObj(i -> wantedName + " (" + i + ")"), Stream.of(wantedName + " " + UUID.randomUUID().toString())));

        final OrgProject newProject = names
                .map (name -> {
                    try {
                        final NewProjectRequest npr = NewProjectRequest.newDesignedProject(name,
                                course.getOrgId(),
                                getLocale(), caller, getCountry(), getTz(), null, name, null, false);
                        return ccbc.newProject(caller, npr);
                    } catch (AlreadyExistsException ex) {
                        return null;
                    }
                }).filter(p -> p != null)
                .findFirst()
                .orElseThrow(() -> new AlreadyExistsException("Could not find a free project name to use.")); // Should not happen with random uuid

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

        upr.setStageDatabank(databankId);
        long designId = createBlankDesign(course.getOrgId(), course.getName(), getLocale());
        upr.setDesignId(designId);
        upr.setStageDesignId(designId);
        ccbc.updateOrgProject(upr.createUpdateProjectRequest());

        return newProject.getProjectId();
    }

    private long createBlankDesign(long orgId, String designName, Locale language) {

        long userId = LoginUserAccountHelper.getUserId(cycle);
        long bucketId = new GetCourseDesignBucketCommand(cycle).forOrg(orgId);

        MutableCourseDesignDefinition cdd = CddCodec.getBlank().toMutable();
        cdd.setInfo(new MutableCourseDesignInfo(null, null, null, null, Collections.emptyList()));

        String design =
                CddCodec.encode(cdd);

        CreateDesignRequest createReq = new CreateDesignRequest(userId, bucketId, designName,
                design, "");
        createReq.setEnabled(false);
        createReq.setLang(language);

        CreateDesignResponse response = getCourseDesignClient(cycle).createDesign(createReq);

        return response.getDesignId();
    }

    private TimeZone getTz() {
        String tzStr = DwsRealmHelper.getRealmConfiguration(cycle).getValue("cocobox.project.timezone.default");
        if (tzStr == null) {
            return null;
        }
        return TimeZone.getTimeZone(tzStr);
    }

    private Locale getLocale() {
        return CocositeUserHelper.getUserLocale(cycle);
    }

    private Locale getCountry() {
        String cStr = DwsRealmHelper.getRealmConfiguration(cycle).getValue(
                "cocobox.project.countrylocale.default");
        if (cStr == null) {
            return null;
        }
        return HybridLocaleUtils.toLocale(cStr);
    }
}
