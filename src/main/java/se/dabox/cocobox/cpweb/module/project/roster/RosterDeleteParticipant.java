/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.roster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.project.AbstractRosterListCommand;
import se.dabox.cocobox.cpweb.module.project.DeleteFailure;
import se.dabox.cocosite.user.UserIdentifierHelper;
import se.dabox.cocosite.webmessage.WebMessage;
import se.dabox.cocosite.webmessage.WebMessageType;
import se.dabox.cocosite.webmessage.WebMessages;
import se.dabox.dws.client.DwsServiceErrorCodeException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.project.ParticipationToken;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.tokenmanager.client.TokenStatus;
import se.dabox.service.webutils.listform.ListformContext;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class RosterDeleteParticipant extends AbstractRosterListCommand {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RosterDeleteParticipant.class);

    @Override
    public RequestTarget execute(ListformContext context,
            List<Long> values) {

        RequestTarget validationTarget = validateDelete(context, values);

        if (validationTarget != null) {
            return validationTarget;
        }
        
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(context);

        List<DeleteFailure> idFailures =
                new ParticipationIdProjectProductDeleteCheck(
                ccbc,
                getProjectId(context)).
                check(context, values);

        if (idFailures != null) {
            setDeleteFailures(context, idFailures);

            return NavigationUtil.toProjectPage(getProjectId(context));
        }

        RequestTarget retval = super.execute(context, values);

        List<DeleteFailure> failures = getOrCreateDeleteFailures(context);
        if (!failures.isEmpty()) {

            RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();

            String message = String.format(
                    "%d errors occured while trying to delete %d participants", failures.size(),
                    values.size());
            WebMessages.getInstance(cycle).addMessage(WebMessage.createTextMessage(message,
                    WebMessageType.error));

            setDeleteFailures(context, failures);
        } else {
            RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();

            String message;

            if (values.size() == 1) {
                message = "Deleted 1 participant";
            } else {
                message = String.format("Deleted %d participants", values.size());
            }

            WebMessages.getInstance(cycle).addMessage(WebMessage.createTextMessage(message,
                    WebMessageType.success));
        }

        return retval;
    }

    @Override
    protected void executeSingle(ListformContext context, Long value) {
        CocoboxCoordinatorClient ccbcClient =
                getCocoboxCordinatorClient(context);

        long userId = LoginUserAccountHelper.getCurrentCaller(context.getCycle());
        try {
            ccbcClient.deleteProjectParticipant(userId, getProjectId(context), value);
        } catch(NotFoundException nfe) {
            LOGGER.warn("Unable to delete participation {}. Participation not found", value);
        } catch (DwsServiceErrorCodeException secx) {

            if (secx.getErrorCode() != 12) {
                throw secx;
            }

            String name = "";
            ProjectParticipation part = ccbcClient.getProjectParticipation(value);
            if (part != null) {
                final UserIdentifierHelper helper = new UserIdentifierHelper(context.getCycle());
                name = helper.getName(part.getUserId());
                
            }

            getOrCreateDeleteFailures(context).add(new DeleteFailure(value, name,
                    "Failed to deactivate all products for participant"));
        }
    }

    private RequestTarget validateDelete(ListformContext context,
            List<Long> participationIds) {

        CocoboxCoordinatorClient ccbcClient = getCocoboxCordinatorClient(context);

        Set<ProjectParticipation> undeletable = new HashSet<>();

        for (Long participationId : participationIds) {
            if (!isDeletePossible(ccbcClient, participationId)) {
                undeletable.add(ccbcClient.getProjectParticipation(participationId));
            }
        }

        if (undeletable.isEmpty()) {
            return null;
        }

        final UserIdentifierHelper helper = new UserIdentifierHelper(context.getCycle());

        List<DeleteFailure> failures = CollectionsUtil.transformList(undeletable, (ProjectParticipation item) ->
                new DeleteFailure(item.getParticipationId(), helper.getName(item.
                        getUserId()), "Unable to delete participation"));

        setDeleteFailures(context, failures);

        return NavigationUtil.toProjectPage(getProjectId(context));

    }

    private CocoboxCoordinatorClient getCocoboxCordinatorClient(ListformContext context) {
        CocoboxCoordinatorClient ccbcClient =
                context.getAttribute("ccbcClient",
                CocoboxCoordinatorClient.class);
        return ccbcClient;
    }

    private boolean isDeletePossible(CocoboxCoordinatorClient ccbcClient, Long participationId) {

        List<ParticipationToken> tokens = ccbcClient.getParticipationTokens(participationId);
        if (tokens == null || tokens.isEmpty()) {
            return true;
        }

        for (ParticipationToken partToken : tokens) {
            if (!TokenStatus.RESERVED.equals(partToken.getTokenStatus())) {
                return false;
            }
        }

        return true;
    }

    private void setDeleteFailures(ListformContext context,
            List<DeleteFailure> failures) {
        context.getCycle().setAttribute(CpwebConstants.DELETE_FAILURE_FLASH, failures);
        context.getCycle().getSession().setFlashAttribute(CpwebConstants.DELETE_FAILURE_FLASH, failures);
    }

    private List<DeleteFailure> getOrCreateDeleteFailures(ListformContext context) {
        List<DeleteFailure> failures = context.getCycle().getAttribute(
                CpwebConstants.DELETE_FAILURE_FLASH);

        if (failures == null) {
            failures = new ArrayList<>();
            context.getCycle().setAttribute(
                    CpwebConstants.DELETE_FAILURE_FLASH, failures);
        }

        return failures;
    }
}
