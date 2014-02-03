/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.service.common.ccbc.project.cddb.DatabankDateConverter;
import se.dabox.service.common.ccbc.project.cddb.DatabankEntry;
import se.dabox.service.common.ccbc.project.cddb.StandardDatabankEntry;
import se.dabox.service.common.coursedesign.ComponentConstants;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.reldate.DueOffsetCalculation;
import se.dabox.service.common.coursedesign.reldate.RelativeStringDecoder;
import se.dabox.service.common.coursedesign.reldate.RelativeStringInformation;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;

/**
 * Updater that check all components for a enable condition of time and if it relative and
 * calculates the relative date accordingly. Fields with an override flag is not updated.
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class RelativeEnableDatabankUpdater {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(RelativeEnableDatabankUpdater.class);

    private final Set<DatabankEntry> databank;
    private final DatabankFacade oldDb;
    private final DatabankDateConverter ddc;
    private final CourseDesignDefinition cdd;

    public RelativeEnableDatabankUpdater(Set<DatabankEntry> databank,
            DatabankFacade oldDatabankFacade,
            TimeZone timeZone,
            CourseDesignDefinition cdd) {
        this.databank = databank;
        this.oldDb = oldDatabankFacade;
        this.ddc = new DatabankDateConverter(timeZone);
        this.cdd = cdd;
    }

    public void update() {
        for (Component component : cdd.getComponentsRecursive()) {
            updateComponent(component);
        }
    }

    private void updateComponent(Component component) {
        if (!isRelativeEnableComponent(component)) {
            return;
        }

        String relenable = component.getProperties().get(ComponentConstants.PROP_RELATIVEENABLE);

        RelativeStringInformation rsi;
        try {
            rsi = new RelativeStringDecoder().decode(relenable);
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Invalid relativeenable rule", iae);
            return;
        }

        Date[] epochDates = getEpochDates(rsi.getUuid());

        if (epochDates == null) {
            LOGGER.info("Epoch dates not available (no due found for component {}", rsi.getUuid());
            return;
        }

        DueOffsetCalculation calc = new DueOffsetCalculation(rsi.getOffset());
        Calendar enableDate = calc.calculate(getCalendar(epochDates[0]), getCalendar(epochDates[1]));

        addEnableDate(component, enableDate);
    }

    private Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance(oldDb.getTimeZone());
        cal.setTime(date);

        return cal;
    }

    private boolean isRelativeEnableComponent(Component component) {
        String enablecond = component.getProperties().get(ComponentConstants.PROP_ENABLE_CONDITION);

        if (!ComponentConstants.ENABLECON_TIME.equals(enablecond)) {
            return false;
        }

        String relenable = component.getProperties().get(ComponentConstants.PROP_RELATIVEENABLE);

        return !StringUtils.isBlank(relenable);
    }

    private Date[] getEpochDates(UUID uuid) {
        Date starts = null;
        Date ends = null;

        for (DatabankEntry databankEntry : databank) {
            if (databankEntry.getCid().equals(uuid)) {
                switch (databankEntry.getName()) {
                    case "starts":
                        starts = ddc.toDate(databankEntry.getValue());
                        break;
                    case "ends":
                        ends = ddc.toDate(databankEntry.getValue());
                        break;
                }

                if (starts != null && ends != null) {
                    return new Date[]{starts, ends};
                }
            }
        }

        return null;
    }

    private void addEnableDate(Component component, Calendar enableDate) {
        final UUID cid = component.getCid();
        final String fieldName = ComponentConstants.FIELD_ENABLEDATE;

        String strOldValue = oldDb.getValue(cid, fieldName);

        boolean add = strOldValue == null;

        final String fieldNameOverride = fieldName + "_override";
        if (strOldValue != null) {
            //Check the override flag
            String strOverride = oldDb.getValue(cid, fieldNameOverride);
            add = strOverride != null && "false".equals(strOverride);
        }

        if (add) {
            Date date = enableDate.getTime();

            String dbValue = ddc.toString(date);
            LOGGER.debug("Adding new enabledate value for {}: {}", cid, dbValue);
            databank.add(new StandardDatabankEntry(cid, fieldName, dbValue, 0));
            databank.add(new StandardDatabankEntry(cid, fieldNameOverride, "false", 0));
        } else {
            LOGGER.debug(
                    "Not setting relative enable date value for {}/{}. Date is same as old value or override block in place",
                    cid, component.getBasetype());
        }

    }

}
