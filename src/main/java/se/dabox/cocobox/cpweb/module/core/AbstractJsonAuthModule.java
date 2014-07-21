/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.AroundInvoke;
import net.unixdeveloper.druwa.annotation.WebActionExceptionHandler;
import net.unixdeveloper.druwa.module.InvocationContext;
import net.unixdeveloper.druwa.request.JsonRequestTarget;
import net.unixdeveloper.druwa.request.binarysource.ByteArrayBinarySource;
import net.unixdeveloper.druwa.request.binarysource.ByteArrayOutputStreamBinarySource;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.PrettyPrinter;
import org.codehaus.jackson.map.ObjectMapper;
import se.dabox.cocobox.cpweb.JsonEncodingException;
import se.dabox.cocobox.cpweb.command.GetOrgBrandingCommand;
import se.dabox.cocosite.branding.GetCachedBrandingCommand;
import se.dabox.cocosite.branding.GetRealmBrandingId;
import se.dabox.cocosite.security.CocoboxSecurityConstants;
import se.dabox.cocosite.security.UserRoleCheckAfterLoginListener;
import se.dabox.cocosite.user.MiniUserAccountHelperContext;
import se.dabox.service.branding.client.Branding;
import se.dabox.service.common.context.DwsExecutionContext;
import se.dabox.service.common.context.DwsExecutionContextHelper;
import se.dabox.service.webutils.json.JsonExceptionHandler;
import se.dabox.service.webutils.login.WebLoginCheck;
import se.dabox.service.webutils.login.nlogin.JavascriptNewLoginChecker;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public abstract class AbstractJsonAuthModule extends AbstractAuthModule {
    private final WebLoginCheck loginChecker;

    public AbstractJsonAuthModule() {
        DwsExecutionContext context =
                DwsExecutionContextHelper.getContext();

        this.loginChecker = new JavascriptNewLoginChecker();
        this.loginChecker.
                addAfterLoginListener(new UserRoleCheckAfterLoginListener(
                CocoboxSecurityConstants.USER_ROLE));
    }

    @AroundInvoke(order=500)
    public Object loginCheck(InvocationContext ctx) {
        return loginChecker.loginCheck(ctx);
    }

    protected RequestTarget jsonTarget(Map<String, ?> map) {
        ObjectMapper mapper = new ObjectMapper();

        String jsonString;
        try {
            jsonString =
                    mapper.writer((PrettyPrinter) null).writeValueAsString(map);
            return new JsonRequestTarget(jsonString);
        } catch (Exception ex) {
            throw new JsonEncodingException("Failed to encode map: "+map, ex);
        }
    }

    protected RequestTarget jsonTarget(List<?> list) {
        ObjectMapper mapper = new ObjectMapper();

        String jsonString;
        try {
            jsonString =
                    mapper.writer((PrettyPrinter) null).writeValueAsString(list);
            return new JsonRequestTarget(jsonString);
        } catch (Exception ex) {
            throw new JsonEncodingException("Failed to encode list: "+list, ex);
        }
    }

    protected static RequestTarget jsonTarget(ByteArrayOutputStream baos) {
        return new JsonRequestTarget(new ByteArrayOutputStreamBinarySource(baos),
                JsonRequestTarget.DEFAULT_ENCODING);
    }

    protected static RequestTarget jsonTarget(byte[] data) {
        return new JsonRequestTarget(new ByteArrayBinarySource(data, false),
                JsonRequestTarget.DEFAULT_ENCODING);
    }

    protected static void writeLongNullField(JsonGenerator generator, String name,
            Long value) throws IOException {
        if (value == null) {
            generator.writeNullField(name);
        } else {
            generator.writeNumberField(name, value);
        }
    }

    @WebActionExceptionHandler
    public RequestTarget exceptionHandler(RequestCycle cycle, RequestTarget target, Exception ex) {
        return JsonExceptionHandler.exceptionHandler(cycle, target, ex);
    }

    @Override
    protected void checkOrgPermission(RequestCycle cycle, String strOrgId) {
        super.checkOrgPermission(cycle, strOrgId);
        activateOrgBranding(cycle, Long.valueOf(strOrgId));

    }

    @Override
    protected void checkOrgPermission(RequestCycle cycle, long orgId) {
        super.checkOrgPermission(cycle, orgId);
        activateOrgBranding(cycle, orgId);

    }

    private void activateOrgBranding(RequestCycle cycle, long orgId) {
        Branding branding = new GetOrgBrandingCommand(cycle).forOrg(orgId);
        if (branding != null) {
            setLetterBubbleColor(branding);
        }
    }

    private void setLetterBubbleColor(Branding branding) {
        java.awt.Color color = branding.getMetadataColor("cpPrimaryColor");
        if (color != null) {
            MiniUserAccountHelperContext.getCycleContext().setLetterBubbleBgColor(color);
        }
    }

}
