/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import com.fasterxml.jackson.core.JsonGenerator;
import com.sun.org.apache.xml.internal.resolver.Catalog;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.user.UserModule;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.module.core.AbstractCocositeJsModule;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.category.CatalogCategoryId;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.ParamUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static se.dabox.service.common.ccbc.participation.filter.FilterParticipationSortField.id;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */

@WebModuleMountpoint("/course.json")
public class CourseJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CourseJsonModule.class);

    @WebAction
    public List<CatalogCourse> onListOrgCourses(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        CourseCatalogClient ccc = getCourseCatalogClient(cycle);

        final List<CatalogCourse> courses = ccc.listCourses(new ListCatalogCourseRequestBuilder().withOrgId(orgId).build());
        long caller = LoginUserAccountHelper.getUserId(cycle);
        return courses;
    }

    @WebAction
    public RequestTarget onListSessions(RequestCycle cycle, String strOrgId, String strCourseId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);
//        long courseId = Long.c(strCourseId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);


        String file = "/coursecatalog/sessions-" + strCourseId + ".json";

        final URL res = this.getClass().getResource(file);

        byte[] data
                = IOUtils.toByteArray(res);

        return json(data);
    }

    private RequestTarget json(byte[] data) { // Temporary test function
        JsonRequestTarget target = AbstractCocositeJsModule.jsonTarget(data);
        target.allowCrossDomain(true);

        return target;
    }


    @WebAction
    public RequestTarget onCourse(RequestCycle cycle, String strCourseId)
            throws Exception {
//        checkPermission(cycle, strCourseId);

//        return jsonTarget(Collections.singletonMap("members", uas.size()));
        String file = "/coursecatalog/course-" + strCourseId + ".json";

        final URL res = this.getClass().getResource(file);

        byte[] data
                = IOUtils.toByteArray(res);

        return json(data);
    }

    @WebAction
    public RequestTarget onSaveCourse(RequestCycle cycle, String strCourseId) {
        ParamUtil.required(strCourseId, "strCourseId");
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        final String description = cycle.getRequest().getParameter("description");

        return jsonTarget(Collections.singletonMap("status", "ok"));
    }

    @WebAction
    public RequestTarget onCreateCourse(RequestCycle cycle) {
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "name");
        final String description = cycle.getRequest().getParameter("description");

        return jsonTarget(Collections.singletonMap("status", "ok"));
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
