/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.AroundInvoke;
import net.unixdeveloper.druwa.module.InvocationContext;
import net.unixdeveloper.druwa.request.BytesRequestTarget;
import net.unixdeveloper.druwa.request.StringRequestTarget;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.PrettyPrinter;
import org.codehaus.jackson.map.ObjectMapper;
import se.dabox.cocobox.cpweb.JsonEncodingException;
import se.dabox.cocosite.security.CocoboxSecurityConstants;
import se.dabox.cocosite.security.UserRoleCheckAfterLoginListener;
import se.dabox.service.common.context.DwsExecutionContext;
import se.dabox.service.common.context.DwsExecutionContextHelper;
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
            return new StringRequestTarget("text/json", jsonString);
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
            return new StringRequestTarget("text/json", jsonString);
        } catch (Exception ex) {
            throw new JsonEncodingException("Failed to encode list: "+list, ex);
        }
    }

    protected static RequestTarget jsonTarget(ByteArrayOutputStream baos) {
        byte[] data = baos.toByteArray();

        return jsonTarget(data);
    }

    protected static RequestTarget jsonTarget(byte[] data) {
        String ct = "text/json";

        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();
        if (cycle.getRequest().getParameter("txt") != null) {
            ct = "text/plain;charset=UTF-8";
        }
        return new BytesRequestTarget(data, ct);
    }

    protected static void writeLongNullField(JsonGenerator generator, String name,
            Long value) throws IOException {
        if (value == null) {
            generator.writeNullField(name);
        } else {
            generator.writeNumberField(name, value);
        }
    }
}
