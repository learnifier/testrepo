/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.codehaus.jackson.JsonGenerator;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.CpJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.io.RuntimeIOException;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/user.json")
public class UserJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onListUserParticipations(RequestCycle cycle, String strUserId) {
        long userId = Long.valueOf(strUserId);

        List<ProjectParticipation> participations =
                getCocoboxCordinatorClient(cycle).listProjectParticipationsForUserId(userId);

        return jsonTarget(toJsonResponse(cycle, participations));
    }

    private ByteArrayOutputStream toJsonResponse(
            RequestCycle cycle,
            List<ProjectParticipation> participations) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                CpJsonModule.DEFAULT_BYTE_SIZE);

        LazyProjectName projName = new LazyProjectName(getCocoboxCordinatorClient(cycle));

        try {
            try (JsonGenerator generator
                    = CpJsonModule.FACTORY.createJsonGenerator(
                            baos)) {
                generator.writeStartObject();
                
                generator.writeArrayFieldStart("aaData");
                
                for (ProjectParticipation ppart : participations) {
                    generator.writeStartObject();
                    generator.writeNumberField("id", ppart.getParticipationId());
                    generator.writeStringField("projectname", projName.forProject(ppart.getProjectId()));
                    generator.writeStringField("projectlink", NavigationUtil.toProjectPageUrl(cycle,
                            ppart.getProjectId()));

                    generator.writeEndObject();
                }

                generator.writeEndArray();

                generator.writeEndObject();
            }

            return baos;
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to encode JSON", ex);
        }
    }

}
