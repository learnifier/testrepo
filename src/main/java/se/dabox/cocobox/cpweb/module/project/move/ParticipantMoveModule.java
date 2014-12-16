/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.move;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.project.AbstractProjectWebModule;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.move.ActionType;
import se.dabox.service.common.ccbc.participation.move.MoveError;
import se.dabox.service.common.ccbc.participation.move.MoveParticipationRequest;
import se.dabox.service.common.ccbc.participation.move.MoveParticipationResponse;
import se.dabox.service.common.ccbc.participation.move.ProductAction;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectDetails;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.pmove")
public class ParticipantMoveModule extends AbstractProjectWebModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantMoveModule.class);

    @WebAction
    public RequestTarget onSelectTarget(RequestCycle cycle, String strProjectId,
            String strParticipationId) {

        OrgProject project = getProject(cycle, strProjectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);
        ProjectParticipation part = checkParticipation(cycle, project, strParticipationId);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);
        map.put("participation", part);

        return new FreemarkerRequestTarget("/project/move/selectTarget.html", map);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onVerify(RequestCycle cycle, String strProjectId,
            String strParticipationId) {

        OrgProject project = getProject(cycle, strProjectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);
        ProjectParticipation part = checkParticipation(cycle, project, strParticipationId);

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);

        Long targetProjectId
                = DruwaParamHelper.getLongParam(LOGGER, cycle.getRequest(), "targetProjectId");

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
        
        MoveParticipationRequest mpr = new MoveParticipationRequest(caller, part.
                getParticipationId(), targetProjectId);
        
        MoveParticipationResponse result
                = getCocoboxCordinatorClient(cycle).verifyMoveParticipation(mpr);

        map.put("result", result);
        map.put("deletedList", getDeletedProducts(cycle, result));
        map.put("targetProjectId", targetProjectId);
        map.put("participation", part);

        return new FreemarkerRequestTarget("/project/move/verificationResult.html", map);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onExecute(RequestCycle cycle, String strProjectId,
            String strParticipationId) {

        OrgProject project = getProject(cycle, strProjectId);

        checkPermission(cycle, project);
        checkProjectPermission(cycle, project, CocoboxPermissions.CP_VIEW_PROJECT);
        ProjectParticipation part = checkParticipation(cycle, project, strParticipationId);

        Long targetProjectId
                = DruwaParamHelper.getLongParam(LOGGER, cycle.getRequest(), "targetProjectId");

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);

        MoveParticipationRequest mpr = new MoveParticipationRequest(caller, part.
                getParticipationId(), targetProjectId);

        MoveParticipationResponse result
                = getCocoboxCordinatorClient(cycle).moveParticipation(mpr);

        if (result.getMoveError().equals(MoveError.OK)) {
            return NavigationUtil.toProjectPage(targetProjectId);
        }

        Map<String, Object> map = createMap();
        addCommonMapValues(map, project, cycle);

        map.put("result", result);
        map.put("targetProjectId", targetProjectId);

        return new FreemarkerRequestTarget("/project/move/verificationResult.html", map);
    }


    private ProjectParticipation checkParticipation(RequestCycle cycle, ProjectDetails project,
            String strParticipationId) {

        long participationId = Long.valueOf(strParticipationId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        ProjectParticipation part = ccbc.getProjectParticipation(participationId);

        if (part == null) {
            throw new IllegalStateException("Participation not found " + strParticipationId);
        }

        if (part.getProjectId() != project.getProjectId()) {
            throw new IllegalStateException("Invalid participation: " + strParticipationId);
        }

        return part;
    }

    private List<Material> getDeletedProducts(RequestCycle cycle, MoveParticipationResponse result) {

        List<ProductId> productIds = new ArrayList<>();

        for (ProductAction productAction : result.getProductActionList()) {
            if (productAction.getAction() == ActionType.REMOVE) {
                productIds.add(productAction.getProductId());
            }
        }

        ProductDirectoryClient pdClient = getProductDirectoryClient(cycle);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        MaterialListFactory mlf = new MaterialListFactory(cycle, userLocale);

        List<Product> products = pdClient.getProductsByIds(productIds);

        mlf.addProducts(products);

        return mlf.getList();
    }
}
