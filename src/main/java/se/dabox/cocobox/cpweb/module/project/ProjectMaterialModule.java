/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.project.AddMaterialForm;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailure;
import se.dabox.cocobox.cpweb.module.project.error.ProjectProductFailureFactory;
import se.dabox.cocobox.crisp.runtime.CrispException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.DeniedException;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.ccbc.project.AllocatedCreditsProjectProductException;
import se.dabox.service.common.ccbc.project.InDesignProjectProductException;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectProductException;
import se.dabox.service.common.ccbc.project.material.CanDeleteProjectProductResponse;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.proddir.material.ProductMaterialConstants;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.material")
public class ProjectMaterialModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectMaterialModule.class);

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onAddMaterial(final RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        DruwaFormValidationSession<AddMaterialForm> formsess =
                getValidationSession(AddMaterialForm.class, cycle);

        if (!formsess.process()) {
            return NavigationUtil.toProjectMaterialPage(cycle, prjId);
        }

        String materialId = formsess.getObject().getMaterialId();

        if (materialId == null) {
            return NavigationUtil.toProjectMaterialPage(cycle, prjId);
        }

        String[] splitId = materialId.split("\\|", 2);

        String type = splitId[0];
        String id = splitId[1];
        switch (type) {
            case ProductMaterialConstants.NATIVE_SYSTEM:
                return onAddProjectProduct(cycle, formsess, prj, id);
            case OrgMaterialConstants.NATIVE_SYSTEM:
                return onAddProjectOrgmat(cycle, prj, id);
        }

        throw new IllegalStateException("Unable to handle type: " + type);
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onRemoveMaterial(final RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        String materialId = StringUtils.trimToNull(cycle.getRequest().getParameter("materialId"));

        if (materialId == null) {
            return NavigationUtil.toProjectMaterialPage(cycle, prjId);
        }

        String[] splitId = materialId.split("\\|", 2);

        String type = splitId[0];
        String id = splitId[1];
        switch (type) {
            case ProductMaterialConstants.NATIVE_SYSTEM:
                return onRemoveProjectProduct(cycle, prj, id);
            case OrgMaterialConstants.NATIVE_SYSTEM:
                return onRemoveOrgmat(cycle, prj, id);
        }

        throw new IllegalStateException("Unable to handle type: " + type);
    }

    private RequestTarget onRemoveProjectProduct(final RequestCycle cycle,
            OrgProject prj,
            String productId) {

        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);

        try {
            CanDeleteProjectProductResponse canResp = pmcClient.canDeleteProjectProduct(prj.
                    getProjectId(), productId);

            if (!canResp.isDeletePossible()) {
                return jsonTarget(Collections.singletonMap("status", "error.candenied"));
            }
        } catch(NotFoundException nfe) {
            LOGGER.debug("Project {} or product {} doesn't exist. Can delete check ignored",
                    prj.getProjectId(),
                    productId);
        }

        try {
            pmcClient.deleteProjectProduct(prj.getProjectId(), productId);
        } catch (InDesignProjectProductException ex) {
            return jsonTarget(Collections.singletonMap("status", "error.indesign"));
        } catch (AllocatedCreditsProjectProductException ex) {
            return jsonTarget(Collections.singletonMap("status", "error.allocatedcredits"));
        } catch (ProjectProductException ex) {
            return jsonTarget(Collections.singletonMap("status", "error"));
        }


        Map<String, String> map = new HashMap<>();
        map.put("status", "OK");
        map.put("location", NavigationUtil.toProjectMaterialPageUrl(cycle, prj.getProjectId()));

        return jsonTarget(map);
    }

    private RequestTarget onRemoveOrgmat(RequestCycle cycle, OrgProject prj, String id) {
        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);

        long orgMatId = Long.valueOf(id);

        try {
            pmcClient.deleteProjectOrgMaterial(prj.getProjectId(), orgMatId);
        } catch (InDesignProjectProductException ex) {
            return jsonTarget(Collections.singletonMap("status", "error.indesign"));
        } catch(NotFoundException nfe) {
            //Ignore if this happens
        }

        Map<String, String> map = new HashMap<>();
        map.put("status", "OK");
        map.put("location", NavigationUtil.toProjectMaterialPageUrl(cycle, prj.getProjectId()));

        return jsonTarget(map);
    }

    private RequestTarget onAddProjectProduct(final RequestCycle cycle,
            DruwaFormValidationSession<AddMaterialForm> formsess,
            OrgProject prj,
            String productId) {

        final long prjId = prj.getProjectId();

        try {
            getProjectMaterialCoordinatorClient(cycle).addProjectProduct(prjId, productId);
        } catch (AllocatedCreditsProjectProductException ex) {
            formsess.addError(new ValidationError(ValidationConstraint.CONSISTENCY,
                    "materialId",
                    "allocatedcredits"));

            return NavigationUtil.toProjectMaterialPage(cycle, prjId);
        } catch (ProjectProductException ex) {
            formsess.addError(new ValidationError(ValidationConstraint.CONSISTENCY,
                    "materialId",
                    "error"));
        } catch (DeniedException ex) {
            ValidationError error =
                    new ValidationError(ValidationConstraint.CONSISTENCY,
                    "materialId",
                    "error");

            if (ex.getDisplayMessage() != null) {
                error.setMessage(ex.getDisplayMessage());
            }

            formsess.addError(error);
        } catch(CrispException cex) {
            ProjectProductFailure failure = new ProjectProductFailureFactory(cycle).newFailure(
                    productId, cex);
            cycle.getSession().setFlashAttribute(ProjectProductFailure.VIEWSESSION_NAME,
                    Collections.singletonList(failure));
        }

        return NavigationUtil.toProjectMaterialPage(cycle, prjId);
    }

    private RequestTarget onAddProjectOrgmat(RequestCycle cycle, OrgProject prj, String id) {

        long orgmatId = Long.valueOf(id);

        getProjectMaterialCoordinatorClient(cycle).addProjectOrgMaterial(prj.getProjectId(),
                orgmatId);

        return NavigationUtil.toProjectMaterialPage(cycle, prj.getProjectId());
    }
}
