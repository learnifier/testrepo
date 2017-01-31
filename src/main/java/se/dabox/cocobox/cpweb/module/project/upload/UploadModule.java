/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.upload;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.state.ParticipationStateJsonHelper;
import se.dabox.service.common.ccbc.participation.state.ParticipationUpload;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.watermark.client.WatermarkClient;
import se.dabox.service.watermark.client.WatermarkRequest;
import se.dabox.service.watermark.client.WatermarkResponse;
import se.dabox.service.watermark.client.ZeroWatermarkRequestFactory;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/projectupload.json")
public class UploadModule extends AbstractProjectWebModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UploadModule.class);


    @WebAction
    public RequestTarget onDownload(RequestCycle cycle, String strParticipationId, String uploadId) {

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        final ProjectParticipation participation = ccbc.getProjectParticipation(Long.valueOf(strParticipationId));

        OrgProject project = ccbc.getProject(participation.getProjectId());
        checkPermission(cycle, project);

        final String rawId = uploadId.substring(ParticipationUpload.PREFIX.length());

        final ParticipationUpload upload =
                new ParticipationStateJsonHelper<>(ParticipationUpload.class, cycle, ParticipationUpload.PREFIX).getParticipationState(participation.getParticipationId(), rawId);

        String crl = upload.getCrl();
        String filename = upload.getFilename();

        String url = getNoWatermarkDownloadUrl(cycle, crl, filename);

        return new RedirectUrlRequestTarget(url);
    }

    private static String getNoWatermarkDownloadUrl(RequestCycle cycle, String crurl,
                                                    String filename) {

        final long caller = LoginUserAccountHelper.getUserId(cycle);
        WatermarkRequest req =
                ZeroWatermarkRequestFactory.createRequest(caller, crurl, filename);

        WatermarkResponse resp =
                CacheClients.getClient(cycle, WatermarkClient.class).createWatermark(req);
        return resp.getUrl();
    }
}
