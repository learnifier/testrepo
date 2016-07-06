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
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.session.*;
import se.dabox.service.coursecatalog.client.session.impl.StandardSessionVisibility;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

import java.util.*;

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
        long caller = LoginUserAccountHelper.getUserId(cycle);
        CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(Integer.valueOf(strCourseSessionId));

        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        String pk = cycle.getRequest().getParameter("pk");
        String value = cycle.getRequest().getParameter("value");
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));
        checkSessionPermission(cycle, session);
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
                VisibilityMode visibilityMode = VisibilityMode.valueOf(value);
                final SessionVisibility visibility = session.getVisibility();
                final StandardSessionVisibility newVisibility;
                if(visibility == null) {
                    newVisibility = new StandardSessionVisibility(null, null, visibilityMode);
                } else {
                    newVisibility = new StandardSessionVisibility(visibility.getFrom(), visibility.getTo(), visibilityMode);
                }
                final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setVisibilitySettings(newVisibility).createUpdateSessionRequest();
                ccc.updateSession(usr);
                return null;
            }
        });

        Map<String, String> map = Collections.singletonMap("status", "OK");

        return jsonTarget(map);
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
