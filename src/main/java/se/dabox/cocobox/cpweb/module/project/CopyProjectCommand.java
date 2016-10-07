package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.state.NewProjectSession;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.service.common.ccbc.project.ProjectType;

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

        Integer courseId = null;
        nps.setCourseId(courseId);
        final CreateProjectGeneral input = new CreateProjectGeneral(); // TODO: Remove form object
        input.setProjectname(newName);
        input.setCountry(orgProj.getCountry());
        input.setDesign(String.valueOf(orgProj.getDesignId()));
        input.setProjectlang(orgProj.getLocale());
        input.setTimezone(orgProj.getTimezone());

        nps.storeInSession(cycle.getSession());

        return nps.process(cycle, null);
    }


}
