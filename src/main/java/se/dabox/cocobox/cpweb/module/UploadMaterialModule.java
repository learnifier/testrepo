/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import java.io.IOException;
import java.net.URI;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.unixdeveloper.druwa.FileUpload;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.formdata.MaterialsForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.common.ccbc.material.MutableOrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.material.OrgMaterialConstants;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.contentrepo.util.ContentRepoUri;
import se.dabox.service.contentrepo.util.URINormalizer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/uploadorgmat")
public class UploadMaterialModule extends AbstractWebAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UploadMaterialModule.class);
    private static final Pattern NAME_PATTERN =
            Pattern.compile("([^:/\\\\]+?)$");

    @WebAction(methods=HttpMethod.POST)
    public RequestTarget onNewMaterialPost(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo miniOrg = secureGetMiniOrg(cycle, strOrgId);

        DruwaFormValidationSession<MaterialsForm> formsess =
                getValidationSession(MaterialsForm.class, cycle);
        
        //Cheating a little
        if ("link".equals(cycle.getRequest().getParameter("type"))) {
            formsess.getMutableFieldDescription("link", true).setOptional(false);
        } else {
            formsess.getMutableFieldDescription("file", true).setOptional(false);
        }

        if (!formsess.process()) {
            return new WebModuleRedirectRequestTarget(CpMainModule.class,
                    "newMaterial", strOrgId);
        }

        String crlink = null;
        String weblink = null;

        if ("link".equals(formsess.getObject().getType())) {
            weblink = formsess.getObject().getLink();
        } else {

            FileUpload upload = formsess.getObject().getFile();
            try {
                String realm = DwsRealmHelper.determineRequestRealm(cycle);
                URI baseUri =
                        ContentRepoUri.createUri(realm,
                        CocoSiteConstants.INTERNAL_CONTENTREPO);
                String name = determineName(upload.getName());
                URI uploadUri = ContentRepoUri.addPath(baseUri, "/orgmat/" + strOrgId
                        + '/'
                        + name);
                uploadUri = URINormalizer.normalizeAndValidate(uploadUri);

                try {
                    getContentRepoClient().upload(uploadUri, upload.
                            getInputStream());
                } catch (IOException ex) {
                    LOGGER.error("Failed to upload to content repository", ex);
                    throw new IllegalStateException("Upload failed ", ex);
                }
                crlink = uploadUri.toString();
            } finally {
                IOUtils.closeQuietly(upload.getInputStream());
            }
        }

        MaterialsForm form = formsess.getObject();

        OrgMaterial material;
        if (crlink != null) {
             material = MutableOrgMaterial.createNewCrMaterial(miniOrg.getId(), form.
                getType(), form.getTitle(), form.
                getDescription(), crlink, 0, form.getLang());
        } else {
            material = MutableOrgMaterial.createNewWeblinkMaterial(miniOrg.getId(), form.
                getType(), form.getTitle(), form.
                getDescription(), crlink, 0, form.getLang());
        }

        getCocoboxCordinatorClient(cycle).newOrgMaterial(material);

        WebModuleRedirectRequestTarget target =
                new WebModuleRedirectRequestTarget(CpMainModule.class,
                "listMaterials", strOrgId);
        target.setAnchor(OrgMaterialConstants.NATIVE_SYSTEM);

        return target;
    }

    private String determineName(String name) {
        if (name == null) {
            return unknownName();
        }

        Matcher matcher = NAME_PATTERN.matcher(name);
        if (!matcher.find()) {
            return unknownName();
        }

        return matcher.group(1);
    }

    private String unknownName() {
        return "unknown-"+new Random().nextInt();
    }

}
