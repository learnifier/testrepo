package se.dabox.cocobox.cpweb.module.project.event;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationEventHelper {

    private static final String EVENT_PREFIX = "event.";

    static List<ParticipationEvent> getParticipationEvents(RequestCycle cycle, long participationId) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final ProjectParticipationState state = ccbc.getParticipationState(participationId);
        final Map<String, String> stateMap = state.getMap();

        if (stateMap != null) {
            final Stream<Map.Entry<String, String>> entryStream = stateMap.entrySet().stream()
                    .filter(e ->
                            e.getKey() != null && e.getKey().startsWith(EVENT_PREFIX));
        }
        return Collections.emptyList();
    }

    public static CocoboxCoordinatorClient getCocoboxCordinatorClient(
            ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }
}
