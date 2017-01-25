package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.slf4j.Logger;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.login.client.UserAccount;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public abstract class AbstractProjectJsModule extends AbstractJsonAuthModule {
    protected void checkPermission(RequestCycle cycle, OrgProject project, String strProjectId, Logger log) {
        if (project == null) {
            log.warn("Project {} doesn't exist.", strProjectId);

            ErrorCodeRequestTarget error
                    = new ErrorCodeRequestTarget(HttpServletResponse.SC_NOT_FOUND);

            throw new RetargetException(error);
        } else {
            checkPermission(cycle, project);
        }
    }

    protected Map<Long, UserAccount> getUserAccountMap(RequestCycle cycle, List<Long> userIds) {
        List<UserAccount> userAccounts = getUserAccountServiceClient(cycle).getUserAccounts(userIds);
        return userAccounts.stream().collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));
    }

}
