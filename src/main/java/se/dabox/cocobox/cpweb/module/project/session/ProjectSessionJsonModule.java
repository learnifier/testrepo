/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.session;

import java.text.DateFormat;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.CreateCatalogCourseSessionCmd;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSessionId;
import se.dabox.service.coursecatalog.client.session.DisenrollmentSettings;
import se.dabox.service.coursecatalog.client.session.EnrollmentMode;
import se.dabox.service.coursecatalog.client.session.EnrollmentSettings;
import se.dabox.service.coursecatalog.client.session.ExtendedCatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.ParticipationSettings;
import se.dabox.service.coursecatalog.client.session.SessionVisibility;
import se.dabox.service.coursecatalog.client.session.VisibilityMode;
import se.dabox.service.coursecatalog.client.session.impl.StandardDisenrollmentSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardEnrollmentSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardParticipationSettings;
import se.dabox.service.coursecatalog.client.session.impl.StandardSessionVisibility;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import net.unixdeveloper.druwa.DruwaApplication;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.login.client.CocoboxUserAccount;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/session.json")
public class ProjectSessionJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectSessionJsonModule.class);

    @WebAction
    public RequestTarget onSetCourseSessionField(RequestCycle cycle, String strProjectId) {
        final long caller = LoginUserAccountHelper.getUserId(cycle);

        final Integer projectId = Integer.valueOf(strProjectId);

        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final OrgProject project = ccbc.getProject(projectId);

        checkPermission(cycle, project, strProjectId);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_EDIT_PROJECT);


        final CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(project.getCourseSessionId());

        final String pk = cycle.getRequest().getParameter("pk");
        final String value = cycle.getRequest().getParameter("value");
        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));


        Map<String, Object> resultMap = new HashMap<>();
        final SessionField sessionField = SessionField.valueOf(pk);
        String responseValue = sessionField.accept(new SessionField.SessionFieldVisitor<String>(){
            @Override
            public String visitDescription() {
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setDescription(value).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public String visitVisibility() {
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
            public String visitEnrollmentMode() {
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
            public String visitEnrollmentFromDate() {
                final Instant newVal = instantFromString(value, project.getTimezone());
                final EnrollmentSettings settings = session.getEnrollmentSettings();
                final StandardEnrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardEnrollmentSettings(false, newVal, null, null);
                } else {
                    newSettings = new StandardEnrollmentSettings(settings.isEnabled(), newVal, settings.getTo(), settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);

                return toDateString(newVal);
            }

            @Override
            public String visitEnrollmentToDate() {
                final Instant newVal = instantFromString(value, project.getTimezone());
                final EnrollmentSettings settings = session.getEnrollmentSettings();
                final StandardEnrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardEnrollmentSettings(false, null, newVal, null);
                } else {
                    newSettings = new StandardEnrollmentSettings(settings.isEnabled(), settings.getFrom(), newVal, settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setEnrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);

                return toDateString(newVal);
            }

            @Override
            public String visitDisenrollmentMode() {
                EnrollmentMode mode = EnrollmentMode.valueOf(value);
                final DisenrollmentSettings settings = session.getDisenrollmentSettings();
                final StandardDisenrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardDisenrollmentSettings(null, null, mode);
                } else {
                    newSettings = new StandardDisenrollmentSettings(settings.getFrom(), settings.getTo(), mode);
                }

                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setUnenrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }

            @Override
            public String visitDisenrollmentFromDate() {
                final Instant newVal = instantFromString(value, project.getTimezone());
                final DisenrollmentSettings settings = session.getDisenrollmentSettings();
                final StandardDisenrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardDisenrollmentSettings(newVal, null, null);
                } else {
                    newSettings = new StandardDisenrollmentSettings(newVal, settings.getTo(), settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setUnenrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);

                return toDateString(newVal);
            }

            @Override
            public String visitDisenrollmentToDate() {
                final Instant newVal = instantFromString(value, project.getTimezone());
                final DisenrollmentSettings settings = session.getDisenrollmentSettings();
                final StandardDisenrollmentSettings newSettings;
                if(settings == null) {
                    newSettings = new StandardDisenrollmentSettings(null, newVal, null);
                } else {
                    newSettings = new StandardDisenrollmentSettings(settings.getFrom(), newVal, settings.getMode());
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setUnenrollmentSettings(newSettings).createUpdateSessionRequest();
                ccc.updateSession(usr);

                return toDateString(newVal);
            }

            @Override
            public String visitParticipationEnabled() {
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
            public String visitParticipationShowName() {
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
            public String visitParticipationShowThumbnail() {
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

            private String toDateString(Instant value) {
                if (value == null) {
                    return null;
                }

                Locale locale = CocositeUserHelper.getUserLocale(cycle);
                DateTimeFormatter formatter
                        = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG).
                                withLocale(locale).
                                withZone(project.getTimezone().toZoneId());

                return formatter.format(value);
            }
        });

        resultMap.put("status", "OK");
        resultMap.put("value", responseValue);

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

    @WebAction
    public RequestTarget onUpdateCourse(RequestCycle cycle, String strProjectId) {
        final long caller = LoginUserAccountHelper.getUserId(cycle);
        final Integer projectId = Integer.valueOf(strProjectId);
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final OrgProject project = ccbc.getProject(projectId);

        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        checkPermission(cycle, project, strProjectId);

        final CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(project.getCourseSessionId());
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));

        final String strCourseId = cycle.getRequest().getParameter("courseId");


        if("".equals(strCourseId)) {
            ccbc.updateOrgProject(new UpdateProjectRequestBuilder(caller, projectId).setCourseSessionId(null).createUpdateProjectRequest());
            ccc.deleteSession(caller, session.getId());
        } else {
            final CatalogCourseId courseId = CatalogCourseId.valueOf(Integer.valueOf(strCourseId));
            final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setCourseId(courseId).createUpdateSessionRequest();
            ccc.updateSession(usr);
        }
        return jsonTarget(Collections.singletonMap("status", "ok"));
    }

    @WebAction
    public RequestTarget onCreateSession(RequestCycle cycle, String strProjectId) {

        // TODO: Create session coupled to project and course.
        final long caller = LoginUserAccountHelper.getUserId(cycle);
        final Integer projectId = Integer.valueOf(strProjectId);
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final OrgProject project = ccbc.getProject(projectId);
        checkPermission(cycle, project, strProjectId);

        final String strCourseId = cycle.getRequest().getParameter("courseId");
        CatalogCourseId courseId = CatalogCourseId.valueOf(Integer.valueOf(strCourseId));
        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final CatalogCourse course = CollectionsUtil.singleItemOrNull(ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build()));

        new CreateCatalogCourseSessionCmd(cycle, caller).run(project, course);

        return jsonTarget(Collections.singletonMap("status", "ok"));



    }



    private ExtendedCatalogCourseSession getExtendedSession(CourseCatalogClient ccc, CatalogCourseSessionId courseSessionId) {
        return CollectionsUtil.singleItemOrNull(ccc.listExtendedSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));
    }


    private Instant instantFromString(String value, TimeZone tz) {
        if(value == null || "".equals(value)) {
            return null;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime parsedDate = LocalDateTime.parse(value, formatter);
            return parsedDate.atZone(tz.toZoneId()).toInstant();
        }
    }

    protected void checkPermission(RequestCycle cycle, OrgProject project, String strProjectId) {
        if (project == null) {
            LOGGER.warn("Project {} doesn't exist.", strProjectId);

            ErrorCodeRequestTarget error
                    = new ErrorCodeRequestTarget(HttpServletResponse.SC_NOT_FOUND);

            throw new RetargetException(error);
        } else {
            super.checkPermission(cycle, project);
        }
    }
}
