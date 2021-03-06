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
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocosite.freemarker.util.CdnUtils;

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

        map.put("course", cycle.getRequest().getParameter("course"));
        map.put("org", org);

        return new FreemarkerRequestTarget("/course/listCourses.html", map);
    }

    @WebAction
    public RequestTarget onListAndCreate(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        Map<String, Object> map = createMap();

        map.put("initiateCreate", true);
        map.put("course", cycle.getRequest().getParameter("course"));
        map.put("org", org);

        return new FreemarkerRequestTarget("/course/listCourses.html", map);
    }

    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String courseId) {
        // Json call to get data will handle security check.
        Map<String, Object> map = createMap();
        return new FreemarkerRequestTarget("/project/courseOverview.html", map);
    }


    @WebAction
    public RequestTarget onEditCourse(RequestCycle cycle, String strCourseId) {
        // Json call to get/save data will handle security check.
        Map<String, Object> map = createMap();
        map.put("courseId", strCourseId);
        map.put("defaultImage", CdnUtils.getResourceUrl(CpwebConstants.SESSION_DEFAULT_THUMBNAIL));
        return new FreemarkerRequestTarget("/course/editCourse.html", map);
    }

    @WebAction
    public RequestTarget onCreateCourse(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        Map<String, Object> map = createMap();
        map.put("org", org);
        map.put("defaultImage", CdnUtils.getResourceUrl(CpwebConstants.SESSION_DEFAULT_THUMBNAIL));
        return new FreemarkerRequestTarget("/course/editCourse.html", map);
    }
}
