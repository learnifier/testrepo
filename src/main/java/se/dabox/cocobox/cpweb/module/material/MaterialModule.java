/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.material;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocobox.cpweb.formdata.material.EditMaterialForm;
import se.dabox.cocobox.cpweb.module.CpMainModule;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.material.MutableOrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.ccbc.material.UpdateOrgMaterialRequest;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author borg321
 */
@WebModuleMountpoint("/material")
public class MaterialModule extends AbstractWebAuthModule {

    @DefaultWebAction
    @WebAction
    public RequestTarget onEdit(RequestCycle cycle, String strOrgId, String strOrgmatId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        Map<String, Object> map = createMap();

        DruwaFormValidationSession<EditMaterialForm> formsess =
                getValidationSession(EditMaterialForm.class, cycle);

        long orgmatId = Long.valueOf(strOrgmatId);
        formsess.populateFromObject(getEditMaterialForm(cycle, orgmatId));

        map.put("formsess", formsess);
        map.put("org", org);
        String formLink = cycle.urlFor(MaterialModule.class, "performEdit", strOrgId,
                strOrgmatId);
        map.put("formLink", formLink);

        return new FreemarkerRequestTarget("/material/editMaterial.html", map);
    }

    @WebAction
    public RequestTarget onPerformEdit(RequestCycle cycle, String strOrgId, String strOrgmatId) {
        checkOrgPermission(cycle, strOrgId);

        DruwaFormValidationSession<EditMaterialForm> formsess =
                getValidationSession(EditMaterialForm.class, cycle);

        if (!formsess.process()) {
            return new WebModuleRedirectRequestTarget(MaterialModule.class, strOrgId, strOrgmatId);
        }

        EditMaterialForm form = formsess.getObject();
        OrgMaterial sourceOrgMat = getOrgMaterial(cycle, Long.valueOf(strOrgmatId));
        
        MutableOrgMaterial orgMat =
                new MutableOrgMaterial(sourceOrgMat);

        orgMat.setDescription(StringUtils.trimToEmpty(form.getDescription()));
        orgMat.setLocale(form.getLang());
        orgMat.setTitle(form.getTitle());
        if ("link".equals(orgMat.getType())) {
            orgMat.setWeblink(form.getLink());
        }

        orgMat.setUpdatedBy(LoginUserAccountHelper.getUserId(cycle));

        UpdateOrgMaterialRequest updateReq = UpdateOrgMaterialRequest.fromOrgMaterial(orgMat);
        getCocoboxCordinatorClient(cycle).updateOrgMaterial(updateReq);

        WebModuleRedirectRequestTarget reqTarget =
                new WebModuleRedirectRequestTarget(CpMainModule.class,
                CpMainModule.LIST_MATERIALS, strOrgId);
        reqTarget.setAnchor(OrgMaterialConstants.NATIVE_SYSTEM);

        return reqTarget;
    }

    private EditMaterialForm getEditMaterialForm(RequestCycle cycle, long orgmatId) {
        OrgMaterial orgmat = getOrgMaterial(cycle, orgmatId);

        EditMaterialForm form = new EditMaterialForm();
        form.setDescription(orgmat.getDescription());
        form.setLang(orgmat.getLocale());
        form.setTitle(orgmat.getTitle());
        form.setLink(orgmat.getWeblink());
        form.setType(orgmat.getType());

        return form;
    }

    private OrgMaterial getOrgMaterial(RequestCycle cycle, long orgmatId) throws IllegalArgumentException {
        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgMaterial orgmat = ccbc.getOrgMaterial(orgmatId);
        if (orgmat == null) {
            throw new IllegalArgumentException("OrgMat missing "+orgmatId);
        }
        return orgmat;
    }

}
