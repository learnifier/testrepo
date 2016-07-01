/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import com.fasterxml.jackson.core.JsonGenerator;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.apache.commons.io.IOUtils;
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
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.reflections.util.ConfigurationBuilder.build;

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
        checkOrgPermission(cycle, strOrgId); // TODO: real permission check
//        long orgId = Long.valueOf(strOrgId);
        int intCourseId = Integer.valueOf(strCourseId);
        CatalogCourseId courseId = CatalogCourseId.valueOf(intCourseId);
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourseSession> sessions = ccc.listSessions(new ListCatalogSessionRequestBuilder().withCourseId(courseId).build());
        return sessions;
    }

    private RequestTarget json(byte[] data) { // Temporary test function
        JsonRequestTarget target = AbstractCocositeJsModule.jsonTarget(data);
        target.allowCrossDomain(true);

        return target;
    }

    @WebAction
    public RequestTarget onCourse(RequestCycle cycle, String strCourseId) throws Exception {
//        checkPermission(cycle, strCourseId);

//        return jsonTarget(Collections.singletonMap("members", uas.size()));
        String file = "/coursecatalog/course-" + strCourseId + ".json";

        final URL res = this.getClass().getResource(file);

        byte[] data
                = IOUtils.toByteArray(res);

        return json(data);
    }

    @WebAction
    public RequestTarget onSaveCourse(RequestCycle cycle, String strOrgId, String strCourseId) {
        ParamUtil.required(strCourseId, "strCourseId");


        return jsonTarget(Collections.singletonMap("status", "ok"));
    }

    @WebAction
    public RequestTarget onCreateCourse(RequestCycle cycle, String strOrgId) {
        ParamUtil.required(strOrgId, "strOrgId");
        long orgId = Long.valueOf(strOrgId);
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        final String description = cycle.getRequest().getParameter("description");
        CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        long caller = 0L;
        // TODO: Not sure what to do about locale
        final CatalogCourse course = ccc.createCourse(new CreateCourseRequest(caller, name, orgId, Locale.ENGLISH).withUpdate(UpdateCourseRequestBuilder.newCreateUpdateBuilder(caller).setDescription(description).build()));
        return jsonTarget(
                new HashMap<String, Object>(){
                    {
                        put("status", "ok");
                        put("id", course.getId());
                    }
                }
        );
    }

//    protected void checkPermission(RequestCycle cycle, Course course, String courseId) {
//        if (course == null) {
//            LOGGER.warn("Project {} doesn't exist.", courseId);
//
//            ErrorCodeRequestTarget error
//                    = new ErrorCodeRequestTarget(HttpServletResponse.SC_NOT_FOUND);
//            throw new RetargetException(error);
//        } else {
//            super.checkPermission(cycle, course);
//        }
//    }

    private byte[] toJson(final RequestCycle cycle,
                                      final List<CatalogCourse> courses) {

        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();
                generator.writeArrayFieldStart("aaData");

                for (CatalogCourse c : courses) {
                    generator.writeStartObject();
                    generator.writeStringField("name", c.getName());
                    generator.writeStringField("thumbnail", c.getThumbnailUrl());
                    generator.writeEndObject();
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }.encode();
    }
}
