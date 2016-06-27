/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.module.core.AbstractCocositeJsModule;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */

@WebModuleMountpoint("/course.json")
public class CourseJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CourseJsonModule.class);

    @WebAction
    public RequestTarget onListOrgCourses(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        long caller = LoginUserAccountHelper.getUserId(cycle);

        List<OrgProject> projects =
                ccbc.listOrgProjects(orgId);

//        ByteArrayOutputStream os = toJsonObjectProjects(cycle, projects);

        // Temp
//        public RequestTarget onCourses(RequestCycle cycle) throws JsonProcessingException, IOException {

            String file = "/coursecatalog/courses.json";

            final URL res = this.getClass().getResource(file);

            byte[] data
                    = IOUtils.toByteArray(res);

            return json(data);
        // end temp
//        return jsonTarget(Collections.singletonMap("courses", Collections.emptyMap()));
    }

    private RequestTarget json(byte[] data) { // Temporary test function
        JsonRequestTarget target = AbstractCocositeJsModule.jsonTarget(data);
        target.allowCrossDomain(true);

        return target;
    }


    @WebAction
    public RequestTarget onCourseInfo(RequestCycle cycle, String strCourseId)
            throws Exception {
//        checkPermission(cycle, strCourseId);

//        return jsonTarget(Collections.singletonMap("members", uas.size()));
        return jsonTarget(Collections.singletonMap("course", Collections.emptyMap()));
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

//    private byte[] toJson(final List<UserAccount> uas) {
//        return new JsonEncoding() {
//            @Override
//            protected void encodeData(JsonGenerator generator) throws IOException {
//                generator.writeStartObject();
//                generator.writeArrayFieldStart("aaData");
//                for(UserAccount ua: uas) {
//                    generator.writeStartObject();
//                    generator.writeNumberField("userId", ua.getUserId());
//                    generator.writeEndObject();
//                }
//                generator.writeEndArray();
//                generator.writeEndObject();
//            }
//        }.encode();
//    }
}
