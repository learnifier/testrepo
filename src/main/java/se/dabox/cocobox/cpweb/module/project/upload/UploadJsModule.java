/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.upload;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectJsModule;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.common.json.JsonException;
import se.dabox.service.common.json.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/projectupload.json")
public class UploadJsModule extends AbstractProjectJsModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UploadJsModule.class);

    private static final String UPLOAD_PREFIX = "fileupload.";

    @WebAction
    public RequestTarget onListUploads(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project, strProjectId, LOGGER);

        final List<ProjectParticipation> participations = ccbc.listProjectParticipations(project.getProjectId());


        List<Map<String, ?>> res = new ArrayList<>();


        if(participations != null) {
            participations.stream().forEach(p -> {
                Map<String, Object> user = new HashMap<>();
                user.put("participantId", p.getParticipationId());
                user.put("participantName", "Kalle Kula");
                List<Map<String, Object>> uploads = new ArrayList<>();
                final ProjectParticipationState state = ccbc.getParticipationState(p.getParticipationId());
                if(state != null) {
                    final Map<String, String> stateMap = state.getMap();

                    if(stateMap != null) {
                        stateMap.entrySet().stream()
                                .filter(e ->
                                        e.getKey() != null && e.getKey().startsWith(UPLOAD_PREFIX))
                                .forEach(e -> {
                                    try {
                                        final Map<String, ?> json = JsonUtils.decode(e.getValue());
                                        if(json != null && json.containsKey("cid") && json.containsKey("fileName") && json.containsKey("crl")){
                                            final ImmutableMap.Builder<String, Object> uploadBuilder = ImmutableMap.builder();
                                            uploadBuilder.put("cid", json.get("cid"));
                                            uploadBuilder.put("fileName", json.get("fileName"));
                                            uploadBuilder.put("crl", json.get("crl"));
                                            if(json.containsKey("comment")) {
                                                uploadBuilder.put("comment", json.get("comment"));
                                            }
                                            uploads.add(uploadBuilder.build());
                                        }
                                    } catch(JsonException ex) {
                                        LOGGER.warn("Ignoring malformed JSON: ", e.getValue());
                                    }
                                });
                    }
                }
                user.put("uploads", uploads);
                res.add(user);
            });
        }
        return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                "status", "ok",
                "result", res)));
    }

}
