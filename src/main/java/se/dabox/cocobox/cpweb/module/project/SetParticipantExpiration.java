/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.roster.PermissionListformCommand;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.permission.Permission;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.update.UpdateParticipationRequestBuilder;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.webutils.listform.ListformContext;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class SetParticipantExpiration extends AbstractRosterListCommand implements
        PermissionListformCommand<Long> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SetParticipantExpiration.class);

    private static final long serialVersionUID = 1L;

    public SetParticipantExpiration() {
    }

    @Override
    public RequestTarget execute(ListformContext context, List<Long> values) {
        Date expirationDate = getExpirationDate(context);
        saveDate(context, expirationDate);

        return super.execute(context, values);
    }

    private void saveDate(ListformContext context, Date expirationDate) {
        context.setAttribute("expDate", expirationDate);
    }

    @Override
    protected void executeSingle(ListformContext context, Long value) {
        CocoboxCoordinatorClient ccbc =
                CacheClients.getClient(context.getCycle(), CocoboxCoordinatorClient.class);

        ProjectParticipation part = ccbc.getProjectParticipation(value);

        if (part == null) {
            return;
        }

        long caller = LoginUserAccountHelper.getCurrentCaller(context.getCycle());

        UpdateParticipationRequestBuilder builder
                = new UpdateParticipationRequestBuilder(caller, value);
        builder.setExpiration(getDate(context));

        ccbc.updateProjectParticipation(builder.buildUpdateParticipationRequest());
    }

    private Date getExpirationDate(ListformContext context) {
        TimeZone tz = getProjectTimeZone(context);

        WebRequest req = context.getCycle().getRequest();
        String strDate = DruwaParamHelper.getMandatoryParam(LOGGER, req, "expirationdate");

        FastDateFormat fdf = FastDateFormat.getInstance(DateFormatUtils.ISO_DATE_FORMAT.getPattern(), 
                tz);

        try {
            Date date = fdf.parse(strDate);
            return date;
        } catch (ParseException ex) {
            throw new RetargetException(new ErrorCodeRequestTarget(400, "Invalid expirationdate"));
        }
    }

    private TimeZone getProjectTimeZone(ListformContext context) {
        OrgProject project = getProject(context);
        return project.getTimezone();
    }

    private Date getDate(ListformContext context) {
         return context.getAttribute("expDate", Date.class);
    }

    @Override
    public List<Permission> getPermissionsRequired() {
        return Collections.singletonList(CocoboxPermissions.PRJ_CHANGE_EXPIRATION);
    }


}
