/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.collections.map.Flat3Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.command.RecentTimezoneUpdateCommand;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.UpdateProjectRequest;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.HybridLocaleUtils;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
@WebModuleMountpoint("/project.sjs")
public class ProjectSettingsJsonModule extends AbstractJsonAuthModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProjectSettingsJsonModule.class);

    @WebAction
    public RequestTarget onChangeSetting(RequestCycle cycle, String strProjectId) {
        try {
            return innerChangeSetting(cycle, strProjectId);
        } catch (ValidationException ex) {
            return returnSettingError(cycle, ex.message);
        }
    }

    private RequestTarget innerChangeSetting(RequestCycle cycle, String strProjectId) {
        long prjId = Long.valueOf(strProjectId);

        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(cycle);
        OrgProject prj = ccbc.getProject(prjId);
        checkPermission(cycle, prj);

        String fieldName = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "pk");
        String fieldValue = DruwaParamHelper.getMandatoryParam(LOGGER, cycle.getRequest(), "value");
        String stringValue = null;

        long projectId = prj.getProjectId();
        String name = prj.getName();
        Locale locale = prj.getLocale();
        long updatedBy = LoginUserAccountHelper.getUserId(cycle);

        Locale country = prj.getCountry();
        TimeZone timezone = prj.getTimezone();
        Long designId = prj.getDesignId();
        Long stageDesignId = prj.getStageDesignId();

        Long masterDatabank = prj.getMasterDatabank();
        Long stageDatabank = prj.getStageDatabank();
        String note = prj.getNote();
        String invitePassword = prj.getInvitePassword();

        Integer inviteLimit = prj.getInviteLimit();
        boolean selfRegistrationEnabled = prj.isSelfRegistrationEnabled();
        String userTitle = prj.getUserTitle();

        String userDescription = prj.getUserDescription();
        boolean autoIcal = prj.isAutoIcal();

        boolean match = true;
        switch (fieldName) {
            case "name":
                name = fieldValue;
                break;
            case "note":
                note = fieldValue;
                break;
            case "usertitle":
                if (prj.getType() != ProjectType.MATERIAL_LIST_PROJECT) {
                    return returnSettingError(cycle, "Must be a matlist project");
                }
                userTitle = fieldValue;
                break;
            case "userdesc":
                if (prj.getType() != ProjectType.MATERIAL_LIST_PROJECT) {
                    return returnSettingError(cycle, "Must be a matlist project");
                }
                userDescription = fieldValue;
                break;
            case "timezone":
                ObjectStringContainer<TimeZone> tzc = getProjectTimeZone(cycle, fieldValue);
                timezone = tzc.getObj();
                stringValue = tzc.getValue();
                break;
            case "locale":
                ObjectStringContainer<Locale> localc = getProjectLocale(cycle, fieldValue);
                locale = localc.getObj();
                stringValue = localc.getValue();
                break;
            case "country":
                ObjectStringContainer<Locale> countryc = getProjectCountry(cycle, fieldValue);
                country = countryc.getObj();
                stringValue = countryc.getValue();
                break;
            default:
                match = false;
        }

        if (!match) {
            return returnSettingError(cycle, "Unknown field: " + fieldName);
        }

        UpdateProjectRequest upr = new UpdateProjectRequest(projectId, name, locale, updatedBy,
                country, timezone, designId, stageDesignId, masterDatabank, stageDatabank, note,
                invitePassword, inviteLimit, selfRegistrationEnabled, userTitle, userDescription,
                autoIcal, prj.isSocial());

        getCocoboxCordinatorClient(cycle).updateOrgProject(upr);

        new RecentTimezoneUpdateCommand(cycle).updateRecentTimezone(prj.getOrgId(), timezone);

        if (stringValue == null) {
            stringValue = fieldValue;
        }

        Map<String, String> map = createFlatMap();
        map.put("status", "OK");
        map.put("value", fieldValue);
        map.put("stringValue", stringValue);

        return jsonTarget(map);
    }

    private static RequestTarget returnSettingError(RequestCycle cycle, String fieldName) {
        cycle.getResponse().setStatus(400);
        cycle.getResponse().setContentType("text/plain;charset=UTF-8");
        cycle.getResponse().getWriter().append(fieldName);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> createFlatMap() {
        return new Flat3Map();
    }

    private ObjectStringContainer<TimeZone> getProjectTimeZone(RequestCycle cycle, String fieldValue) {
        TimeZone tz = TimeZone.getTimeZone(fieldValue);

        List<TimeZone> projTzs =
                NewProjectModule.getTimezones(cycle);

        if (!projTzs.contains(tz)) {
            throw new ValidationException("Invalid choice");
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new ObjectStringContainer<>(tz, tz.getDisplayName(userLocale));
    }

    private ObjectStringContainer<Locale> getProjectLocale(RequestCycle cycle, String fieldValue) {
        Locale locale = HybridLocaleUtils.toLocale(fieldValue);

        List<Locale> locales =
                NewProjectModule.getProjectLocales(cycle);

        if (!locales.contains(locale)) {
            throw new ValidationException("Invalid choice");
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new ObjectStringContainer<>(locale, locale.getDisplayName(userLocale));
    }

    private ObjectStringContainer<Locale> getProjectCountry(RequestCycle cycle, String fieldValue) {
        Locale locale = HybridLocaleUtils.toLocale(fieldValue);

        List<Locale> locales =
                NewProjectModule.getProjectCountries(cycle);

        if (!locales.contains(locale)) {
            throw new ValidationException("Invalid choice");
        }

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        return new ObjectStringContainer<>(locale, locale.getDisplayCountry(userLocale));
    }

    private final class ValidationException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        public final String message;

        public ValidationException(String message) {
            this.message = message;
        }
    }

    private static final class ObjectStringContainer<T> {

        private final T obj;
        private final String value;

        public ObjectStringContainer(T obj, String value) {
            this.obj = obj;
            this.value = value;
        }

        public T getObj() {
            return obj;
        }

        public String getValue() {
            return value;
        }
    }
}
