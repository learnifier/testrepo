/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.service.common.ccbc.project.cddb.DatabankDateConverter;
import se.dabox.service.common.ccbc.project.cddb.DatabankEntry;
import se.dabox.service.common.ccbc.project.cddb.StandardDatabankEntry;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.reldate.RelativeDateCalculator;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.coursedesign.validator.impl.TypeHandler;

/**
 * This class calculates all relative dates and store them in the databank unless
 * there is a override flag (databank value name is the same and a trailing _override)
 * is stored in the databank.
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class RelativeDateDatabankUpdater {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelativeDateDatabankUpdater.class);
    private static final Set<String> NAME_SET = new HashSet<>(Arrays.asList("starts", "ends"));
    private static final String DUE = "due";
    private static final String DUE_OVERRIDE = "due_override";

    private final Set<DatabankEntry> databank;
    private final DatabankFacade oldDb;
    private final RelativeDateCalculator rdCalculator;
    private final DatabankDateConverter ddc;
    private final CourseDesignDefinition cdd;

    public RelativeDateDatabankUpdater(Set<DatabankEntry> databank,
            DatabankFacade oldDatabankFacade,
            RelativeDateCalculator rdCalculator, 
            TimeZone timeZone,
            CourseDesignDefinition cdd) {
        this.databank = databank;
        this.oldDb = oldDatabankFacade;
        this.rdCalculator = rdCalculator;
        this.ddc = new DatabankDateConverter(timeZone);
        this.cdd = cdd;
    }

    public void update() {

        LOGGER.debug("Entries to process {}", databank.size());
        
        Map<UUID,StartEndsInfo> infoMap = new HashMap<>();
        
        for (DatabankEntry databankEntry : databank) {
            final String deName = databankEntry.getName();
            if (!NAME_SET.contains(deName)) {
                continue;
            }

            StartEndsInfo info = infoMap.get(databankEntry.getCid());
            if (info == null) {
                info = new StartEndsInfo();
                infoMap.put(databankEntry.getCid(), info);
            }
            switch (deName) {
                case "starts":
                    info.starts = databankEntry.getValue();
                    break;
                case "ends":
                    info.ends = databankEntry.getValue();
                    break;
                case "due":
                    info.due = databankEntry.getValue();
                    break;
            }
        }

        LOGGER.debug("Entries after first pass: {}", infoMap.size());

        //Remove invalid entries
        for (Iterator<Map.Entry<UUID, StartEndsInfo>> it = infoMap.entrySet().iterator(); it.
                hasNext();) {
            Entry<UUID, StartEndsInfo> entry = it.next();

            if (!entry.getValue().isValid()) {
                it.remove();
            }
        }

        LOGGER.debug("Entries after second pass: {}", infoMap.size());

        //Now we have a map with valid entries, loop them through

        for (Iterator<Map.Entry<UUID, StartEndsInfo>> it = infoMap.entrySet().iterator(); it.
                hasNext();) {
            Entry<UUID, StartEndsInfo> entry = it.next();

            UUID cid = entry.getKey();

            if (!rdCalculator.isEpoch(cid)) {
                continue;
            }

            StartEndsInfo info = entry.getValue();

            Date starts = getDate(info.getStarts());
            Date ends = getDate(info.getEnds());

            Map<UUID, Date> dueResults = rdCalculator.calculateChanges(cid, starts, ends);
            Map<UUID, Date> startsResults = rdCalculator.calculateChanges(cid, starts, starts);
            Map<UUID, Date> endsResults = rdCalculator.calculateChanges(cid, ends, ends);

            for (Entry<UUID, Date> newRelDate : dueResults.entrySet()) {
                UUID newCid = newRelDate.getKey();
                Component targetComponent = cdd.getComponentMap().get(newCid);

                addNewFieldValue(targetComponent, "due", dueResults);
                addNewFieldValue(targetComponent, "starts", startsResults);
                addNewFieldValue(targetComponent, "ends", endsResults);                
            }
        }

    }

    private Date getDate(String dateString) {
        return ddc.toDate(dateString);
    }

    private void addNewFieldValue(Component component, String fieldName,
            Map<UUID, Date> results) {

        UUID cid = component.getCid();

        if (TypeHandler.getValidatorForType(component.getBasetype()).getField(fieldName)
                == null) {
            LOGGER.debug(
                    "Component {}/{} did not have a {} field",
                    component.getBasetype(), component.getCid(), fieldName);
            return;
        }

        String strOldValue = oldDb.getValue(cid, fieldName);

        boolean add = strOldValue == null;

        final String fieldNameOverride = fieldName+"_override";
        if (strOldValue != null) {
            //Check the override flag
            String strOverride = oldDb.getValue(cid, fieldNameOverride);
            add = strOverride != null && "false".equals(strOverride);
        }

        if (add) {
            Date date = results.get(cid);

            String dbValue = ddc.toString(date);
            LOGGER.debug("Adding new due value for {}: {}", cid, dbValue);
            databank.add(new StandardDatabankEntry(cid, fieldName, dbValue, 0));
            databank.add(new StandardDatabankEntry(cid, fieldNameOverride, "false", 0));
        } else {
            LOGGER.debug("Override block in the way. Not setting relative due value for {}/{}",
                    cid, component.getBasetype());
        }
    }

    private static class StartEndsInfo {
        public String starts;
        public String ends;
        public String due;

        public StartEndsInfo() {            
        }

        public boolean isValid() {
            return !StringUtils.isEmpty(due) || 
                    (!StringUtils.isEmpty(starts) && !StringUtils.isEmpty(ends));
        }

        public String getStarts() {
            return !StringUtils.isEmpty(starts) ? starts : due;
        }

        public String getEnds() {
            return !StringUtils.isEmpty(ends) ? ends : due;
        }
    }
}
