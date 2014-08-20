/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.List;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.service.webutils.listform.AbstractIteratingListformProcessor;
import se.dabox.service.webutils.listform.ListformContext;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public abstract class AbstractRosterListCommand extends AbstractIteratingListformProcessor<Long>{

    @Override
    protected RequestTarget getRequestTarget(ListformContext context,
            List<Long> values) {
        Long projectId = getProjectId(context);

        return new WebModuleRedirectRequestTarget(ProjectModule.class, "roster",
                Long.toString(projectId));
    }

    protected Long getProjectId(ListformContext context) {
        Long projectId = context.getAttribute("projectId", Long.class);
        return projectId;
    }

}
