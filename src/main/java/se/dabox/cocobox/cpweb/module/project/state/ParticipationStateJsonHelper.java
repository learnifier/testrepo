package se.dabox.cocobox.cpweb.module.project.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.event.ParticipationEvent;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.common.json.DwsJacksonObjectMapperFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper to read/write participation state data in json format.
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationStateJsonHelper<T> {
    private static final ObjectMapper MAPPER = new DwsJacksonObjectMapperFactory().newInstance();

    Class<T> typeParameterClass;
    private final RequestCycle cycle;
    private final String prefix;
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParticipationStateJsonHelper.class);

    /**
     *
     * @param cycle
     * @param prefix Prefix of state data. Should include the trailing dot (e.g. "event.").
     */
    public ParticipationStateJsonHelper(Class<T> typeParameterClass, RequestCycle cycle, String prefix) {
        this.typeParameterClass = typeParameterClass;
        this.cycle = cycle;
        this.prefix = prefix;
    }

    /**
     *
     * @param cycle
     * @param participationId
     * @return List of ParticipationsEvent .
     */
    public @Nonnull List<T> getParticipationEvents(RequestCycle cycle, long participationId) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final ProjectParticipationState state = ccbc.getParticipationState(participationId);
        final Map<String, String> stateMap = state.getMap();

        if (stateMap != null) {
            final List<T> events = stateMap.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().startsWith(prefix))
                    .map(e -> fromJson(e.getValue()))
                    .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                    .collect(Collectors.toList());
            return events;
        }
        return Collections.emptyList();
    }

    /**
     *
     * @param cycle
     * @param participationIds
     * @return
     */
    public Map<Long, List<T>> getParticipationEvents(RequestCycle cycle, List<Long> participationIds) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final List<ProjectParticipationState> states = ccbc.getParticipationState(participationIds);

        if(states != null) {
            final Map<Long, List<T>> partStateMap = states.stream()
                    .collect(Collectors.toMap(
                            ProjectParticipationState::getParticipationId,
                            state -> {
                                final Map<String, String> stateMap = state.getMap();

                                if (stateMap != null) {
                                    final List<T> events = stateMap.entrySet().stream()
                                            .filter(e -> e.getKey() != null && e.getKey().startsWith(prefix))
                                            .map(e -> fromJson(e.getValue()))
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

    public @Nullable T getParticipationEvent(RequestCycle cycle, long participationId, String cid) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectParticipationState state = ccbc.getParticipationState(participationId);

        if(state != null) {
            final Map<String, String> stateMap = state.getMap();

            if (stateMap != null) {
                return stateMap.entrySet().stream()
                        .filter(e -> e.getKey() != null && e.getKey().equals(prefix + cid))
                        .map(e -> fromJson(e.getValue()))
                        .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private Optional<T> fromJson(String jsonString) {
        try {
            final T obj = MAPPER.readValue(jsonString, typeParameterClass);
            return Optional.of(obj);
        } catch(IOException e) {
            return Optional.empty();
        }
    }

    public void setParticipationEvent(RequestCycle cycle, String id, ParticipationEvent event, long participationId) {
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        String field = prefix + id;

        try {
            String st = MAPPER.writeValueAsString(event);
            ccbc.setParticipationState(participationId, field, st);
        } catch(JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize object");
        }
    }

    private CocoboxCoordinatorClient getCocoboxCordinatorClient(
            ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }
}
