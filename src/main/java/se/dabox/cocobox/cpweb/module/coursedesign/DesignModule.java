/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.dabox.cocobox.cpweb.module.coursedesign;

import se.dabox.service.common.coursedesign.techinfo.CpDesignTechInfo;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import net.unixdeveloper.druwa.annotation.DefaultWebAction;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.coursebuilder.initdata.InitData;
import se.dabox.cocobox.coursebuilder.initdata.InitDataBuilder;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.design.EditDesignSettingsForm;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocosite.branding.GetOrgBrandingIdCommand;
import se.dabox.cocosite.coursedesign.CourseDesignThumbnail;
import se.dabox.cocosite.coursedesign.GetCourseDesignBucketCommand;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.upweb.linkaction.LinkActionUrlHelper;
import se.dabox.cocosite.upweb.linkaction.cpreview.CoursePreviewLinkAction;
import se.dabox.cocosite.upweb.linkaction.cpreview.DirectCddSource;
import se.dabox.cocosite.upweb.linkaction.cpreview.PreviewDatabankSource;
import se.dabox.cocosite.upweb.linkaction.cpreview.PreviewParticipationSource;
import se.dabox.cocosite.upweb.linkaction.cpreview.PreviewProjectSource;
import se.dabox.cocosite.user.UserIdentifierHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.coursedesign.BucketCourseDesignInfo;
import se.dabox.service.common.coursedesign.CopyDesignRequest;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.UpdateDesignRequest;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.CourseDesignInfo;
import se.dabox.service.common.coursedesign.v1.CourseDesignXmlMutator;
import se.dabox.service.common.duration.DurationString;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author borg321
 */
@WebModuleMountpoint("/design")
public class DesignModule extends AbstractWebAuthModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(DesignModule.class);

    public static final String OVERVIEW_ACTION = "overview";

    @DefaultWebAction
    @WebAction
    public RequestTarget onOverview(RequestCycle cycle, String strOrgId, String designId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_COURSEDESIGN);

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(designId));

        if (bcd == null) {
            LOGGER.warn("Course design {} (in org {}) doesn't exist", designId, strOrgId);
            return new RedirectUrlRequestTarget(NavigationUtil.toDesignListPageUrl(cycle, strOrgId));
        }

        Map<String, Object> map = createMap();

        map.put("org", org);
        map.put("design", bcd.getDesign());
        map.put("info", bcd.getInfo());
        map.put("designId", designId);
        map.put("usernameHelper", new UserIdentifierHelper(cycle));
        map.put("expiration", getExpirationDays(bcd));
        map.put("thumbnail", new CourseDesignThumbnail(cycle, bcd.getDesign().getDesignId()).get());

        return new FreemarkerRequestTarget("/design/designOverview.html", map);
    }

    @WebAction
    public RequestTarget onEdit(RequestCycle cycle, String strOrgId, String strDesignId,
            String strCopy) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        boolean copyMode = !StringUtils.isEmpty(strCopy);

        if (copyMode) {
            checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_COPY_COURSEDESIGN);
        } else {
            checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_COURSEDESIGN);
        }

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(strDesignId));

        if (bcd == null || (bcd.getInfo().isSticky() && !copyMode)) {
            return toListPage(cycle, strOrgId);
        }

        EditDesignSettingsForm form = makeFrom(bcd);

        DruwaFormValidationSession<EditDesignSettingsForm> formsess = getValidationSession(
                EditDesignSettingsForm.class, cycle);
        formsess.populateFromObject(form);

        Map<String, Object> map = createMap();

        map.put("formsess", formsess);
        map.put("org", org);
        map.put("design", bcd.getDesign());
        map.put("info", bcd.getInfo());
        map.put("copyMode", copyMode);

        String formLink;

        if (copyMode) {
            formLink = cycle.urlFor(DesignModule.class, "editSave", strOrgId, strDesignId, "t");
        } else {
            formLink = cycle.urlFor(DesignModule.class, "editSave", strOrgId, strDesignId);
        }

        map.put("formLink", formLink);

        return new FreemarkerRequestTarget("/design/editDesignSettings.html", map);
    }

    @WebAction
    public RequestTarget onEditSave(RequestCycle cycle, String strOrgId, String strDesignId,
            String strCopy) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        boolean copyMode = !StringUtils.isEmpty(strCopy);
        if (copyMode) {
            checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_COPY_COURSEDESIGN);
        } else {
            checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_COURSEDESIGN);
        }

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(strDesignId));

        DruwaFormValidationSession<EditDesignSettingsForm> formsess = getValidationSession(
                EditDesignSettingsForm.class, cycle);

        if (!formsess.process()) {
            return toEditPage(copyMode, strOrgId, strDesignId, strCopy);
        }

        if (copyMode) {
            return copyDesign(cycle, bcd, formsess, strOrgId, org.getId());
        } else {
            return saveDesign(cycle, bcd, formsess, strOrgId);
        }
    }

    @WebAction
    public RequestTarget onDelete(RequestCycle cycle, String strOrgId, String strDesignId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_DELETE_COURSEDESIGN);

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(strDesignId));

        if (bcd == null || bcd.getInfo().isSticky()) {
            return toListPage(cycle, strOrgId);
        }

        long userId = LoginUserAccountHelper.getUserId(cycle);

        getCourseDesignClient(cycle).removeDesign(userId, bcd.getInfo().getDesignId());

        return toListPage(cycle, strOrgId);
    }

    @WebAction
    public RequestTarget onEditDesign(RequestCycle cycle, String strOrgId, String strDesignId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_COURSEDESIGN);

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(strDesignId));

        if (bcd == null || bcd.getInfo().isSticky()) {
            return toListPage(cycle, strOrgId);
        }

        String backUrl = NavigationUtil.
                toDesignPageUrl(cycle, strOrgId, bcd.getInfo().getDesignId());

        long brandingId = new GetOrgBrandingIdCommand(cycle).forOrg(org.getId());

        InitData id = new InitDataBuilder().setOrgGenericInitData(org.getId(),
                brandingId,
                bcd.getDesign().getName(),
                bcd.getInfo().getDesignId(),
                backUrl).createInitData();

        return new RedirectUrlRequestTarget(GotoDesignBuilder.process(cycle, id));
    }

    @WebAction
    public RequestTarget onView(RequestCycle cycle, String strOrgId, String strDesignId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_COURSEDESIGN);

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(strDesignId));

        if (bcd == null) {
            return toListPage(cycle, strOrgId);
        }

        String backUrl = NavigationUtil.
                toDesignPageUrl(cycle, strOrgId, bcd.getInfo().getDesignId());

        long brandingId = new GetOrgBrandingIdCommand(cycle).forOrg(org.getId());

        InitData id = new InitDataBuilder().setOrgGenericInitData(org.getId(),
                brandingId,
                bcd.getDesign().getName(),
                bcd.getInfo().getDesignId(),
                backUrl).setReadOnly(true).createInitData();

        return new RedirectUrlRequestTarget(GotoDesignBuilder.process(cycle, id));
    }

    @WebAction
    public RequestTarget onPreview(RequestCycle cycle, String strOrgId, String strDesignId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_COURSEDESIGN);

        BucketCourseDesign bcd = getOrgCourseDesign(cycle, org.getId(), Long.valueOf(strDesignId));

        if (bcd == null) {
            return toListPage(cycle, strOrgId);
        }

        CoursePreviewLinkAction preview = new CoursePreviewLinkAction();
        preview.setCddSource(new DirectCddSource(bcd.getDesign().getDesignId()));
        preview.setParticipationSource(new PreviewParticipationSource());
        preview.setProjectSource(new PreviewProjectSource(org.getId()));
        preview.setDatabankSource(new PreviewDatabankSource());

        String url = LinkActionUrlHelper.getUrl(cycle, preview);

        return new RedirectUrlRequestTarget(url);
    }

    private BucketCourseDesign getOrgCourseDesign(final RequestCycle cycle, final long orgId,
            final long courseDesignId) {

        CourseDesignClient cdClient = getCourseDesignClient(cycle);

        CourseDesign design = cdClient.getDesign(courseDesignId);
        if (design == null) {
            return null;
        }

        long bucketId = new GetCourseDesignBucketCommand(cycle).forOrg(orgId);

        List<BucketCourseDesignInfo> infos = cdClient.listDesigns(bucketId);

        if (infos == null) {
            return null;
        }

        for (BucketCourseDesignInfo info : infos) {
            if (info.getDesignId() == courseDesignId) {
                return new BucketCourseDesign(design, info);
            }
        }

        return null;
    }

    private CourseDesignClient getCourseDesignClient(RequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

    private EditDesignSettingsForm makeFrom(BucketCourseDesign bcd) {
        EditDesignSettingsForm esf = new EditDesignSettingsForm();
        esf.setDescription(bcd.getInfo().getDescription());
        esf.setName(bcd.getInfo().getName());

        Integer expirationDays = getExpirationDays(bcd);
        esf.setExpiration(expirationDays);

        return esf;
    }

    protected RequestTarget toEditPage(boolean copyMode, String strOrgId, String strDesignId,
            String strCopy) {
        if (copyMode) {
            return new WebModuleRedirectRequestTarget(DesignModule.class, "edit", strOrgId,
                    strDesignId, strCopy);
        } else {
            return new WebModuleRedirectRequestTarget(DesignModule.class, "edit", strOrgId,
                    strDesignId);
        }
    }

    private RequestTarget saveDesign(RequestCycle cycle, BucketCourseDesign bcd,
            DruwaFormValidationSession<EditDesignSettingsForm> formsess, String strOrgId) {
        final BucketCourseDesignInfo info = bcd.getInfo();
        if (info.isSticky()) {
            return toOverviewPage(cycle, strOrgId, info.getDesignId());
        }

        EditDesignSettingsForm form = formsess.getObject();

        String newDesign = updateDesign(cycle, bcd, form);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateDesignRequest updateReq = new UpdateDesignRequest(info.getDesignId(), userId,
                newDesign);
        updateReq.setName(form.getName());
        updateReq.setDescription(StringUtils.trimToEmpty(form.getDescription()));

        getCourseDesignClient(cycle).updateDesign(updateReq);

        return toOverviewPage(cycle, strOrgId, info.getDesignId());
    }

    private RequestTarget copyDesign(RequestCycle cycle, BucketCourseDesign bcd,
            DruwaFormValidationSession<EditDesignSettingsForm> formsess, String strOrgId, long orgId) {
        final BucketCourseDesignInfo info = bcd.getInfo();
        EditDesignSettingsForm form = formsess.getObject();

        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
        UpdateDesignRequest updateReq = new UpdateDesignRequest(info.getDesignId(), caller);
        updateReq.setName(form.getName());
        updateReq.setDescription(StringUtils.trimToEmpty(form.getDescription()));

        String techInfo = CpDesignTechInfo.createOrgTechInfo(orgId);

        CopyDesignRequest copyRequest = new CopyDesignRequest(info.getDesignId(), caller, techInfo);
        final CourseDesignClient cdClient = getCourseDesignClient(cycle);
        long newDesignId = cdClient.copyDesign(copyRequest);

        UpdateDesignRequest udr = new UpdateDesignRequest(newDesignId, caller);
        udr.setName(form.getName());
        udr.setDescription(form.getDescription());

        cdClient.updateDesign(udr);

        long bucketId = new GetCourseDesignBucketCommand(cycle).forOrg(orgId);

        cdClient.addBucketDesign(caller, bucketId, newDesignId);

        BucketCourseDesign newBcd = getOrgCourseDesign(cycle, orgId, newDesignId);
        String newXml = updateDesign(cycle, newBcd, form);
        getCourseDesignClient(cycle).updateDesign(new UpdateDesignRequest(newDesignId, caller,
                newXml));

        return toOverviewPage(cycle, strOrgId, newDesignId);
    }

    private RequestTarget toOverviewPage(RequestCycle cycle, String strOrgId, long designId) {
        return new RedirectUrlRequestTarget(NavigationUtil.
                toDesignPageUrl(cycle, strOrgId, designId));
    }

    private RequestTarget toListPage(RequestCycle cycle, String strOrgId) {
        return new RedirectUrlRequestTarget(NavigationUtil.
                toDesignListPageUrl(cycle, strOrgId));
    }

    private String updateDesign(RequestCycle cycle, BucketCourseDesign bcd,
            EditDesignSettingsForm form) {
        CourseDesignDefinition cdd = CddCodec.decode(cycle, bcd.getDesign().getDesign());
        CourseDesignXmlMutator mutator = new CourseDesignXmlMutator(bcd.getDesign().getDesign());

        DurationString newExpiration = formToDurationString(form);

        CourseDesignInfo oldInfo = cdd.getInfo();
        CourseDesignInfo newInfo = new CourseDesignInfo(oldInfo.getUserTitle(), oldInfo.
                getUserDescription(), oldInfo.getThumbnailCrl(), newExpiration);
        mutator.setInfo(newInfo);

        return mutator.toXmlString();
    }

    private DurationString formToDurationString(EditDesignSettingsForm form) {
        DurationString newExpiration
                = form.getExpiration() == null ? null : DurationString.valueOf(form.getExpiration()
                                + "D");
        return newExpiration;
    }

    private Integer getExpirationDays(BucketCourseDesign bcd) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();

        CourseDesignDefinition cdd = CddCodec.decode(cycle, bcd.getDesign().getDesign());

        final DurationString exp = cdd.getInfo().getDefaultParticipationExpiration();

        if (exp == null) {
            return null;
        }

        return exp.getDays();
    }
}
