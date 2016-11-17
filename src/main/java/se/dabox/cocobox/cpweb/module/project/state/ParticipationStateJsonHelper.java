package se.dabox.cocobox.cpweb.module.project.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Helper to read/write participation state data in json format. Type T is a class that can be serialized by Jackson.
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class ParticipationStateJsonHelper<T> {
    private static final ObjectMapper MAPPER = new DwsJacksonObjectMapperFactory().newInstance();

    private final Class<T> typeParameterClass;
    private final RequestCycle cycle;
    private final String prefix;
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParticipationStateJsonHelper.class);

    /**
     * Create an instance that works with a specific cycle and prefix.
     *
     * The extra typeParameterClass parameter is needed to work around type erasure problem for object (de-)serialization.
     *
     * @param typeParameterClass Class of type.
     * @param cycle Cycle.
     * @param prefix Prefix of state data. Should include the trailing dot (e.g. "event.").
     */
    public ParticipationStateJsonHelper(Class<T> typeParameterClass, RequestCycle cycle, String prefix) {
        this.typeParameterClass = typeParameterClass;
        this.cycle = cycle;
        this.prefix = prefix;
    }

    /**
     * Return all state data with prefix for one participation.
     *
     * @param participationId Id of participation
     * @return List of objects of type T
     */
    public @Nonnull List<T> getParticipationEvents(long participationId) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final ProjectParticipationState state = ccbc.getParticipationState(participationId);
        final Map<String, String> stateMap = state.getMap();

        if (stateMap != null) {
            return stateMap.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().startsWith(prefix))
                    .map(e -> fromJson(e.getValue()))
                    .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Return all state data with prefix for multiple participations.
     *
     * @param participationIds
     * @return Map of participation id -> List of objects of type T
     */
    public Map<Long, List<T>> getParticipationEvents(@Nonnull List<Long> participationIds) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final List<ProjectParticipationState> states = ccbc.getParticipationState(participationIds);

        if(states != null) {
            return states.stream()
                    .collect(Collectors.toMap(
                            ProjectParticipationState::getParticipationId,
                            state -> {
                                final Map<String, String> stateMap = state.getMap();

                                if (stateMap != null) {
                                    return stateMap.entrySet().stream()
                                            .filter(e -> e.getKey() != null && e.getKey().startsWith(prefix))
                                            .map(e -> fromJson(e.getValue()))
                                            .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                                            .collect(Collectors.toList());
                                }
                                return Collections.emptyList();
                            }));
        }
        return Collections.emptyMap();
    }

    /**
     * Lookup first matching participation state with matching prefix + id.
     *
     * @param participationId
     * @param id
     * @return Object of type T created from json data.
     */
    public @Nullable T getParticipationState(long participationId, String id) {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectParticipationState state = ccbc.getParticipationState(participationId);

        if(state != null) {
            final Map<String, String> stateMap = state.getMap();

            if (stateMap != null) {
                return stateMap.entrySet().stream()
                        .filter(e -> e.getKey() != null && e.getKey().equals(prefix + id))
                        .map(e -> fromJson(e.getValue()))
                        .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())  // In Java 9: .flatMap(Optional::stream)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    /**
     * Sets participation state data.
     *
     * @param participationId Id of participation.
     * @param id Id to save under. "prefix" will prepended to id for the actual key saved under.
     * @param data Object that will be serialized to json.
     */
    public void setParticipationEvent(long participationId, String id, @Nullable T data) {
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        String field = prefix + id;

        try {
            String st = MAPPER.writeValueAsString(data);
            ccbc.setParticipationState(participationId, field, st);
        } catch(JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize object");
        }
    }

    private Optional<T> fromJson(String jsonString) {
        try {
            final T obj = MAPPER.readValue(jsonString, typeParameterClass);
            return Optional.of(obj);
        } catch(IOException e) {
            return Optional.empty();
        }
    }

    private CocoboxCoordinatorClient getCocoboxCordinatorClient(
            ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }
}
