package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourseId;
import se.dabox.service.coursecatalog.client.course.list.ListCatalogCourseRequestBuilder;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSession;
import se.dabox.service.coursecatalog.client.session.CatalogCourseSessionId;
import se.dabox.service.coursecatalog.client.session.list.ListCatalogSessionRequestBuilder;
import se.dabox.util.collections.CollectionsUtil;

import java.util.List;

import static se.dabox.cocobox.cpweb.module.project.ProductsHelper.getDesignOrgmats;


/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class CopyProjectCommand {

    private final RequestCycle cycle;
    CopyProjectCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }

    RequestTarget execute(Project project, String newName) {

        final ProjectType type = project.getType();
        if(type != ProjectType.DESIGNED_PROJECT) {
            throw new UnsupportedOperationException("Can only copy projects of type designed");
        }
        final String subtype = project.getSubtype();
        OrgProject orgProj = (OrgProject)project; // How do i check project type?

        final Long designId = orgProj.getDesignId();
        List<String> products = products = ProductsHelper.getDesignProducts(cycle, designId, orgProj.getOrgId());
        List<Long> orgmats = getDesignOrgmats(cycle, designId);


        CreateProjectSessionProcessor processor = new CreateProjectSessionProcessor(orgProj.getOrgId());

        String cancelUrl = null;

        final NewProjectSession nps = new NewProjectSession(type.toString(), orgmats, products, processor,
                cancelUrl,
                designId,
                null);

        Integer courseIdRaw = null;

        final Integer sessionId = orgProj.getCourseSessionId();
        if(sessionId != null) {
            final CatalogCourseSession session = CollectionsUtil.singleItemOrNull(getCourseCatalogClient(cycle)
                    .listSessions(new ListCatalogSessionRequestBuilder()
                            .withId(CatalogCourseSessionId.valueOf(sessionId)).build()));
            CatalogCourseId courseId = session.getCourseId();
            if(courseId != null) {
                courseIdRaw = courseId.getId();
            }
        }

        nps.setCourseId(courseIdRaw);

        final CreateProjectGeneral input = new CreateProjectGeneral(); // TODO: Remove form object from NewProjectSession
        input.setProjectname(newName==null?"zzz":newName);
        input.setCountry(orgProj.getCountry());
        input.setDesign(String.valueOf(orgProj.getDesignId()));
        input.setProjectlang(orgProj.getLocale());
        input.setTimezone(orgProj.getTimezone());
        nps.setCreateProjectGeneral(input);

        nps.storeInSession(cycle.getSession());

        return nps.process(cycle, null);
    }

    public static CourseCatalogClient getCourseCatalogClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseCatalogClient.class);
    }


}
