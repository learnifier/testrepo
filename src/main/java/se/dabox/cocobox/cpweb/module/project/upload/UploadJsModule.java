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
import se.dabox.service.login.client.UserAccount;

import java.util.ArrayList;
import java.util.Arrays;
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

        // TODO: List course components to retrieve name of download components.

        // cid => { componentName: "", component: partId => { participantName: name, uploads: [ { crl: "", fileName: "", commment: "" } ] }
        Map<String, Map<String, Object>> uploads = new HashMap<>();

        if(participations != null) {
            participations.forEach(participant -> {
                UserAccount userAccount = getUserAccountServiceClient(cycle).getUserAccount(participant.getUserId());
                if(userAccount == null) {
                    return;
                }
                final ProjectParticipationState state = ccbc.getParticipationState(participant.getParticipationId());
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
                                            String cid = (String)json.get("cid");
                                            final ImmutableMap.Builder<String, Object> uploadBuilder = ImmutableMap.builder();
                                            uploadBuilder.put("fileName", json.get("fileName"));
                                            uploadBuilder.put("crl", json.get("crl"));
                                            if(json.containsKey("comment")) {
                                                uploadBuilder.put("comment", json.get("comment"));
                                            }
                                            final ImmutableMap<String, Object> upload = uploadBuilder.build();

                                            if(!uploads.containsKey(cid)) {
                                                uploads.put(cid, ImmutableMap.of(
                                                        "componentName", "A component name", // TODO: Use proper name
                                                        "component", new HashMap<Long, Map<String, Object>>()
                                                ));

                                            }
                                            final Map<Long, Map<String, Object>> participantMap = (Map<Long, Map<String, Object>>)uploads.get(cid).get("component");

                                            if(!participantMap.containsKey(participant.getUserId())) {
                                                participantMap.put(participant.getUserId(), ImmutableMap.of(
                                                        "participantName", userAccount.getDisplayName(),
                                                        "uploads", new ArrayList<Map<String, Object>>()
                                                ));
                                            }

                                            // TODO: Can I avoid this funky cast?
                                            final List<Map<String, Object>> uploadList = (List<Map<String, Object>>)participantMap.get(participant.getUserId()).get("uploads");
                                            uploadList.add(upload);
                                        }
                                    } catch(JsonException ex) {
                                        LOGGER.warn("Ignoring malformed JSON: ", e.getValue());
                                    }
                                });
                    }
                }
            });
        }
        return new JsonRequestTarget(JsonUtils.encode(ImmutableMap.of(
                "status", "ok",
                "result", uploads)));
    }

}
