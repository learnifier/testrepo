package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequest;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.coursecatalog.client.CocoboxCourseSourceConstants;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.EnrollmentMode;
import se.dabox.service.coursecatalog.client.session.create.CreateSessionRequest;
import se.dabox.service.coursecatalog.client.session.impl.StandardCourseSessionSource;
import se.dabox.service.coursecatalog.client.session.impl.StandardDisenrollmentSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardEnrollmentSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardParticipationSettings;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;

import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCourseCatalogClient;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */

public class CreateCatalogCourseSessionCmd {

    private RequestCycle cycle;
    private long caller;

    public CreateCatalogCourseSessionCmd(RequestCycle cycle, long caller) {
        this.cycle = cycle;
        this.caller = caller;
    }

    public void run(OrgProject project, CatalogCourse course) {
        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        UpdateProjectRequestBuilder updateBuilder
                = new UpdateProjectRequestBuilder(caller, project.getProjectId());

        final UpdateSessionRequestBuilder upBuilder =
                UpdateSessionRequestBuilder.newCreateBuilder(caller)
                        .setSource(new StandardCourseSessionSource(CocoboxCourseSourceConstants.PROJECT, Long.toString(project.getProjectId())))
                        .setEnrolledSettings(new StandardParticipationSettings(true, true, true))
                        .setEnrollmentSettings(new StandardEnrollmentSettings(false, null, null, EnrollmentMode.direct))
                        .setUnenrollmentSettings(new StandardDisenrollmentSettings(null, null, EnrollmentMode.direct));

        final UpdateSessionRequest updateSource = upBuilder.createUpdateSessionRequest();

        final CreateSessionRequest csr = new CreateSessionRequest(caller, course.getId())
                .withName(course.getName())
                .withLocale(project.getLocale())
                .withUpdate(updateSource);

        final CatalogCourseSession ccs = ccc.createSession(csr);

        updateBuilder.setCourseSessionId(ccs.getId().getId());
        UpdateProjectRequest upr = updateBuilder.createUpdateProjectRequest();

        ccbc.updateOrgProject(upr);
    }

}
