/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.event;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectJsModule;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.state.ParticipationEvent;
import se.dabox.service.common.ccbc.participation.state.ParticipationEventChannel;
import se.dabox.service.common.ccbc.participation.state.ParticipationEventState;
import se.dabox.service.common.ccbc.participation.state.ParticipationStateJsonHelper;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.login.client.UserAccount;

import java.io.IOException;
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
        final List<Long> userIds = participations.stream().map(ProjectParticipation::getUserId).collect(Collectors.toList());
        final Map<Long, UserAccount> uaMap = getUserAccountMap(cycle, userIds);

        // Hashed on participationId
        final Map<Long, List<ParticipationEvent>> participationEvents =
                new ParticipationStateJsonHelper<>(ParticipationEvent.class, cycle, ParticipationEvent.PREFIX).getParticipationStates(participations.stream()
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
    public Map<String, Object> onChangeEventState(RequestCycle cycle, String strParticipationId) {

        final WebRequest req = cycle.getRequest();
        try {
            final Map<String, ?> json = JsonUtils.decode(IOUtils.toString(req.getReader()));
            final String cid = (String) json.get("cid");
            final String strState = (String) json.get("state");
            if(empty(strParticipationId) || empty(cid) || empty(strState)) {
                throw new RetargetException(new ErrorCodeRequestTarget(400));
            }
            final ParticipationEventState state;
            try {
                state = ParticipationEventState.valueOf(strState);
            } catch(IllegalArgumentException e) {
                throw new RetargetException(new ErrorCodeRequestTarget(400));
            }

            CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
            long partId = Long.valueOf(strParticipationId);
            final ProjectParticipation participation = ccbc.getProjectParticipation(partId);

            OrgProject project = ccbc.getProject(participation.getProjectId());
            checkPermission(cycle, project);

            // TODO: Should verify that cid is in design.
            new ParticipationStateJsonHelper<>(ParticipationEvent.class, cycle, ParticipationEvent.PREFIX)
                    .setParticipationState(
                            partId,
                            cid,
                            new ParticipationEvent(cid, state, new Date(), ParticipationEventChannel.CPWEB));

            return ImmutableMap.of(
                    "status", "ok",
                    "state", state.toString()
            );
        } catch (IOException e) {
            throw new RetargetException(new ErrorCodeRequestTarget(400));
        }
    }

    private boolean empty(String st) {
        return st == null || "".equals(st);
    }

}
