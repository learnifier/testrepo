/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Strings;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.module.core.AbstractCocositeJsModule;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.create.CreateCourseRequest;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.course.update.UpdateCourseRequest;
import se.dabox.service.coursecatalog.client.course.update.UpdateCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.ParamUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.sun.org.apache.xerces.internal.impl.xpath.regex.CaseInsensitiveMap.get;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */

@WebModuleMountpoint("/course.json")
public class CourseJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CourseJsonModule.class);

    @WebAction
    public List<CatalogCourse> onListOrgCourses(RequestCycle cycle, String strOrgId) {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withOrgId(orgId).build());
        return courses;
    }

    @WebAction
    public List<CatalogCourseSession> onListSessions(RequestCycle cycle, String strOrgId, String strCourseId) {
        // TODO: strOrgId not used, remove here and in js.

        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build());
        if(courses == null || courses.size() != 1) {
            return null;
        }
        CatalogCourse course = courses.get(0);
        checkOrgPermission(cycle, course.getOrgId());

        final List<CatalogCourseSession> sessions = ccc.listSessions(new ListCatalogSessionRequestBuilder().withCourseId(courseId).build());
        return sessions;
    }

    @WebAction
    public CatalogCourse onCourse(RequestCycle cycle, String strCourseId) throws Exception {
        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build());
        if(courses == null || courses.size() != 1) {
            return null;
        }
        CatalogCourse course = courses.get(0);
        checkOrgPermission(cycle, course.getOrgId());
        return course;
    }

    @WebAction
    public RequestTarget onSaveCourse(RequestCycle cycle, String strCourseId) {
        ParamUtil.required(strCourseId, "strCourseId");
        long caller = LoginUserAccountHelper.getUserId(cycle);
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        final String description = cycle.getRequest().getParameter("description");

        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withCourseId(courseId).build());
        if(courses == null || courses.size() != 1) {
            return null;
        }
        CatalogCourse course = courses.get(0);

        checkOrgPermission(cycle, course.getOrgId());

        final UpdateCourseRequestBuilder updateReq = UpdateCourseRequestBuilder.newBuilder(caller, courseId);
        boolean change = false;
        if(!Objects.equals(course.getName(), name)) {
            updateReq.setName(name);
            change = true;
        }
        if(!Objects.equals(course.getDescription(), description)) {
            updateReq.setDescription(description);
            change = true;
        }
        if(change) {
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

        checkOrgPermission(cycle, orgId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        long caller = LoginUserAccountHelper.getUserId(cycle);

        final CreateCourseRequest ccr = new CreateCourseRequest(caller, name, orgId, getUserLocale(cycle));
        if(description != null) {
            ccr.withUpdate(UpdateCourseRequestBuilder.newCreateUpdateBuilder(caller).setDescription(description).build());
        }

        final CatalogCourse course = ccc.createCourse(ccr);
        return jsonTarget(
                new HashMap<String, Object>(){{
                    put("status", "ok");
                    put("id", course.getId());
                }}
        );
    }
}
