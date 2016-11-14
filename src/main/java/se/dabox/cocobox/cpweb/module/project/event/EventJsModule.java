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
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.login.client.UserAccount;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    public RequestTarget onListEvents(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project, strProjectId, LOGGER);

        final List<ProjectParticipation> participations = ccbc.listProjectParticipations(project.getProjectId());
        final List<Map<String, Object>> eventPart;
        if(participations == null) {
            eventPart = Collections.emptyList();
        } else {
            final List<Long> userIds = participations.stream().map(p -> p.getUserId()).collect(Collectors.toList());
            final Map<Long, UserAccount> uaMap = getUserAccountMap(cycle, userIds);

            // TODO: The eventPart array need to be remade per event once we have the event participation info available.
            eventPart = participations.stream()
                    .filter(p -> uaMap.containsKey(p.getUserId()))
                    .map(p -> {
                        final UserAccount ua = uaMap.get(p.getUserId());
                        return ImmutableMap.<String, Object>of(
                                "userId", ua.getUserId(),
                                "displayName", ua.getDisplayName(),
                                "email", ua.getPrimaryEmail(),
                                "eventState", getState()
                        );
                    })
                    .collect(Collectors.toList());
        }

        final CourseDesign design = getCourseDesignClient(cycle).getDesign(project.getDesignId());
        if(design == null) {
            return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                    "status", "ok",
                    "result", Collections.emptyList())));
        }


        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());
        final List<Map<String, Object>> events = cdd.getAllComponents().stream()
                .filter(c -> c.getType().startsWith("ev_"))
                .map(c ->
                        ImmutableMap.of(
                                "cid", c.getCid(),
                                "title", c.getProperties().getOrDefault("title", "(Unnamed event)"),
                                "participants", eventPart
                        )
                )
                .collect(Collectors.toList());

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
