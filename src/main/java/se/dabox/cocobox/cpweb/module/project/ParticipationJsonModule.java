/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Date;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.partdetails.ParticipationDetailsCommand;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.cocosite.date.DateFormatters;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.proddir.ProductFetchUtil;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductUtils;
import se.dabox.service.webutils.json.JsonErrorMessageException;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.jspart")
public class ParticipationJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onParticipationDetails(RequestCycle cycle, String strParticipationId) {
        
        long partId = Long.valueOf(strParticipationId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        ProjectParticipation participation =
                ccbc.getProjectParticipation(partId);

        OrgProject project = ccbc.getProject(participation.getProjectId());
        checkPermission(cycle, project);

        try {
            return new ParticipationDetailsCommand(cycle).
                    refreshCrispInformation(true).
                    forParticipation(project, participation);
        } catch (CrispException cex) {
            handleCrispException(cycle, cex);

            //Never happens
            return null;
        }
    }

    private void handleCrispException(RequestCycle cycle, CrispException cex) {
        StringBuilder sb = new StringBuilder(128);

        sb.append("An unexpected error occured when trying to get participant status from integration partner.\n\nPlease try again later or contact support.\n\n");
        sb.append("Product: ").append(getProductInfo(cycle, cex)).append("\n");
        sb.append('\n');
        sb.append(cex.getMessage());
        sb.append("\n\nTime: ").append(DateFormatters.JQUERYAGO_FORMAT.format(new Date())).
                append("\n\n");


        throw new JsonErrorMessageException("Communication error", sb.toString());

    }

    private CharSequence getProductInfo(RequestCycle cycle, CrispException cex) {
        if (cex.getProductId() == null) {
            return "(Unknown)";
        }

        Product product
                = ProductFetchUtil.getProduct(getProductDirectoryClient(cycle), cex.getProductId());

        if (product == null) {
            return cex.getProductId();
        }

        String intpartner = ProductUtils.getCrispId(product);

        return product.getTitle() + " (" + cex.getProductId() + ") (Integration partner: "
                + intpartner + ')';
    }

}
