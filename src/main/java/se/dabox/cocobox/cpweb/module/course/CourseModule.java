/*
 * (c) Dabox AB 2016 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.cocosite.org.MiniOrgInfo;

import java.util.Map;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */

@WebModuleMountpoint("/courses")
public class CourseModule extends AbstractProjectWebModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CourseModule.class);
    public static final String LIST_ACTION = "list";
    public static final String OVERVIEW_ACTION = "overview";

    @WebAction
    public RequestTarget onList(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        map.put("org", org);

        return new FreemarkerRequestTarget("/course/listCourses.html", map);
    }

    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String courseId) {

//        OrgProject project =
//                getProject(cycle, courseId);
//
//        checkPermission(cycle, course);
//        checkCoursePermission(cycle, course, CocoboxPermissions.CP_VIEW_PROJECT);

        Map<String, Object> map = createMap();

//        addCommonMapValues(map, project, cycle);

        return new FreemarkerRequestTarget("/project/courseOverview.html", map);
    }


    @WebAction
    public RequestTarget onEditCourse(RequestCycle cycle, String strCourseId) {
        Map<String, Object> map = createMap();
        map.put("courseId", strCourseId);
        return new FreemarkerRequestTarget("/course/editCourse.html", map);
    }

    @WebAction
    public RequestTarget onCreateCourse(RequestCycle cycle) {
        Map<String, Object> map = createMap();
        return new FreemarkerRequestTarget("/course/editCourse.html", map);
    }

}
