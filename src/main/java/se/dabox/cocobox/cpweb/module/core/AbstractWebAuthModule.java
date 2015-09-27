/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.core;

import java.util.Locale;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.HttpMethod;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.annotation.AroundInvoke;
import net.unixdeveloper.druwa.module.InvocationContext;
import se.dabox.cocobox.cpweb.FragmentInitializer;
import se.dabox.cocobox.security.CocoboxSecurityConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.login.LoginHandler;
import se.dabox.cocosite.security.CocositeLoginChecker;
import se.dabox.cocosite.security.UserRoleCheckAfterLoginListener;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.coursedesign.CourseDesign;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.webutils.login.WebLoginCheck;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public abstract class AbstractWebAuthModule extends AbstractAuthModule {

    private final WebLoginCheck loginChecker;

    public AbstractWebAuthModule() {
        this.loginChecker = new CocositeLoginChecker();
        this.loginChecker.
                addRequestSecurityCheck(new UserRoleCheckAfterLoginListener(
                CocoboxSecurityConstants.USER_ROLE));


        LoginHandler handler = DruwaApplication.get().getAttribute(
                LoginHandler.ATTRIBUTE_NAME);

        if (handler == null) {
            throw new IllegalStateException(
                    "No login handler stored as applicationAttribute "
                    + LoginHandler.ATTRIBUTE_NAME);
        }

        loginChecker.addAfterLoginListener(handler);
    }

    protected WebLoginCheck getLoginChecker() {
        return loginChecker;
    }

    @AroundInvoke(order = 500)
    public Object loginCheck(InvocationContext ctx) {
        return loginChecker.loginCheck(ctx);
    }

    @AroundInvoke(order = 600)
    public Object setUserLocale(InvocationContext ctx) {
        Locale locale =
                CocositeUserHelper.getUserLocale(ctx.getRequestCycle());

        ctx.getRequestCycle().getResponse().setLocale(locale);

        return ctx.proceed();
    }

    @Override
    protected void checkOrgPermission(RequestCycle cycle, String strOrgId) {
        super.checkOrgPermission(cycle, strOrgId);
        checkAndCreateOrgFragments(cycle, Long.valueOf(strOrgId));
    }

    private void checkAndCreateOrgFragments(RequestCycle cycle, long orgId) {
        //Do net check fragments unless we are running a GET
        if (!HttpMethod.GET.equals(cycle.getRequest().getMethod())) {
            return;
        }

        if (!FragmentInitializer.hasOrgFragments(cycle, orgId)) {
            FragmentInitializer fi = new FragmentInitializer();
            fi.initFragments(cycle, orgId);
//            String url = cycle.getRequest().getRequestUrl().toString();
//            throw new RetargetException(new RedirectUrlRequestTarget(url));
        }
    }

    protected CourseDesignDefinition getProjectCourseDesignDefinition(RequestCycle cycle,
            OrgProject project) {
        ParamUtil.required(project,"project");

        if (project.getDesignId() == null) {
            return null;
        }

        CourseDesignClient cdClient =
                CacheClients.getClient(cycle, CourseDesignClient.class);

        CourseDesign design = cdClient.getDesign(project.getDesignId());

        if (design == null) {
            return null;
        }

        return CddCodec.decode(cycle, design.getDesign());
    }
}
