/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.formdata.Blank;
import se.dabox.cocobox.cpweb.module.core.AbstractWebAuthModule;
import se.dabox.cocobox.cpweb.module.coursedesign.DesignTechInfo;
import se.dabox.cocobox.cpweb.module.project.details.DateTimeFormatter;
import se.dabox.cocobox.cpweb.module.project.details.ExtendedComponentFieldName;
import se.dabox.cocobox.cpweb.module.project.details.FieldSetCreator;
import se.dabox.cocobox.cpweb.module.project.details.RelativeDateDatabankUpdater;
import se.dabox.cocobox.cpweb.module.project.details.RelativeEnableDatabankUpdater;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.autoical.ParticipationCalendarCancellationRequest;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectSubtypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.UpdateProjectRequest;
import se.dabox.service.common.ccbc.project.cddb.DatabankDateConverter;
import se.dabox.service.common.ccbc.project.cddb.DatabankEntry;
import se.dabox.service.common.ccbc.project.cddb.StandardDatabankEntry;
import se.dabox.service.common.coursedesign.ComponentDataValue;
import se.dabox.service.common.coursedesign.ComponentFieldName;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.ValidateDataRequest;
import se.dabox.service.common.coursedesign.ValidateDesignDataResponse;
import se.dabox.service.common.coursedesign.reldate.RelativeDateCalculator;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.CourseDesignInfo;
import se.dabox.service.common.coursedesign.v1.DataType;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;
import se.dabox.util.collections.Predicate;
import se.dabox.util.collections.Transformer;
import se.dabox.util.collections.ValueUtils;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project.vd")
public class VerifyProjectDesignModule extends AbstractWebAuthModule {

    public static final String ACTION_VERIFY_NEW_DESIGN = "verifyNewDesign";
    public static final String ACTION_SECONDARY_DATA = "verifyNewDesignSecondary";
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyProjectDesignModule.class);

    @WebAction
    public RequestTarget onVerifyNewDesign(final RequestCycle cycle, String strProjectId) {
        return innerVerifyNewDesign(cycle, strProjectId, true);
    }

    @WebAction
    public RequestTarget onVerifyNewDesignSecondary(final RequestCycle cycle, String strProjectId) {
        return innerVerifyNewDesign(cycle, strProjectId, false);
    }

    private RequestTarget innerVerifyNewDesign(final RequestCycle cycle, String strProjectId,
            final boolean primary) {
        final CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        final OrgProject project = ccbc.getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        return ProjectTypeUtil.callSubtype(project, new ProjectSubtypeCallable<RequestTarget>() {
            @Override
            public RequestTarget callMainProject() {
                long designId = ValueUtils.coalesce(project.getStageDesignId(), project.
                        getDesignId());

                CourseDesign design = getCourseDesign(cycle).getDesign(designId);

                ValidateDesignDataResponse resp = getPureValidationResponse(design, cycle);

                return configureDataPage(cycle, project, design, resp, primary);
            }

            @Override
            public RequestTarget callIdProjectProject() {
                String msg = String.format("IdProject projects are not supported (%d)", project.
                        getProjectId());
                throw new UnsupportedOperationException(msg);
            }
        });
    }

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onUpdateNewDesign(RequestCycle cycle, String strProjectId) {
        return innerUpdateNewDesign(cycle, strProjectId, false);
    }

    private RequestTarget innerUpdateNewDesign(RequestCycle cycle, String strProjectId,
            boolean autoCall) {
        final CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        OrgProject project = ccbc.getProject(Long.valueOf(strProjectId));

        checkPermission(cycle, project);

        long designId = ValueUtils.coalesce(project.getStageDesignId(), project.getDesignId());

        CourseDesign design = getCourseDesign(cycle).getDesign(designId);

        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        Set<DatabankEntry> databank = new HashSet<>();

        ValidateDesignDataResponse resp = getPureValidationResponse(design, cycle);

        Map<String, Set<ExtendedComponentFieldName>> fieldMapSet
                = new FieldSetCreator(cdd, project).
                createFieldMapSet(resp);

        WebRequest req = cycle.getRequest();

        SimpleDateFormat sdf = getDatePickerSimpleDateFormat(project);
        DatabankDateConverter ddc = new DatabankDateConverter(project.getTimezone());

        Set<String> errorFields = new HashSet<>();
        Map<String, String> oldFieldValues = new HashMap<>();

        DatabankFacade oldDatabankFacade = getOldDatabankFacade(project, ccbc);

        RelativeDateCalculator rdCalculator = new RelativeDateCalculator().loadCourse(cdd).
                withTimeZone(project.getTimezone());

        for (Component component : cdd.getComponentsRecursive()) {
            String strCid = component.getCid().toString();
            Set<ExtendedComponentFieldName> cFields = fieldMapSet.get(strCid);

            if (cFields == null) {
                continue;
            }

            for (ExtendedComponentFieldName cField : cFields) {
                //Skip all non primary fields
                if (!cField.isPrimaryField()) {
                    continue;
                }

                String fFieldName = componentFieldName(strCid, cField.getName());

                LOGGER.trace("Processing field {}", fFieldName);

                //We want to save empty strings for values that are empty.
                //Duration for example is auto-populated if missing (when the value is null)
                String fValue = StringUtils.trimToEmpty(req.getParameter(fFieldName));

                if (fValue.isEmpty() && cField.isMandatory()) {
                    errorFields.add(fFieldName);
                    continue;
                }

                oldFieldValues.put(fFieldName, fValue);

                if (fValue.length() == 0) {
                    databank.add(new StandardDatabankEntry(component.getCid(), cField.getName(),
                            fValue, 0));
                    continue;
                }

                DataType dataType = cField.getDataType();
                if (dataType == DataType.DATETIME) {
                    try {
                        Date date = sdf.parse(fValue);
                        fValue = ddc.toString(date);
                    } catch (ParseException ex) {
                        errorFields.add(fFieldName);
                        continue;
                    }
                } else if (dataType == DataType.NUMBER) {
                    try {
                        Long.valueOf(fValue);
                    } catch (NumberFormatException nfe) {
                        errorFields.add(fFieldName);
                        continue;
                    }
                } else if (dataType == DataType.URL) {
                    try {
                        new URI(fValue);
                    } catch (URISyntaxException ex) {
                        errorFields.add(fFieldName);
                        continue;
                    }
                }

                databank.
                        add(new StandardDatabankEntry(component.getCid(), cField.getName(), fValue,
                                        0));
            }
        }

        if (!errorFields.isEmpty()) {
            cycle.getViewSession().setAttribute("errorFields", errorFields);
            cycle.getViewSession().setAttribute("oldFieldValues", oldFieldValues);
            LOGGER.debug("Errors in the following fields: {} (size {})", errorFields, errorFields.
                    size());
            return new WebModuleRedirectRequestTarget(VerifyProjectDesignModule.class,
                    "verifyNewDesign", strProjectId);
        }

        RelativeDateDatabankUpdater reldateUpdater
                = new RelativeDateDatabankUpdater(databank, oldDatabankFacade, rdCalculator, project.
                        getTimezone(), cdd);

        reldateUpdater.update();

        RelativeEnableDatabankUpdater relenableUpdate
                = new RelativeEnableDatabankUpdater(databank, oldDatabankFacade, project.
                        getTimezone(), cdd);

        relenableUpdate.update();

        saveDatabank(cycle, ccbc, project, databank);

        ValidateDataRequest newValidationReq = new ValidateDataRequest(design.getDesign());
        for (DatabankEntry databankEntry : databank) {
            newValidationReq.addValue(new ComponentDataValue(databankEntry.getCid(), databankEntry.
                    getName(), databankEntry.getValue()));
        }

        //Do not use the ccbc validator. That doesn't work since the relative events get demoted
        //to non mandatory fields which break the requirement to set date
        upstage(cycle, project);
        processAutoIcalChanges(cycle, project);

        WebModuleRedirectRequestTarget target = NavigationUtil.
                toProjectSecondaryData(cycle, project);

        if (autoCall) {
            target.setExtraTargetParameterString("auto=t");
        }

        return target;
    }

    private RequestTarget configureDataPage(RequestCycle cycle, OrgProject project,
            CourseDesign design, ValidateDesignDataResponse resp, boolean primary) {

        Map<String, Object> map = createMap();

        map.put("prj", project);
        map.put("designInfo", design);
        map.put("org", securedGetOrganization(cycle, project.getOrgId()));
        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());
        map.put("formsess", getValidationSession(Blank.class, cycle));

        Map<String, Set<ExtendedComponentFieldName>> fieldMapSet
                = new FieldSetCreator(cdd, project).
                createFieldMapSet(resp);

        List<Component> components = cdd.getComponents();
        List<Component> primaryComponents = getPrimaryComponentsView(components, fieldMapSet);

        if (primary) {
            components = primaryComponents;
        } else {
            populateExtraValues(cycle, fieldMapSet, cdd, project);
        }

        LOGGER.debug("Components: {}", components);
        LOGGER.debug("Primary components: {}", primaryComponents);

        map.put("components", components);

        map.put("componentFieldSet", fieldMapSet);

        map.put("databankValues", getDatabankValues(cycle, project, fieldMapSet, components));

        Set<String> errorFields = cycle.getViewSession().getAttribute("errorFields", Collections.
                <String>emptySet());
        Map<String, String> oldFieldValues = cycle.getViewSession().getAttribute("oldFieldValues",
                Collections.
                <String, String>emptyMap());

        map.put("errorFields", errorFields);
        map.put("oldValues", oldFieldValues);

        int totalComponents = countTotalFields(0, components, resp);

        map.put("primaryFieldCount", countTotalFields(0, primaryComponents, resp));

        if (totalComponents == 0) {
            if (primary) {
                LOGGER.debug("Total components is 0. Doing a innerUpdateNewDesign directly");
                return innerUpdateNewDesign(cycle, Long.toString(project.getProjectId()), true);
            } else if (isAutoRedirect(cycle)) {
                LOGGER.debug("No components found. Redirecting to project page.");
                return NavigationUtil.toProjectPage(project.getProjectId());
            }
        }

        if (primary) {
            return new FreemarkerRequestTarget("/project/projectDesignDataPrimary.html", map);
        } else {
            return new FreemarkerRequestTarget("/project/projectDesignDataSecondary.html", map);
        }
    }

    private ValidateDesignDataResponse getPureValidationResponse(CourseDesign design,
            RequestCycle cycle) {
        ValidateDataRequest req = new ValidateDataRequest(design.getDesign());
        ValidateDesignDataResponse resp = getCourseDesign(cycle).validateDesignData(req);
        return resp;
    }

    private void upstage(RequestCycle cycle, OrgProject project) {

        CourseDesignClient cdc = getCourseDesign(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);

        Long designId = project.getDesignId();

        boolean newDesign = false;
        if (project.getStageDesignId() != null) {
            String techInfo = DesignTechInfo.createLiveTechInfo(project.getProjectId());
            designId = cdc.copyDesign(project.getStageDesignId(), userId, techInfo);
            newDesign = true;
        }

        Long oldMasterDatabank = null;
        Long databankId = project.getMasterDatabank();

        if (project.getStageDatabank() != null) {
            databankId = project.getStageDatabank();
            oldMasterDatabank = project.getMasterDatabank();
        }

        CourseDesignInfo info = getCourseInfo(cycle, designId);

        UpdateProjectRequest upr = new UpdateProjectRequest(project.getProjectId(), project.
                getName(), project.getLocale(), userId, project.getCountry(), project.getTimezone(),
                designId, null, databankId, null, project.getNote(), project.getInvitePassword(),
                project.getInviteLimit(), project.isInvitePossible(),
                info.getUserTitle(),
                info.getUserDescription(),
                project.isAutoIcal());

        getCocoboxCordinatorClient(cycle).updateOrgProject(upr);

        if (oldMasterDatabank != null) {
            try {
                getCocoboxCordinatorClient(cycle).deleteDatabank(userId, oldMasterDatabank);
            } catch (NotFoundException nfe) {
                LOGGER.info("Old databank didn't exist: {}", oldMasterDatabank);
            }
        }

        if (newDesign) {
            //Remove both

            removeDesign(cycle, userId, project.getDesignId());
            removeDesign(cycle, userId, project.getStageDesignId());
        }

        getCocoboxCordinatorClient(cycle).syncProjectState(project.getProjectId());
    }

    private void saveDatabank(RequestCycle cycle, CocoboxCordinatorClient ccbc, OrgProject project,
            Set<DatabankEntry> databank) {

        List<DatabankEntry> oldDatabank = getOldDatabank(project, ccbc);

        Set<DatabankEntry> newSet = new HashSet<>();
        newSet.addAll(databank);
        newSet.addAll(oldDatabank);

        List<DatabankEntry> list = new ArrayList<>(newSet);

        Long databankId;
        if (project.getStageDatabank() == null) {
            databankId = ccbc.createCopyDatabank(LoginUserAccountHelper.getUserId(cycle), project.
                    getMasterDatabank());
            project.setStageDatabank(databankId);
        } else {
            databankId = project.getStageDatabank();
        }

        ccbc.saveDatabank(LoginUserAccountHelper.getUserId(cycle), databankId, list);
    }

    private Map<String, String> getDatabankValues(RequestCycle cycle,
            OrgProject project,
            Map<String, Set<ExtendedComponentFieldName>> fieldMapSet,
            List<Component> allComponents) {

        Long databankId = ValueUtils.
                coalesce(project.getStageDatabank(), project.getMasterDatabank());

        List<DatabankEntry> databank = getCocoboxCordinatorClient(cycle).getDatabank(databankId);

        Map<String, String> map = MapUtil.createHash(databank);
        SimpleDateFormat sdf = getDatePickerSimpleDateFormat(project);
        DatabankDateConverter ddc = new DatabankDateConverter(project.getTimezone());

        for (DatabankEntry databankEntry : databank) {
            DataType type = getDatabankFieldType(databankEntry.getCidString(), databankEntry.
                    getName(),
                    fieldMapSet);

            String formValue = StringUtils.trimToNull(databankEntry.getValue());

            if (DataType.DATETIME == type) {
                Date date = ddc.toDate(formValue);
                if (date == null) {
                    formValue = null;
                } else {
                    formValue = sdf.format(date);
                }
            }

            //Don't use componentFieldName here since we want a "pure" value.
            String name = databankEntry.getCidString() + '_' + databankEntry.getName();
            map.put(name, formValue);
        }

        addDefaultDurationTimes(cycle, map, fieldMapSet, allComponents, project);

        return map;
    }

    private CourseDesignClient getCourseDesign(RequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

    private void removeDesign(RequestCycle cycle, long userId, Long designId) {
        if (designId == null) {
            return;
        }

        getCourseDesign(cycle).removeDesign(userId, designId);
    }

    private static DataType getDatabankFieldType(final String cid, final String name,
            final Map<String, Set<ExtendedComponentFieldName>> fieldMapSet) {

        Set<? extends ComponentFieldName> set = fieldMapSet.get(cid);

        if (set == null) {
            return null;
        }

        for (ComponentFieldName componentFieldName : set) {
            if (componentFieldName.getName().equals(name)) {
                return componentFieldName.getDataType();
            }
        }

        return null;
    }

    public static SimpleDateFormat getDatePickerSimpleDateFormat(OrgProject project) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sdf.setTimeZone(project.getTimezone());
        return sdf;
    }

    private void addDefaultDurationTimes(RequestCycle cycle,
            Map<String, String> defaultValueMap,
            Map<String, Set<ExtendedComponentFieldName>> fieldMapSet,
            List<Component> components,
            OrgProject project) {
        new ProductDurationDefaultValuePopulator(cycle, project).populate(defaultValueMap,
                fieldMapSet, components);
    }

    /**
     * This returns the form field name.
     *
     * <p>
     * <b>IMPORTANT</b> THIS IS NOT ALWAYS WHAT YOU WANT. SOMETIMES YOU WANT WITH THE f PREFIX AND
     * SOMETIMES NOT</b></p>
     *
     * @param cid
     * @param fieldName
     *
     * @return
     */
    public static String componentFieldName(String cid, String fieldName) {
        return 'f' + cid + '_' + fieldName;
    }

    private CourseDesignInfo getCourseInfo(RequestCycle cycle, Long designId) {
        CourseDesignClient cdClient = getCourseDesign(cycle);

        CourseDesign design = cdClient.getDesign(designId);
        CourseDesignDefinition cdd = CddCodec.decode(cycle, design.getDesign());

        return cdd.getInfo();
    }

    private void processAutoIcalChanges(RequestCycle cycle, OrgProject project) {

        if (!WebFeatures.getFeatures(cycle).hasFeature(CocositeWebFeatureConstants.AUTOICAL)) {
            LOGGER.debug("WebFeature autoical not enabled");
            return;
        }

        if (!project.isAutoIcal()) {
            LOGGER.debug("Project {} has not enabled autoical", project.getProjectId());
            return;
        }

        CocoboxCordinatorClient ccbcClient = getCocoboxCordinatorClient(cycle);

        List<ProjectParticipation> participations = ccbcClient.listProjectParticipations(project.
                getProjectId());

        List<Long> participationIds = new ArrayList<>(participations.size());

        final long caller = LoginUserAccountHelper.getUserId(cycle);

        for (ProjectParticipation participation : participations) {
            if (!participation.isActivated()) {
                continue;
            }

            participationIds.add(participation.getParticipationId());

            ccbcClient.activateParticipation(caller, participation.getParticipationId());
        }

        ParticipationCalendarCancellationRequest cancellationReq
                = ParticipationCalendarCancellationRequest.mailCancellationRequest(caller,
                        participationIds);

        ccbcClient.sendParticipationCalendarCancellations(cancellationReq);
    }

    private List<Component> getPrimaryComponentsView(List<Component> components,
            final Map<String, Set<ExtendedComponentFieldName>> fieldMapSet) {
        return CollectionsUtil.transformListNotNull(components,
                new Transformer<Component, Component>() {
                    @Override
                    public Component transform(Component item) {
                        return getPrimaryComponentView(item, fieldMapSet);
                    }
                });
    }

    /**
     * Returns a view for the component if it contains a primary component (somewhere)
     *
     */
    private Component getPrimaryComponentView(Component component,
            final Map<String, Set<ExtendedComponentFieldName>> fieldMapSet) {
        List<Component> children = CollectionsUtil.transformListNotNull(component.getChildren(),
                new Transformer<Component, Component>() {
                    @Override
                    public Component transform(Component item) {
                        return getPrimaryComponentView(item, fieldMapSet);
                    }
                });

        if (children.size() > 0) {
            return component;
        }

        Set<ExtendedComponentFieldName> fields = fieldMapSet.get(component.getCid().toString());

        boolean isPrimary = false;

        if (fields != null) {
            for (ExtendedComponentFieldName field : fields) {
                if (field.isPrimaryField()) {
                    isPrimary = true;
                    break;
                }
            }
        }

        if (isPrimary) {
            Component retval = new Component(component.getType(), component.getCid());
            retval.setProperties(component.getProperties());
            retval.setChildren(children);
            return retval;
        }

        return null;
    }

    private List<DatabankEntry> getOldDatabank(OrgProject project, CocoboxCordinatorClient ccbc) {
        long oldDatabankId = ValueUtils.coalesce(project.getStageDatabank(), project.
                getMasterDatabank());
        List<DatabankEntry> oldDatabank = ccbc.getDatabank(oldDatabankId);
        return oldDatabank;
    }

    private DatabankFacade getOldDatabankFacade(OrgProject project, CocoboxCordinatorClient ccbc) {
        List<DatabankEntry> oldDatabank = getOldDatabank(project, ccbc);

        return new DatabankFacade(oldDatabank, project);
    }

    private void populateExtraValues(RequestCycle cycle,
            Map<String, Set<ExtendedComponentFieldName>> fieldMapSet, CourseDesignDefinition cdd,
            OrgProject project) {
        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        DatabankFacade dbFacade = getOldDatabankFacade(project, ccbc);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        DateTimeFormatter dtf = new DateTimeFormatter(project.getTimezone(), userLocale);

        for (Map.Entry<String, Set<ExtendedComponentFieldName>> entry : fieldMapSet.entrySet()) {
            for (ExtendedComponentFieldName cfn : entry.getValue()) {
                if (cfn.getDataType() == DataType.DATETIME) {
                    Date dbDate = dbFacade.getDate(cfn.getCid(), cfn.getName());
                    if (dbDate != null) {
                        cfn.setStringValue(dtf.format(dbDate));

                        String strOverride = dbFacade.getValue(cfn.getCid(), cfn.getName()
                                + "_override");
                        boolean override = strOverride == null && "true".equals(strOverride);

                        cfn.setOverrideValue(override);
                    }
                } else {
                    String strVal = StringUtils.trimToEmpty(dbFacade.
                            getValue(cfn.getCid(), cfn.getName()));
                    cfn.setStringValue(strVal);
                }
            }
        }

    }

    private int countTotalFields(int total, List<Component> components,
            ValidateDesignDataResponse vddr) {

        if (components == null || components.isEmpty()) {
            return total;
        }

        final Set<UUID> cids = CollectionsUtil.transform(components,
                new Transformer<Component, UUID>() {

                    @Override
                    public UUID transform(Component item) {
                        return item.getCid();
                    }
                });

        final Set<UUID> allCids = new HashSet<>(cids);

        for (Component component : components) {
            List<Component> children = component.getChildren();

            if (children == null || children.isEmpty()) {
                continue;
            }

            for (Component child : children) {
                allCids.add(child.getCid());
            }
        }

        return CollectionsUtil.countMatching(vddr.getFields(), new Predicate<ComponentFieldName>() {

            @Override
            public boolean evalute(ComponentFieldName item) {
                return allCids.contains(item.getCid());
            }
        });
    }

    private boolean isAutoRedirect(RequestCycle cycle) {
        return "t".equals(cycle.getRequest().getParameter("auto"));
    }
}
