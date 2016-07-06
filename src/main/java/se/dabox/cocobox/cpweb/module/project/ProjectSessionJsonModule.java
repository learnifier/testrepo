/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import com.fasterxml.jackson.core.JsonGenerator;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.collections4.map.Flat3Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.formdata.project.SetRegCreditLimitForm;
import se.dabox.cocobox.cpweb.formdata.project.SetRegPasswordForm;
import se.dabox.cocobox.cpweb.module.OrgMaterialJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.publish.IsProjectPublishingCommand;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.project.ProjectPermissionCheck;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocosite.webmessage.WebMessage;
import se.dabox.cocosite.webmessage.WebMessageType;
import se.dabox.cocosite.webmessage.WebMessages;
import se.dabox.dws.client.langservice.LangBundle;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.ListProjectParticipationsRequest;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.autoical.ParticipationCalendarCancellationRequest;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.project.*;
import se.dabox.service.common.ccbc.project.catalog.CatalogMode;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.ccbc.project.update.UpdateProjectRequestBuilder;
import se.dabox.service.common.mailsender.BounceConstants;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplateCodec;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.common.proddir.ProductTypeUtil;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.session.*;
import se.dabox.service.coursecatalog.client.session.impl.StandardSessionVisibility;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequest;
import se.dabox.service.coursecatalog.client.session.update.UpdateSessionRequestBuilder;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductTransformers;
import se.dabox.service.webutils.druwa.FormbeanJsRequestTargetFactory;
import se.dabox.service.webutils.freemarker.text.JavaCocoText;
import se.dabox.service.webutils.freemarker.text.LangServiceClientFactory;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.RecentList;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.corba.se.spi.activation.IIOP_CLEAR_TEXT.value;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/session.json")
public class ProjectSessionJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectSessionJsonModule.class);

    @WebAction
    public RequestTarget onSetCourseSessionVisibility(RequestCycle cycle, String strCourseSessionId) {
        long caller = LoginUserAccountHelper.getUserId(cycle);
        CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(Integer.valueOf(strCourseSessionId));

        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        VisibilityMode visibilityMode = VisibilityMode.valueOf(cycle.getRequest().getParameter("value"));
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));
        checkSessionPermission(cycle, session);
//        final ExtendedCatalogCourseSession extSession = CollectionsUtil.singleItemOrNull(ccc.listExtendedSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));
        final SessionVisibility visibility = session.getVisibility();
        final StandardSessionVisibility newVisibility;
        if(visibility == null) {
            newVisibility = new StandardSessionVisibility(null, null, visibilityMode);
        } else {
            newVisibility = new StandardSessionVisibility(visibility.getFrom(), visibility.getTo(), visibilityMode);
        }
        final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setVisibilitySettings(newVisibility).createUpdateSessionRequest();
        ccc.updateSession(usr);

        Map<String, String> map = Collections.singletonMap("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onSetCourseSessionDescription(RequestCycle cycle, String strCourseSessionId) {
        long caller = LoginUserAccountHelper.getUserId(cycle);
        CatalogCourseSessionId courseSessionId = CatalogCourseSessionId.valueOf(Integer.valueOf(strCourseSessionId));

        final CourseCatalogClient ccc = getCourseCatalogClient(cycle);
        String description = cycle.getRequest().getParameter("value");
        final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(ccc.listSessions(new ListCatalogSessionRequestBuilder().withId(courseSessionId).build()));
        checkSessionPermission(cycle, session);
        final UpdateSessionRequest usr = UpdateSessionRequestBuilder.newBuilder(caller, courseSessionId).setDescription(description).createUpdateSessionRequest();
        ccc.updateSession(usr);

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
