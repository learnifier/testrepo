/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import net.unixdeveloper.druwa.request.StringRequestTarget;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.time.DateUtils.toCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.project.details.DateTimeFormatter;
import se.dabox.cocobox.cpweb.module.project.details.ProjectTypeValueValidator;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.cddb.DatabankDateConverter;
import se.dabox.service.common.ccbc.project.cddb.DatabankEntry;
import se.dabox.service.common.ccbc.project.cddb.StandardDatabankEntry;
import se.dabox.service.common.coursedesign.ComponentConstants;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.reldate.DueOffsetCalculation;
import se.dabox.service.common.coursedesign.reldate.RelativeDateCalculator;
import se.dabox.service.common.coursedesign.reldate.RelativeStringDecoder;
import se.dabox.service.common.coursedesign.reldate.RelativeStringInformation;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.coursedesign.v1.DataType;
import se.dabox.service.common.json.JsonUtils;
import se.dabox.service.coursedesign.validator.impl.TypeHandler;
import se.dabox.service.coursedesign.validator.spi.CourseComponentValidator;
import se.dabox.service.coursedesign.validator.spi.DataField;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.ValueUtils;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project.db")
public class ProjectDatabankJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectDatabankJsonModule.class);

    @WebAction(methods = HttpMethod.POST)
    public RequestTarget onSetValue(RequestCycle cycle, String strProjectId) {
        WebRequest req = cycle.getRequest();

        String strCid = DruwaParamHelper.getMandatoryParam(LOGGER, req, "pk");
        String strFieldName = DruwaParamHelper.getMandatoryParam(LOGGER, req, "name");
        String strOverride = cycle.getRequest().getParameter("override");
        strOverride = ValueUtils.coalesce(strOverride, "true");
        String strValue = req.getParameter("value");

        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject project = ccbc.getProject(prjId);
        checkPermission(cycle, project);

        long designId = ValueUtils.coalesce(project.getStageDesignId(), project.getDesignId());

        CourseDesign design =
                getCourseDesign(cycle).getDesign(designId);

        CourseDesignDefinition cdd =
                CddCodec.decode(cycle, design.getDesign());

        Component comp = cdd.getComponentMap().get(UUID.fromString(strCid));
        if (comp == null) {
            return errorJson(cycle, "No component found");
        }

        CourseComponentValidator validator =
                TypeHandler.getValidatorForType(comp.getBasetype());

        if (validator == null) {
            return errorJson(cycle, "No validator found");
        }

        DataField field = validator.getField(strFieldName);

        if (field == null) {
            return errorJson(cycle, "No field found");
        }

        RelativeDateCalculator rdc = new RelativeDateCalculator().loadCourse(cdd).
                withTimeZone(project.getTimezone());

        if (rdc.isEpoch(comp)) {
            return errorJson(cycle, "Component is an epoch");
        }

        boolean override = Boolean.valueOf(strOverride);

        Set<DatabankEntry> newDatabank = new HashSet<>();
        String newValue = "";

        if (override) {
            //Set a new value

            if (!ProjectTypeValueValidator.valid(project, field.getDataType(), strValue)) {
                return errorJson(cycle, "The information you entered is not valid");
            }

            String val = strValue;
            newValue = val;

            if (field.getDataType() == DataType.DATETIME) {
                SimpleDateFormat sdf =
                        VerifyProjectDesignModule.getDatePickerSimpleDateFormat(project);
                Date date;
                try {
                    date = sdf.parse(strValue);
                } catch (ParseException ex) {
                    throw new IllegalStateException("Failed to parse date", ex);
                }

                DatabankDateConverter ddc = new DatabankDateConverter(project.getTimezone());
                val = ddc.toString(date);
                Locale locale = CocositeUserHelper.getUserLocale(cycle);
                newValue =
                        new DateTimeFormatter(project.getTimezone(), locale).format(date);
            }
            addNewValue(newDatabank, comp, strFieldName, val, true);

        } else {
            //Recalculate the default value
            newValue = calculateNewDefaultValue(cycle,
                    ccbc,
                    newDatabank,
                    cdd,
                    project,
                    comp,
                    field,
                    rdc);
        }

        long databankId = ValueUtils.coalesce(project.getStageDatabank(), project.
                getMasterDatabank());

        addOldDatabank(ccbc, newDatabank, databankId);

        long caller = LoginUserAccountHelper.getUserId(cycle);
        getCocoboxCordinatorClient(cycle).saveDatabank(caller, databankId, new ArrayList<>(
                newDatabank));

        Map<String, String> map = new HashMap<>();
        map.put("status", "OK");
        map.put("stringValue", newValue);

        return new StringRequestTarget("application/json", JsonUtils.encode(map));
    }

    private CourseDesignClient getCourseDesign(RequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

    private RequestTarget errorJson(RequestCycle cycle, String errorMessage) {
        cycle.getResponse().setStatus(400);
        cycle.getResponse().setContentType("text/plain;charset=UTF-8");
        cycle.getResponse().getWriter().append(errorMessage);
        return null;
    }

    private void addOldDatabank(CocoboxCordinatorClient ccbc,
            Set<DatabankEntry> newDatabank, long databankId) {
        List<DatabankEntry> databank = ccbc.getDatabank(databankId);
        newDatabank.addAll(databank);
    }

    private String calculateNewDefaultValue(RequestCycle cycle,
            CocoboxCordinatorClient ccbc,
            Set<DatabankEntry> newDatabank,
            CourseDesignDefinition cdd,
            OrgProject project, Component comp, DataField field, RelativeDateCalculator rdc) {

        if (ComponentConstants.FIELD_ENABLEDATE.equals(field.getName())) {
            return calculateNewRelativeEnableDefaultValue(cycle, ccbc, newDatabank, project, comp, field);
        } else {
            return calculateNewRelativeDefaultValue(cycle, ccbc, newDatabank, cdd, project, comp, field, rdc);
        }
    }

    private String calculateNewRelativeEnableDefaultValue(RequestCycle cycle,
            CocoboxCordinatorClient ccbc,
            Set<DatabankEntry> newDatabank,
            OrgProject project, Component comp, DataField field) {

        String rule = comp.getProperties().get(ComponentConstants.PROP_RELATIVEENABLE);

        if (StringUtils.isBlank(rule)) {
            addNewValue(newDatabank, comp, field.getName(), "", false);
            return "";
        }

        RelativeStringInformation rsi;

        try {
            rsi = new RelativeStringDecoder().decode(rule);
        } catch(IllegalArgumentException ex) {
            LOGGER.warn("Invalid relative rule string: {}: {}", rule, ex);
            addNewValue(newDatabank, comp, field.getName(), "", false);
            return "";
        }

        UUID epochCid = rsi.getUuid();

        long oldDatabankId = ValueUtils.coalesce(project.getStageDatabank(), project.
                getMasterDatabank());

        List<DatabankEntry> oldDatabank = ccbc.getDatabank(oldDatabankId);
        DatabankFacade dbFacade = new DatabankFacade(oldDatabank, project);

        Date dueDate = dbFacade.getDate(epochCid, "due");
        Date starts = dbFacade.getDate(epochCid, "starts");
        Date ends = dbFacade.getDate(epochCid, "ends");

        if (starts == null) {
            starts = dueDate;
        }

        if (ends == null) {
            ends = starts;
        }

        if (starts == null && ends == null) {
            addNewValue(newDatabank, comp, field.getName(), "", false);
            return "";
        }

        DueOffsetCalculation calculation = new DueOffsetCalculation(rsi.getOffset());
        Calendar calculatedValue = calculation.calculate(toCalendar(starts), toCalendar(ends));

        if (calculatedValue == null) {
            addNewValue(newDatabank, comp, field.getName(), "", false);
            return "";
        }

        DatabankDateConverter ddc =
                new DatabankDateConverter(project.getTimezone());
        addNewValue(newDatabank, comp, field.getName(), ddc.toString(calculatedValue.getTime()), false);

        Locale locale = CocositeUserHelper.getUserLocale(cycle);
        return new DateTimeFormatter(project.getTimezone(), locale).
                format(calculatedValue.getTime());
    }

    private String calculateNewRelativeDefaultValue(RequestCycle cycle,
            CocoboxCordinatorClient ccbc,
            Set<DatabankEntry> newDatabank,
            CourseDesignDefinition cdd,
            OrgProject project, Component comp, DataField field, RelativeDateCalculator rdc) {

        long oldDatabankId = ValueUtils.coalesce(project.getStageDatabank(), project.
                getMasterDatabank());

        List<DatabankEntry> oldDatabank = ccbc.getDatabank(oldDatabankId);
        DatabankFacade dbFacade = new DatabankFacade(oldDatabank, project);

        UUID epochCid = rdc.getEpochCidFor(comp);

        Date dueDate = dbFacade.getDate(epochCid, "due");
        Date starts = dbFacade.getDate(epochCid, "starts");
        Date ends = dbFacade.getDate(epochCid, "ends");

        if (starts == null) {
            starts = dueDate;
        }

        if (ends == null) {
            ends = starts;
        }

        if (starts == null && ends == null) {
            addNewValue(newDatabank, comp, field.getName(), "", false);
            return "";
        }

        Map<UUID, Date> results = null;
        switch (field.getName()) {
            case "due":
                results = rdc.calculateChanges(epochCid, starts, ends);
                break;
            case "starts":
                results = rdc.calculateChanges(epochCid, starts, starts);
                break;
            case "ends":
                results = rdc.calculateChanges(epochCid, ends, ends);
                break;
        }

        Date newDate = results.get(comp.getCid());
        DatabankDateConverter ddc =
                new DatabankDateConverter(project.getTimezone());

        addNewValue(newDatabank, comp, field.getName(), ddc.toString(newDate), false);

        Locale locale = CocositeUserHelper.getUserLocale(cycle);
        return new DateTimeFormatter(project.getTimezone(), locale).format(newDate);
    }

    private void addNewValue(Set<DatabankEntry> newDatabank, Component comp, String name,
            String val, boolean override) {
        newDatabank.add(new StandardDatabankEntry(comp.getCid(), name, val, 0));
        newDatabank.add(new StandardDatabankEntry(comp.getCid(), name + "_override", override
                ? "true" : "false", 0));
    }
}
