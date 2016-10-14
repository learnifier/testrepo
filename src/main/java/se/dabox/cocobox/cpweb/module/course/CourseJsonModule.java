/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.command.CopyProjectCommand;
import se.dabox.cocobox.cpweb.module.project.command.CreateProjectCommand;
import se.dabox.cocobox.cpweb.module.util.CpwebParameterUtil;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.service.common.ccbc.AlreadyExistsException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.coursecatalog.client.CocoboxCourseSourceConstants;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.create.CreateCourseRequest;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.course.update.UpdateCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSessionId;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.ParamUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.unixdeveloper.druwa.RetargetException;
import se.dabox.cocosite.messagepage.GenericMessagePageFactory;
import se.dabox.cocosite.webmessage.WebMessage;
import se.dabox.cocosite.webmessage.WebMessageType;
import se.dabox.cocosite.webmessage.WebMessages;
import se.dabox.service.client.CacheClients;
import se.dabox.service.coursecatalog.client.DeniedException;
import se.dabox.service.coursecatalog.client.NotFoundException;

import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */

@WebModuleMountpoint("/course.json")
public class CourseJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CourseJsonModule.class);

    @WebAction
    public List<CatalogCourseJson> onListOrgCourses(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withOrgId(orgId).build());

        return CollectionsUtil.transformList(courses, CatalogCourseJson::new);
    }

    @WebAction
    public List<CatalogCourseSession> onListSessions(RequestCycle cycle, String strCourseId) {
        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final CatalogCourse course = CollectionsUtil.singleItemOrNull(ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build()));
        checkOrgPermission(cycle, course.getOrgId());

        final List<CatalogCourseSession> sessions = ccc.listSessions(new ListCatalogSessionRequestBuilder().withCourseId(courseId).build());
        return sessions;
    }

    @WebAction
    public CatalogCourseJson onCourse(RequestCycle cycle, String strCourseId) throws Exception {
        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final CatalogCourse course = CollectionsUtil.singleItemOrNull(ccc.listCourses(
                new ListCatalogCourseRequestBuilder().withCourseId(courseId).build()));

        if (course == null) {
            LOGGER.warn("Course not found: {}", strCourseId);
            throw new RetargetException(GenericMessagePageFactory.
                    newNotFoundPage().
                    withMessageText("Course not found").
                    build());
        }

        checkOrgPermission(cycle, course.getOrgId());
        return new CatalogCourseJson(course);
    }

    @WebAction
    public RequestTarget onSaveCourse(RequestCycle cycle, String strCourseId) {
        ParamUtil.required(strCourseId, "strCourseId");
        long caller = LoginUserAccountHelper.getUserId(cycle);
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        final String description = cycle.getRequest().getParameter("description");
        final String thumbnailUrl = cycle.getRequest().getParameter("thumbnailUrl");

        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final CatalogCourse course = CollectionsUtil.singleItemOrNull(ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build()));

        checkOrgPermission(cycle, course.getOrgId());

        final UpdateCourseRequestBuilder updateReq = UpdateCourseRequestBuilder.newBuilder(caller, courseId);
        boolean change = false;
        if (!Objects.equals(course.getName(), name)) {
            updateReq.setName(name);
            change = true;
        }
        if (!Objects.equals(course.getDescription(), description)) {
            updateReq.setDescription(description);
            change = true;
        }
        if (!Objects.equals(course.getThumbnailUrl(), thumbnailUrl)) {
            updateReq.setThumbnail(thumbnailUrl);
            change = true;
        }

        if (change) {
            ccc.updateCourse(updateReq.build());
        }
        return jsonTarget(Collections.singletonMap("status", "ok"));
    }

    @WebAction
    public RequestTarget onCreateCourse(RequestCycle cycle, String strOrgId) {
        ParamUtil.required(strOrgId, "strOrgId");
        long orgId = Long.valueOf(strOrgId);
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        final String description = cycle.getRequest().getParameter("description");
        final String thumbnailUrl = cycle.getRequest().getParameter("thumbnailUrl");

        checkOrgPermission(cycle, orgId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        long caller = LoginUserAccountHelper.getUserId(cycle);

        UpdateCourseRequestBuilder updateReq = UpdateCourseRequestBuilder.newCreateUpdateBuilder(caller);
        boolean change = false;
        CreateCourseRequest ccr = new CreateCourseRequest(caller, name, orgId, getUserLocale(cycle));
        if (description != null) {
            updateReq = updateReq.setDescription(description);
            change = true;
        }
        if (thumbnailUrl != null) {
            updateReq = updateReq.setThumbnail(thumbnailUrl);
            change = true;
        }
        if (change) {
            ccr = ccr.withUpdate(updateReq.build());
        }

        final CatalogCourse course = ccc.createCourse(ccr);
        return jsonTarget(
                new HashMap<String, Object>() {{
                    put("status", "ok");
                    put("id", course.getId());
                }}
        );
    }

    @WebAction
    public Map<String, String> onDeleteCourse(RequestCycle cycle, String strOrgId) {
        int courseId = DruwaParamHelper.getMandatoryIntParam(LOGGER, cycle.getRequest(), "id");

        CourseCatalogClient client = CacheClients.getClient(cycle, CourseCatalogClient.class);

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
        try {
            client.deleteCourse(caller, CatalogCourseId.valueOf(courseId));
        } catch (NotFoundException notFoundException) {
            //Ignore
        } catch (DeniedException deniedException) {
            return Collections.singletonMap("status", "notempty");
        }

        WebMessages.getInstance(cycle).addMessage(WebMessage.createTextMessage("Course deleted", WebMessageType.success));

        return Collections.singletonMap("status", "ok");
    }

    @WebAction(methods = HttpMethod.POST)
    public Map<String, Object> onCopySession(RequestCycle cycle) {
        final String strCourseSessionId = cycle.getRequest().getParameter("sessionId");
        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(Integer.parseInt(strCourseSessionId));
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));

        if (session.getSource() != null && CocoboxCourseSourceConstants.PROJECT.equals(session.getSource().getType())) {
            final String strProjectId = session.getSource().getId();
            final CocoboxCoordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient(cycle);
            Long projectId = CpwebParameterUtil.stringToLong(strProjectId);
            OrgProject project = null;
            if (projectId != null) {
                project = cocoboxCordinatorClient.getProject(projectId);
            }
            if (project == null) {
                return ImmutableMap.of("status", "error", "message", "Project missing.");
            }

            checkPermission(cycle, project);
            checkProjectPermission(cycle, project, CocoboxPermissions.CP_CREATE_PROJECT);

            try {
                Long newProjId = new CopyProjectCommand(cycle).execute(project);

                if (newProjId != null) {
                    return ImmutableMap.of(
                            "status", "ok",
                            "projectId", newProjId);
                } else {
                    return ImmutableMap.of("status", "error", "message", "Failed to copy project/course session.");
                }
            } catch (AlreadyExistsException e) {
                return ImmutableMap.of("status", "error", "message", "Could not find a free name. Copy aborted.");
            }
        } else {
            throw new IllegalStateException("Can not copy an external project.");
        }
    }

    @WebAction(methods = HttpMethod.POST)
    public Map<String, Object> onCreateSession(RequestCycle cycle) {
        final String strCourseId = cycle.getRequest().getParameter("courseId");

        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        final CatalogCourseId courseId = CatalogCourseId.valueOf(Integer.parseInt(strCourseId));
        final CatalogCourse course = CollectionsUtil.singleItemOrNull(ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build()));
        if(course == null) {
            throw new NotFoundException("Course not found.");
        }
        checkOrgPermission(cycle, course.getOrgId());
        try {
            Long newProjectId = new CreateProjectCommand(cycle).execute(course);
            if (newProjectId != null) {
                return ImmutableMap.of(
                        "status", "ok",
                        "projectId", newProjectId);
            } else {
                return ImmutableMap.of("status", "error", "message", "Failed to create course session.");
            }
        } catch (AlreadyExistsException e) {
            return ImmutableMap.of("status", "error", "message", "Could not find a free name. Copy aborted.");
        }
    }
}
