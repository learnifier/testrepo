/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.update.UpdateParticipationRequestBuilder;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.webutils.listform.ListformContext;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class AdjustParticipantExpiration extends AbstractRosterListCommand {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SetParticipantExpiration.class);

    private static final long serialVersionUID = 1L;

    public AdjustParticipantExpiration() {
    }

    @Override
    public RequestTarget execute(ListformContext context, List<Long> values) {
        Long offset = getExpirationDate(context);
        saveOffset(context, offset);

        return super.execute(context, values);
    }

    private void saveOffset(ListformContext context, Long offset) {
        context.setAttribute("expOffset", offset);
    }

    @Override
    protected void executeSingle(ListformContext context, Long value) {
        CocoboxCoordinatorClient ccbc =
                CacheClients.getClient(context.getCycle(), CocoboxCoordinatorClient.class);

        ProjectParticipation part = ccbc.getProjectParticipation(value);

        if (part == null) {
            return;
        }

        long caller = LoginUserAccountHelper.getCurrentCaller(context.getCycle());

        UpdateParticipationRequestBuilder builder
                = new UpdateParticipationRequestBuilder(caller, value);
        builder.setExpiration(calculateNewOffset(context, part));

        ccbc.updateProjectParticipation(builder.buildUpdateParticipationRequest());
    }

    private Long getExpirationDate(ListformContext context) {
        WebRequest req = context.getCycle().getRequest();
        long adjustmentDays = DruwaParamHelper.getMandatoryLongParam(LOGGER, req,
                "expirationadjustment");

        return TimeUnit.DAYS.toMillis(adjustmentDays);
    }

    private Long getOffset(ListformContext context) {
         return context.getAttribute("expOffset", Long.class);
    }

    private Date calculateNewOffset(ListformContext context, ProjectParticipation part) {
        if (part.getExpiration() == null) {
            return new Date(System.currentTimeMillis()+getOffset(context));
        }

        return new Date(part.getExpiration().getTime() + getOffset(context));
    }
}
