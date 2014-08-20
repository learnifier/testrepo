/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.roster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractRosterListCommand;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.participation.activation.ActivationException;
import se.dabox.service.webutils.listform.ListformContext;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ActivateParticipant extends AbstractRosterListCommand {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ActivateParticipant.class);

    @Override
    protected void executeSingle(ListformContext context, Long participationId) {
        CocoboxCordinatorClient ccbc =
                CacheClients.getClient(context.getCycle(), CocoboxCordinatorClient.class);
        try {
            final long userId = LoginUserAccountHelper.getUserId(context.getCycle());
            ccbc.activateParticipation(userId, participationId);
        } catch (ActivationException ex) {
            LOGGER.warn("Failed to activate participation: {}. Failures: {}", participationId, ex.getFailures());
        } catch (Exception ex) {
            LOGGER.error("Failed to activate participation: {}", participationId, ex);
        }
    }
}
