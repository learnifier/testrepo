package se.dabox.cocobox.cpweb.module.project.event;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.common.json.JsonException;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.util.idgen.ObjectId;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.reflections.util.ConfigurationBuilder.build;
import static se.dabox.service.common.coursedesign.v1.ExternalResourceReferenceType.crl;

/**
 * Helper class to read/write event participation data.
 *
 * TODO: I intend to make this more generic, so it can be used for all participation state data that is saved in json format.
 *
 * TODO: Should perhaps add functions to return data mapped on cid.
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationEventHelper {

    private static final String EVENT_PREFIX = "event.";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EventJsModule.class);

    public static List<ParticipationEvent> getParticipationEvents(RequestCycle cycle, long participationId) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final ProjectParticipationState state = ccbc.getParticipationState(participationId);
        final Map<String, String> stateMap = state.getMap();

        if (stateMap != null) {
            final List<ParticipationEvent> events = stateMap.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().startsWith(EVENT_PREFIX))
                    .map(e -> fromJson(e.getKey(), e.getValue()))
                    .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                    .collect(Collectors.toList());
            return events;
        }
        return Collections.emptyList();
    }

    public static Map<Long, List<ParticipationEvent>> getParticipationEvents(RequestCycle cycle, List<Long> participationIds) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final List<ProjectParticipationState> states = ccbc.getParticipationState(participationIds);

        if(states != null) {
            final Map<Long, List<ParticipationEvent>> partStateMap = states.stream()
                    .collect(Collectors.toMap(
                            ProjectParticipationState::getParticipationId,
                            state -> {
                                final Map<String, String> stateMap = state.getMap();

                                if (stateMap != null) {
                                    final List<ParticipationEvent> events = stateMap.entrySet().stream()
                                            .filter(e -> e.getKey() != null && e.getKey().startsWith(EVENT_PREFIX))
                                            .map(e -> fromJson(e.getKey(), e.getValue()))
                                            .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                                            .collect(Collectors.toList());
                                    return events;
                                }
                                return Collections.emptyList();
                            }));
            return partStateMap;
        }
        return Collections.emptyMap();
    }

    public static ParticipationEvent getParticipationEvent(RequestCycle cycle, long participationId, String cid) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectParticipationState state = ccbc.getParticipationState(participationId);

        if(state != null) {
            final Map<String, String> stateMap = state.getMap();

            if (stateMap != null) {
                return stateMap.entrySet().stream()
                        .filter(e -> e.getKey() != null && e.getKey().equals(EVENT_PREFIX + cid))
                        .map(e -> fromJson(e.getKey(), e.getValue()))
                        .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private static Optional<ParticipationEvent> fromJson(String stateName, String jsonString) {
        // TODO: Should use json annotation somehow to create ParticipationEvent objects.
        final String cid = stateName.substring(EVENT_PREFIX.length()); // event.{cid}
        try {
            final Map<String, ?> json = JsonUtils.decode(jsonString);

            ParticipationEventState state = null;
            if(json.containsKey("state")) {
                try {
                    state = ParticipationEventState.valueOf((String) json.get("state"));
                } catch(IllegalArgumentException|NullPointerException e) {
                    LOGGER.warn("Ignoring event with malformed state data: {}", json.get("state"));
                    return Optional.empty();
                }
            }

            ParticipationEventChannel channel = null;
            if(json.containsKey("channel")) {
                try {
                    channel = ParticipationEventChannel.valueOf((String) json.get("channel"));
                } catch(IllegalArgumentException|NullPointerException e) {
                    LOGGER.warn("Ignoring event with malformed channel data: {}", json.get("channel"));
                    return Optional.empty();
                }
            }

            Date updated = null;
            if(json.containsKey("updated")) {
                try {
                    updated =  new Date((Long)json.get("updated")); // TODO: Will probably change when we decide on da
                } catch(NumberFormatException e) {
                    LOGGER.warn("Ignoring malformed date: {}", json.get("updated"));
                }
            }

            return Optional.of(new ParticipationEvent(cid, state, updated, channel));
        } catch(JsonException e) {
            return Optional.empty();
        }
    }

    public static void setParticipationEvent(RequestCycle cycle, ParticipationEvent event, long participationId) {
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        String field = EVENT_PREFIX + event.getCid();

        final ImmutableMap<String, ? extends Serializable> vals = ImmutableMap.of(
                "cid", event.getCid(),
                "state", event.getState(),
                "channel", event.getChannel(),
                "updated", event.getUpdated());

        ccbc.setParticipationState(participationId, field, JsonUtils.encode(vals));
    }



    private static CocoboxCoordinatorClient getCocoboxCordinatorClient(
            ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }
}
