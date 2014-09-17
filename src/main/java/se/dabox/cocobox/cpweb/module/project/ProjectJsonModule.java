/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.collections.map.Flat3Map;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.formdata.project.SetRegCreditLimitForm;
import se.dabox.cocobox.cpweb.formdata.project.SetRegPasswordForm;
import se.dabox.cocobox.cpweb.module.OrgMaterialJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.mail.GetOrgMailBucketCommand;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.cocosite.security.project.ProjectPermissionCheck;
import se.dabox.service.common.ccbc.project.material.MaterialListFactory;
import se.dabox.dws.client.langservice.LangBundle;
import se.dabox.service.client.Clients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.ListProjectParticipationsRequest;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.autoical.ParticipationCalendarCancellationRequest;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.project.MailBounce;
import se.dabox.service.common.ccbc.project.MailBounceUtil;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.ccbc.project.ProjectProductTransformers;
import se.dabox.service.common.ccbc.project.ProjectTask;
import se.dabox.service.common.ccbc.project.UpdateProjectRequest;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.mailsender.BounceConstants;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplateServiceClient;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplateCodec;
import se.dabox.service.common.material.Material;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductTransformers;
import se.dabox.service.webutils.druwa.FormbeanJsRequestTargetFactory;
import se.dabox.service.webutils.freemarker.text.LangServiceClientFactory;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.RecentList;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/project.json")
public class ProjectJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectJsonModule.class);

    private static List<Material> getMissingProducts(
            List<String> prodIdList,
            List<Product> existingProductsList) {
        
        Set<String> prodIdSet = new LinkedHashSet<>(prodIdList);
        Set<String> existingIdSet = CollectionsUtil.transform(existingProductsList,
                ProductTransformers.getIdStringTransformer());

        //Remove all existing
        prodIdSet.removeAll(existingIdSet);

        //Now only missing ids are left

        return CollectionsUtil.transformList(prodIdSet, new Transformer<String, Material>() {

            @Override
            public Material transform(String item) {
                return new MissingProductMaterial(item);
            }
        });
    }

    @WebAction
    public RequestTarget onProjectRoster(RequestCycle cycle, String strProjectId)
            throws Exception {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);
        List<UserAccount> users =
                Clients.getClient(cycle, UserAccountService.class).
                getUserGroupAccounts(prj.getUserGroupId());

        List<ProjectParticipation> participations;

        try {
            participations = ccbc.listProjectParticipations(new ListProjectParticipationsRequest(
                    prjId, true));
        } catch (NotFoundException nfe) {
            return new ErrorCodeRequestTarget(404);
        }

        String strOrgId = Long.toString(prj.getOrgId());

        boolean impersonateAllowed = ProjectPermissionCheck.fromCycle(cycle).checkPermission(prj,
                CocoboxPermissions.PRJ_IMPERSONATE_PARTICIPANT);

        return jsonTarget(new ProjectRosterJsonGenerator().toJson(cycle, participations, users,
                strOrgId, prj, impersonateAllowed));
    }

    @WebAction
    public RequestTarget onProjectRosterExcel(RequestCycle cycle, String strProjectId)
            throws Exception {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);
        List<UserAccount> users =
                Clients.getClient(cycle, UserAccountService.class).
                getUserGroupAccounts(prj.getUserGroupId());

        List<ProjectParticipation> participations =
                ccbc.listProjectParticipations(new ListProjectParticipationsRequest(prjId, true));

        return new ExcelRoster(cycle).generate(prj, participations, users);
    }

    @WebAction
    public RequestTarget onParticipationBounceInfo(final RequestCycle cycle, String strPartId) {

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        List<MailBounce> bounces = ccbc.getParticipationMailBounce(Long.valueOf(strPartId));
        List<MailBounce> filteredBounces =
                MailBounceUtil.active(bounces);
        filteredBounces = MailBounceUtil.bounced(filteredBounces);

        if (filteredBounces.size() > 1) {
            filteredBounces = filteredBounces.subList(filteredBounces.size() - 1, filteredBounces.
                    size());
        }

        final MailBounce data = filteredBounces.isEmpty() ? null : filteredBounces.get(0);

        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,
                CocositeUserHelper.getUserLocale(cycle));

        ByteArrayOutputStream stream =
                new JsonEncoding(format) {
                    @Override
                    protected void encodeData(JsonGenerator generator) throws IOException {
                        generator.writeStartObject();
                        if (data == null) {
                            generator.writeEndObject();
                            return;
                        }

                        generator.writeNumberField("bounceId", data.getBounceId());

                        generator.writeNumberField("partId", data.getParticipationId());
                        generator.writeNumberField("orgId", data.getOrgId());

                        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
                        OrgProject prj = ccbc.getProject(data.getProjectId());
                        generator.writeStringField("projectName", prj.getName());

                        writeDateField(generator, "created", data.getCreated());

                        generator.writeStringField(BounceConstants.TECH_MESSAGE, data.
                                getTechMessage());
                        generator.writeStringField(BounceConstants.TECH_MESSAGE_EXTENDED, data.
                                getTechMessageExtended());

                        writeDateField(generator, "bounceDate", data.getCreated());

                        generator.writeEndObject();
                    }
                }.encodeToStream();

        return jsonTarget(stream);
    }

    @WebAction
    public RequestTarget onProjectTasks(RequestCycle cycle, String strProjectId)
            throws Exception {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        List<MailTemplate> mailTemplates = getMailTemplates(cycle, prj.getOrgId());

        List<ProjectTask> tasks = ccbc.listProjectTasks(prjId);

        return jsonTarget(toTaskJson(cycle, tasks, mailTemplates, prj));
    }

    @WebAction
    public RequestTarget onProjectMaterials(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);
        List<Material> materials = getProjectMaterials(pmcClient, prjId, cycle);

        return jsonTarget(OrgMaterialJsonModule.toJsonMaterials(cycle, strProjectId, materials));
    }

    @WebAction
    public RequestTarget onListProjectRoleUsers(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        ListProjectAdminJson json = new ListProjectAdminJson(cycle, ccbc);

        return jsonTarget(json.forProject(prj));
    }

    @WebAction
    public RequestTarget onSetInvitationCreditLimit(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);

        DruwaFormValidationSession<SetRegCreditLimitForm> formsess =
                getValidationSession(SetRegCreditLimitForm.class, cycle);

        //Cheat (because we toggle credits limit)
        boolean isOn = Boolean.valueOf(cycle.getRequest().getParameter("creditLimitEnabled"));

        if (isOn) {
            formsess.getMutableFieldDescription("creditLimit", true).setOptional(false);
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        LangBundle bundle =
                LangServiceClientFactory.getInstance(cycle).getLangBundle(
                CocoSiteConstants.DEFAULT_LANG_BUNDLE, userLocale.toString(), true);
        String prefix = "cpweb.registration.creditlimit";

        if (!formsess.process()) {
            return new FormbeanJsRequestTargetFactory(cycle, bundle, prefix).getRequestTarget(
                    formsess);
        }

        Integer inviteLimit = null;
        if (isOn) {
            inviteLimit = formsess.getObject().getCreditLimit();
        }

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateProjectRequest upr = new UpdateProjectRequest(project.getProjectId(), project.
                getName(), project.getLocale(), userId, project.getCountry(), project.getTimezone(),
                project.getDesignId(), project.getStageDesignId(), project.getMasterDatabank(),
                project.getStageDatabank(), project.getNote(), project.getInvitePassword(),
                inviteLimit, project.isInvitePossible(),
                project.getUserTitle(),
                project.getUserDescription(),
                project.isAutoIcal(),
                project.isSocial());

        ccbc.updateOrgProject(upr);

        return new FormbeanJsRequestTargetFactory(cycle, bundle, prefix).
                getSuccessfulMap(Collections.
                singletonMap("status", "OK"));
    }

    @WebAction
    public RequestTarget onSetInvitationPassword(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);

        DruwaFormValidationSession<SetRegPasswordForm> formsess =
                getValidationSession(SetRegPasswordForm.class, cycle);

        //Cheat (because we toggle credits limit)
        boolean isOn = Boolean.valueOf(cycle.getRequest().getParameter("passwordEnabled"));

        if (isOn) {
            formsess.getMutableFieldDescription("password", true).setOptional(false);
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        LangBundle bundle =
                LangServiceClientFactory.getInstance(cycle).getLangBundle(
                CocoSiteConstants.DEFAULT_LANG_BUNDLE, userLocale.toString(), true);
        String prefix = "cpweb.registration.password";

        if (!formsess.process()) {
            return new FormbeanJsRequestTargetFactory(cycle, bundle, prefix).getRequestTarget(
                    formsess);
        }

        String pw = null;
        if (isOn) {
            pw = formsess.getObject().getPassword();
        }

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateProjectRequest upr = new UpdateProjectRequest(project.getProjectId(), project.
                getName(), project.getLocale(), userId, project.getCountry(), project.getTimezone(),
                project.getDesignId(), project.getStageDesignId(), project.getMasterDatabank(),
                project.getStageDatabank(), project.getNote(), pw,
                project.getInviteLimit(), project.isInvitePossible(),
                project.getUserTitle(),
                project.getUserDescription(),
                project.isAutoIcal(),
                project.isSocial());

        ccbc.updateOrgProject(upr);

        return new FormbeanJsRequestTargetFactory(cycle, bundle, prefix).
                getSuccessfulMap(Collections.
                singletonMap("status", "OK"));
    }

    @WebAction
    public RequestTarget onSetInvitationStatus(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        boolean enabled = Boolean.valueOf(cycle.getRequest().getParameter("status"));

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);

        DruwaFormValidationSession<SetRegPasswordForm> formsess =
                getValidationSession(SetRegPasswordForm.class, cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateProjectRequest upr = new UpdateProjectRequest(project.getProjectId(), project.
                getName(), project.getLocale(), userId, project.getCountry(), project.getTimezone(),
                project.getDesignId(), project.getStageDesignId(), project.getMasterDatabank(),
                project.getStageDatabank(), project.getNote(), project.getInvitePassword(),
                project.getInviteLimit(), enabled,
                project.getUserTitle(),
                project.getUserDescription(),
                project.isAutoIcal(),
                project.isSocial());

        ccbc.updateOrgProject(upr);

        return jsonTarget(Collections.singletonMap("status", "OK"));
    }

    @WebAction
    public RequestTarget onSetAutoIcalStatus(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);

        boolean enabled = Boolean.valueOf(DruwaParamHelper.getMandatoryParam(null, cycle.
                getRequest(), "autoical"));

        boolean sendUpdates = Boolean.valueOf(cycle.getRequest().getParameter("sendUpdates"));


        if (project.isAutoIcal() != enabled) {
            //There was a change

            long userId = LoginUserAccountHelper.getUserId(cycle);

            UpdateProjectRequest upr =
                    new UpdateProjectRequest(
                    project.getProjectId(),
                    project.getName(),
                    project.getLocale(),
                    userId,
                    project.getCountry(),
                    project.getTimezone(),
                    project.getDesignId(),
                    project.getStageDesignId(),
                    project.getMasterDatabank(),
                    project.getStageDatabank(),
                    project.getNote(),
                    project.getInvitePassword(),
                    project.getInviteLimit(),
                    project.isSelfRegistrationEnabled(),
                    project.getUserTitle(),
                    project.getUserDescription(),
                    enabled,
                    project.isSocial());

            ccbc.updateOrgProject(upr);

            if (enabled) {
                reactivateProjectParticipants(cycle, project);
            } else {
                cancelInvitations(cycle, project, sendUpdates);
            }
        }

        String str = String.
                format(
                "Would set the autoical setting on project %d to %s (and sendUpdates: %s) and then return a json status response",
                prjId,
                enabled,
                sendUpdates);

        Map<String, String> map = MapUtil.createHash(2);
        map.put("message", str);
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onSetSocialSetting(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);

        boolean enabled = Boolean.valueOf(DruwaParamHelper.getMandatoryParam(null, cycle.
                getRequest(), "enabled"));

        long userId = LoginUserAccountHelper.getUserId(cycle);

        UpdateProjectRequest upr = new UpdateProjectRequest(
                project.getProjectId(),
                project.getName(),
                project.getLocale(),
                userId,
                project.getCountry(),
                project.getTimezone(),
                project.getDesignId(),
                project.getStageDesignId(),
                project.getMasterDatabank(),
                project.getStageDatabank(),
                project.getNote(),
                project.getInvitePassword(),
                project.getInviteLimit(),
                project.isSelfRegistrationEnabled(),
                project.getUserTitle(),
                project.getUserDescription(),
                project.isAutoIcal(),
                enabled);

        ccbc.updateOrgProject(upr);
        
        Map<String, String> map = Collections.singletonMap("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onListCountries(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        List<Locale> countries =
                NewProjectModule.getProjectCountries(cycle);

        List<Map<String,String>> retval = new ArrayList<>(countries.size());

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        for (Locale locale : countries) {
            @SuppressWarnings("unchecked")
            Map<String,String> map = new Flat3Map();
            map.put("value", locale.toLanguageTag());
            map.put("text", locale.getDisplayCountry(userLocale));
            retval.add(map);
        }

        return jsonTarget(retval);
    }

    @WebAction
    public RequestTarget onListLanguages(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        List<Locale> countries =
                NewProjectModule.getProjectLocales(cycle);

        List<Map<String,String>> retval = new ArrayList<>(countries.size());

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        for (Locale locale : countries) {
            @SuppressWarnings("unchecked")
            Map<String,String> map = new Flat3Map();
            map.put("value", locale.toLanguageTag());
            map.put("text", locale.getDisplayName(userLocale));
            retval.add(map);
        }

        return jsonTarget(retval);
    }

    @WebAction
    public RequestTarget onListTimeZones(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        RecentList<TimeZone> timeZones =
                OrgProjectTimezoneFactory.newRecentList(cycle, prj.getOrgId());

        List<Object> response = new ArrayList<>();

        response.add(timezoneSection("Recently used", timeZones.getRecentList()));
        response.add(timezoneSection("All timezones", timeZones.getOtherList()));

        return jsonTarget(response);
    }

    private byte[] toTaskJson(
            final RequestCycle cycle,
            final List<ProjectTask> task, List<MailTemplate> mailTemplates,
            final OrgProject project) {

        final Map<Long, MailTemplate> templateMap = CollectionsUtil.createMap(mailTemplates,
                new Transformer<MailTemplate, Long>() {
                    @Override
                    public Long transform(MailTemplate obj) {
                        return obj.getId();
                    }
                });

        final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.MEDIUM,
                CocositeUserHelper.getUserLocale(cycle));
        df.setTimeZone(project.getTimezone());

        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();

                generator.writeArrayFieldStart("aaData");

                for (ProjectTask projectTask : task) {
                    generator.writeStartObject();

                    generator.writeNumberField("taskId", projectTask.getTaskId());
                    generator.writeStringField("status", projectTask.getStatus());
                    generator.writeStringField("type", projectTask.getType());
                    generator.writeNumberField("scheduled", projectTask.
                            getScheduled().getTime());
                    generator.writeStringField("scheduledString",
                            df.format(projectTask.getScheduled()));
                    if (projectTask.getMailTemplateId() != null) {
                        generator.writeNumberField("mailTemplateId", projectTask.
                                getMailTemplateId());
                    }
                    generator.writeStringField("mailTemplateName", getMailTemplateName(projectTask));
                    generator.writeStringField("targetFilter", projectTask.
                            getTargetFilter());
                    generator.writeNumberField("createdBy", projectTask.
                            getCreatedBy());
                    String editLink = cycle.urlFor(
                            ProjectModificationModule.class,
                            ProjectModificationModule.EDIT_TASK,
                            Long.toString(project.getProjectId()),
                            Long.toString(projectTask.getTaskId()));
                    generator.writeStringField("editLink", editLink);


                    generator.writeEndObject();
                }

                generator.writeEndArray();

                generator.writeEndObject();
            }

            private String getMailTemplateName(ProjectTask projectTask) {
                MailTemplate template = templateMap.get(projectTask.getMailTemplateId());
                if (template == null) {
                    if (projectTask.getPortableMailTemplate() == null) {
                        return "Custom e-mail";
                    }

                    PortableMailTemplate pmt = PortableMailTemplateCodec.decode(projectTask.
                            getPortableMailTemplate());

                    return pmt.getSubject().getContent();
                }

                return template.getName();
            }
        }.encode();
    }

    private List<MailTemplate> getMailTemplates(RequestCycle cycle, long orgId) {

        long mailBucket = new GetOrgMailBucketCommand(cycle).forOrg(orgId);

        MailTemplateServiceClient mtClient = getMailTemplateClient(cycle);

        return mtClient.getBucketMailTemplates(mailBucket);

    }

    private static ProductMaterialCombo getProjectProducts(RequestCycle cycle, long projectId) {
        ProjectMaterialCoordinatorClient pmcClient = getProjectMaterialCoordinatorClient(cycle);
        List<ProjectProduct> projProducts = pmcClient.getProjectProducts(projectId);

        List<String> prodIds = CollectionsUtil.transformList(projProducts, ProjectProductTransformers.
                getProductIdStrTransformer());

        final ProductDirectoryClient pdClient =
                Clients.getClient(cycle, ProductDirectoryClient.class);

        List<Product> existingProducts = pdClient.getProducts(true, prodIds);

        List<Material> missing = getMissingProducts(prodIds, existingProducts);

        return new ProductMaterialCombo(existingProducts, missing);
    }

    public static List<Material> getProjectMaterials(ProjectMaterialCoordinatorClient pmcClient,
            long prjId,
            RequestCycle cycle) {

        List<OrgMaterial> orgMats = pmcClient.getProjectOrgMaterials(prjId);

        ProductMaterialCombo productCombo = getProjectProducts(cycle, prjId);

        MaterialListFactory mlf =
                new MaterialListFactory(cycle, CocositeUserHelper.getUserLocale(cycle));

        mlf.addOrgMaterials(orgMats);
        mlf.addProducts(productCombo.products);
        mlf.addMaterials(productCombo.missing);
        List<Material> materials = mlf.getList();

        return materials;
    }

    private void reactivateProjectParticipants(RequestCycle cycle, OrgProject project) {
        CocoboxCordinatorClient ccbcClient = getCocoboxCordinatorClient(cycle);
        List<ProjectParticipation> participations =
                ccbcClient.listProjectParticipations(project.getProjectId());

        long caller = LoginUserAccountHelper.getUserId(cycle);
        for (ProjectParticipation participation : participations) {
            if (participation.isActivated()) {
                ccbcClient.activateParticipation(caller, participation.getParticipationId());
            }
        }
    }

    private void cancelInvitations(RequestCycle cycle, OrgProject project, boolean sendUpdates) {
        CocoboxCordinatorClient ccbcClient = getCocoboxCordinatorClient(cycle);
        List<ProjectParticipation> participations =
                ccbcClient.listProjectParticipations(project.getProjectId());

        long caller = LoginUserAccountHelper.getUserId(cycle);

        List<Long> ids = new ArrayList<>(participations.size());

        for (ProjectParticipation participation : participations) {
            if (participation.isActivated()) {
                ids.add(participation.getParticipationId());
            }
        }

        ParticipationCalendarCancellationRequest cancelReq =
                new ParticipationCalendarCancellationRequest(caller, ids, sendUpdates, true);

        ccbcClient.sendParticipationCalendarCancellations(cancelReq);

    }

    private Object timezoneSection(String title, List<TimeZone> zones) {
        Map<String,Object> section = new HashMap<>();
        section.put("text", title);

        List<Map<String,String>> otherList = new ArrayList<>(zones.size());

        for (TimeZone timeZone : zones) {
            @SuppressWarnings("unchecked")
            Map<String,String> map = new Flat3Map();
            map.put("value", timeZone.getID());
            map.put("text", timeZone.getID());
            otherList.add(map);
        }

        section.put("children", otherList);

        return section;
    }

    private static class ProductMaterialCombo {
        public final List<Product> products;
        public final List<Material> missing;

        public ProductMaterialCombo(List<Product> products, List<Material> missing) {
            this.products = products;
            this.missing = missing;
        }
    }
}
