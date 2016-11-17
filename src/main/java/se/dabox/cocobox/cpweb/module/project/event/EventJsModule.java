/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.event;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectJsModule;
import se.dabox.cocobox.cpweb.module.project.state.ParticipationStateJsonHelper;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.login.client.UserAccount;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/projectevent.json")
public class EventJsModule extends AbstractProjectJsModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EventJsModule.class);
    private static final String PREFIX = "event.";

    @WebAction
    public Map<String, Object> onListEvents(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project, strProjectId, LOGGER);

        final List<ProjectParticipation> participations = ccbc.listProjectParticipations(project.getProjectId());
        if(participations == null) {
            return ImmutableMap.of(
                    "status", "ok",
                    "result", Collections.emptyList());
        }
        final List<Long> userIds = participations.stream().map(p -> p.getUserId()).collect(Collectors.toList());
        final Map<Long, UserAccount> uaMap = getUserAccountMap(cycle, userIds);

        // Hashed on participationId
        final Map<Long, List<ParticipationEvent>> participationEvents =
                new ParticipationStateJsonHelper<ParticipationEvent>(ParticipationEvent.class, cycle, PREFIX).getParticipationEvents(participations.stream()
                        .map(ProjectParticipation::getParticipationId).collect(Collectors.toList()));

        final CourseDesign design = getCourseDesignClient(cycle).getDesign(project.getDesignId());
        if(design == null) {
            return ImmutableMap.of(
                    "status", "ok",
                    "result", Collections.emptyList());
        }

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

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
                                        .put("displayName", ua.getDisplayName() != null ? ua.getDisplayName() : "(name not set)")
                                        .put("email", ua.getPrimaryEmail() != null ? ua.getPrimaryEmail() : "(email not set)")
                                        .put("participationId", participation.getParticipationId());
                                final List<ParticipationEvent> pes = participationEvents.getOrDefault(participation.getParticipationId(), Collections.emptyList());
                                final ParticipationEvent participationEvent = pes.stream().filter(pe -> cidStr.equals(pe.getCid()))
                                        .findFirst().orElse(null);
                                if(participationEvent != null) {
                                    partB.put("participationEvent", participationEvent);
                                } else {
                                    partB.put("participationEvent", new ParticipationEvent(cidStr, null, null, null));
                                }
                                return partB.build();
                            }).collect(Collectors.toList()));
                }).collect(Collectors.toList());

        return ImmutableMap.of(
                "status", "ok",
                "result", events
        );

    }


    @WebAction
    public Map<String, Object> onChangeEventState(RequestCycle cycle) {

        final WebRequest req = cycle.getRequest();
        final String strParticipationId = req.getParameter("participationId");
        final String cid = req.getParameter("cid");
        final String strState = req.getParameter("state");

        if(strParticipationId == null || "".equals(strParticipationId)) {
            throw new IllegalArgumentException("participationId missing");
        }
        if(cid == null || "".equals(cid)) {
            throw new IllegalArgumentException("cid missing");
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
        long partId = Long.valueOf(strParticipationId);
        final ProjectParticipation participation = ccbc.getProjectParticipation(partId);

        OrgProject project = ccbc.getProject(participation.getProjectId());
        checkPermission(cycle, project);

        new ParticipationStateJsonHelper<ParticipationEvent>(ParticipationEvent.class, cycle, PREFIX)
                .setParticipationEvent(
                        cid,
                        new ParticipationEvent(cid, state, new Date(), ParticipationEventChannel.CPWEB),
                        partId);

        return ImmutableMap.of(
                "status", "ok",
                "state", state.toString()
        );
    }
}
