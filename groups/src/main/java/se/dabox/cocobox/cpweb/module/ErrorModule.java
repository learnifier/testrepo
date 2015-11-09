/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.freemarker.FreemarkerRequestTarget;
import org.apache.http.HttpStatus;
import se.dabox.cocobox.cpweb.module.core.AbstractModule;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/error")
public class ErrorModule extends AbstractModule {

    @WebAction
    public RequestTarget onAccessDenied(RequestCycle cycle) {

        Map<String, Object> map = createMap();
        map.put("link", cycle.getRequest().getContextPath());

        cycle.getResponse().setStatus(HttpStatus.SC_FORBIDDEN);

        FreemarkerRequestTarget target
                = new FreemarkerRequestTarget("/error/accessDenied.html", map);
        //TODO: Disable ETag support

        return target;
    }

}
