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
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.json.JsonException;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.contentrepo.util.InvalidUriException;
import se.dabox.service.login.client.UserAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    public RequestTarget onListEvents(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project, strProjectId, LOGGER);

        final List<ProjectParticipation> participations = ccbc.listProjectParticipations(project.getProjectId());


        final CourseDesign design = getCourseDesignClient(cycle).getDesign(project.getDesignId());
        if(design == null) {
            return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                    "status", "ok",
                    "result", Collections.emptyList())));
        }

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());
        final List<Component> events = cdd.getAllComponents().stream()
                .filter(c -> c.getType().startsWith("ev_"))
                .collect(Collectors.toList());

        return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                "status", "ok",
                "result", events)));
    }

}
