/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.event;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectJsModule;
import se.dabox.cocobox.cpweb.module.project.roster.ParticipationIdProjectProductDeleteCheck;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.common.mailsender.pmt.Part;
import se.dabox.service.login.client.UserAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.http.client.methods.RequestBuilder.put;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/projectevent.json")
public class EventJsModule extends AbstractProjectJsModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EventJsModule.class);

    @WebAction
    public RequestTarget onListEvents(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project, strProjectId, LOGGER);

        final List<ProjectParticipation> participations = ccbc.listProjectParticipations(project.getProjectId());
        if(participations == null) {
            return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                    "status", "ok",
                    "result", Collections.emptyList())));
        }
        final List<Long> userIds = participations.stream().map(p -> p.getUserId()).collect(Collectors.toList());
        final Map<Long, UserAccount> uaMap = getUserAccountMap(cycle, userIds);

        // Hashed on participationId
        final Map<Long, List<ParticipationEvent>> participationEvents = ParticipationEventHelper.getParticipationEvents(cycle, participations.stream().map(ProjectParticipation::getParticipationId).collect(Collectors.toList()));

        // We now have a map from userId -> UserAccount and map from participationId -> ParticipationEvent.

        final CourseDesign design = getCourseDesignClient(cycle).getDesign(project.getDesignId());
        if(design == null) {
            return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                    "status", "ok",
                    "result", Collections.emptyList())));
        }

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

//        final List<Map<String, Object>> events = cdd.getAllComponents().stream()
//                .filter(component -> component.getType().startsWith("ev_"))
//                .map(component -> {
//                    // Extract participations with this cid from participationEvents.
//                    String cidStr = component.getCid().toString();
//                    List<ParticipationEvent> pes = new ArrayList<>();
//                    participationEvents.entrySet().forEach(e -> {
//                        e.getValue().forEach(es -> {
//                            if (cidStr.equals(es.getCid())) {
//                                pes.add(es);
//                            }
//                        });
//                    });
//                    return ImmutableMap.of(
//                            "cid", component.getCid(),
//                            "title", component.getProperties().getOrDefault("title", "(Unnamed event)"),
//                            "participants", pes
//                    );
//                }).collect(Collectors.toList());

        final List<Map<String, Object>> events = cdd.getAllComponents().stream()
                .filter(component -> component.getType().startsWith("ev_"))
                .map(component -> {
                    // Extract participations with this cid from participationEvents.
                    final String cidStr = component.getCid().toString();
                    return ImmutableMap.of(
                            "cid", cidStr,
                            "title", component.getProperties().getOrDefault("title", "(Unnamed event)"),
                            "participations", participations.stream().map(participation -> {
                                UserAccount ua = uaMap.get(participation.getUserId());
                                final ImmutableMap.Builder<String, Object> partB = ImmutableMap.<String, Object>builder()
                                        .put("userId", ua.getUserId())
                                        .put("displayName", ua.getDisplayName() != null ? ua.getDisplayName() : "(unnamed participant)") // TODO: What do i do when these vals are empty?)
                                        .put("email", ua.getPrimaryEmail() != null ? ua.getPrimaryEmail() : "(email not set)");

                                final List<ParticipationEvent> pes = participationEvents.get(participation);
                                if(pes != null) {
                                    pes.stream()
                                            .filter(pe -> cidStr.equals(pe.getCid()))
                                            .findFirst().ifPresent(pe -> {
                                        partB.put("eventState", pe.getState());
                                        partB.put("channel", pe.getChannel());
                                        partB.put("updated", pe.getUpdated());
                                    });
                                }
                                return partB.build();
                            }).collect(Collectors.toList()));
                }).collect(Collectors.toList());

        return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                "status", "ok",
                "result", events)));
    }


    @WebAction
    public RequestTarget onChangeEventState(RequestCycle cycle) {

        final WebRequest req = cycle.getRequest();
        final String strParticipationId = req.getParameter("participationId");
        final String eventCid = req.getParameter("eventCid");
        final String strState = req.getParameter("state");

        if(strParticipationId == null || "".equals(strParticipationId)) {
            throw new IllegalArgumentException("participationId missing");
        }
        if(eventCid == null || "".equals(eventCid)) {
            throw new IllegalArgumentException("eventCid missing");
        }

        if(strState == null || "".equals(strState)) {
            throw new IllegalArgumentException("state missing");
        }

        final ParticipationEventState state;
        try {
            state = ParticipationEventState.valueOf(strState);
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid state");
        }

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final ProjectParticipation participation = ccbc.getProjectParticipation(Long.valueOf(strParticipationId));

        OrgProject project = ccbc.getProject(participation.getProjectId());
        checkPermission(cycle, project);

        // TODO: Actually change state...

        return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                "status", "ok",
                "state", state.toString()
        )));
    }


    static Random rand = new Random(); // TODO: Just for testing before we implement event participations for real.
    private ParticipationEventState getState() {
        final ParticipationEventState[] vals = ParticipationEventState.values();
        return vals[rand.nextInt(vals.length)];
    }
}
