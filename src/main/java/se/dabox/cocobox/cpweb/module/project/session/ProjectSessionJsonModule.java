/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.session;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.course.CatalogCourseJson;
import se.dabox.cocobox.cpweb.module.project.details.DateTimeFormatter;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.*;
import se.dabox.service.coursecatalog.client.session.impl.StandardDisenrollmentSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardEnrollmentSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardParticipationSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardSessionVisibility;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCode.O;
import static se.dabox.cocobox.cpweb.module.project.session.SessionField.visibility;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/session.json")
public class ProjectSessionJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectSessionJsonModule.class);

    @WebAction
    public RequestTarget onSetCourseSessionField(RequestCycle cycle, String strCourseSessionId) {
        final long caller = LoginUserAccountHelper.getUserId(cycle);
        final CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(Integer.valueOf(strCourseSessionId));

        final String pk = cycle.getRequest().getParameter("pk");
        final String value = cycle.getRequest().getParameter("value");
        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));

        checkSessionPermission(cycle, session);

        Map<String, Object> resultMap = new HashMap<>();
        final SessionField sessionField = SessionField.valueOf(pk);
        sessionField.accept(new SessionField.SessionFieldVisitor<Void>(){
            @Override
            public Void visitDescription() {
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setDescription(value).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitVisibility() {
                VisibilityMode mode = VisibilityMode.valueOf(value);
                final SessionVisibility visibility = session.getVisibility();
                final StandardSessionVisibility newVisibility;
                if(visibility == null) {
                    newVisibility = new StandardSessionVisibility(null, null, mode);
                } else {
                    newVisibility = new StandardSessionVisibility(visibility.getFrom(), visibility.getTo(), mode);
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setVisibilitySettings(newVisibility).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitEnrollmentMode() {
                EnrollmentMode mode = EnrollmentMode.valueOf(value);
                final EnrollmentSettings settings = session.getEnrollmentSettings();
                final StandardEnrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardEnrollmentSettings(false, null, null, mode);
                } else {
                    newSettings = new StandardEnrollmentSettings(settings.isEnabled(), settings.getFrom(), settings.getTo(), mode);
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitEnrollmentFromDate() {
//                Instant instant = Instant.parse(value);
                Instant instant = Instant.now();
                final EnrollmentSettings settings = session.getEnrollmentSettings();
                final StandardEnrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardEnrollmentSettings(false, instant, null, null);
                } else {
                    newSettings = new StandardEnrollmentSettings(settings.isEnabled(), instant, settings.getTo(), settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitEnrollmentToDate() {
//                Instant instant = Instant.parse(value);
                Instant instant = Instant.now();
                final EnrollmentSettings settings = session.getEnrollmentSettings();
                final StandardEnrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardEnrollmentSettings(false, null, instant, null);
                } else {
                    newSettings = new StandardEnrollmentSettings(settings.isEnabled(), settings.getFrom(), instant, settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitDisenrollmentMode() {
                EnrollmentMode mode = EnrollmentMode.valueOf(value);
                final DisenrollmentSettings settings = session.getDisenrollmentSettings();
                final StandardDisenrollmentSettings newSettings;
                if(settings == null) {
//                    newSettings = new StandardDisenrollmentSettings(Instant.now(), Instant.now(), mode);
                    newSettings = new StandardDisenrollmentSettings(null, null, mode);
                } else {
                    newSettings = new StandardDisenrollmentSettings(settings.getFrom(), settings.getTo(), mode);
                }
//                final ExtendedCatalogCourseSession extSession = getExtendedSession(ccc, courseSessionId);
//                extSession.getExtendedDisenrollmentSettings(); ???

                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setUnenrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitDisenrollmentFromDate() {
//                Instant instant = Instant.parse(value);
                Instant instant = Instant.now();
                final DisenrollmentSettings settings = session.getDisenrollmentSettings();
                final StandardDisenrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardDisenrollmentSettings(null, instant, null);
                } else {
                    newSettings = new StandardDisenrollmentSettings(settings.getFrom(), instant, settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setUnenrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitDisenrollmentToDate() {
//                Instant instant = Instant.parse(value);
                Instant instant = Instant.now();
                final DisenrollmentSettings settings = session.getDisenrollmentSettings();
                final StandardDisenrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardDisenrollmentSettings(null, instant, null);
                } else {
                    newSettings = new StandardDisenrollmentSettings(settings.getFrom(), instant, settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setUnenrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public Void visitParticipationEnabled() {
                final ParticipationSettings settings = session.getParticipationSettings();
                final StandardParticipationSettings newSettings;
                final boolean newVal;
                if(settings == null) {
                    newVal = true; // true = toggled default false
                    newSettings = new StandardParticipationSettings(newVal, false, false);
                } else {
                    newVal = !settings.isEnabled();
                    newSettings = new StandardParticipationSettings(newVal, settings.isShowName(), settings.isShowThumbnail());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrolledSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                resultMap.put("enabled", newVal);
                return null;
            }

            @Override
            public Void visitParticipationShowName() {
                final ParticipationSettings settings = session.getParticipationSettings();
                final StandardParticipationSettings newSettings;
                final boolean newVal;
                if(settings == null) {
                    newVal = true; // true = toggled default false
                    newSettings = new StandardParticipationSettings(false, newVal, false);
                } else {
                    newVal = !settings.isShowName();
                    newSettings = new StandardParticipationSettings(settings.isEnabled(), newVal, settings.isShowThumbnail());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrolledSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                resultMap.put("enabled", newVal);
                return null;
           }

            @Override
            public Void visitParticipationShowThumbnail() {
                final ParticipationSettings settings = session.getParticipationSettings();
                final StandardParticipationSettings newSettings;
                final boolean newVal;
                if(settings == null) {
                    newVal = true; // true = toggled default false
                    newSettings = new StandardParticipationSettings(false, false, newVal);
                } else {
                    newVal = !settings.isShowThumbnail();
                    newSettings = new StandardParticipationSettings(settings.isEnabled(), settings.isShowName(), newVal);
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrolledSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                resultMap.put("enabled", newVal);
                return null;
            }
        });

        resultMap.put("status", "OK");

        return jsonTarget(resultMap);
    }

    @WebAction
    public RequestTarget onListCourses(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withOrgId(orgId).build());

        final List<HashMap<String, Object>> map = courses.stream().map(c ->
                new HashMap<String, Object>() {{
                    put("id", c.getId().getId());
                    put("text", c.getName());
                }}
        ).collect(Collectors.toList());
        return(jsonTarget(map));
    }


    private ExtendedCatalogCourseSession getExtendedSession(CourseCatalogClient ccc, CatalogCourseSessionId courseSessionId) {
        return CollectionsUtil.singleItemOrNull(ccc.listExtendedSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));
    }





    protected void checkSessionPermission(RequestCycle cycle, CatalogCourseSession courseSession) { // TODO: implement

//        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
//        OrgProject project = ccbc.getProject(prjId);
//        checkPermission(cycle, project, strProjectId);


//        if (project == null) {
//            LOGGER.warn("Project {} doesn't exist.", strProjectId);
//
//            ErrorCodeRequestTarget error
//                    = new ErrorCodeRequestTarget(HttpServletResponse.SC_NOT_FOUND);
//
//            throw new RetargetException(error);
//        } else {
//            super.checkPermission(cycle, project);
//        }
    }

}
