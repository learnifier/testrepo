/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.druwa;

import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RetargetException;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.event.RequestBeginEventListener;
import net.unixdeveloper.druwa.request.RedirectUrlRequestTarget;
import net.unixdeveloper.druwa.util.UrlBuilder;
import se.dabox.service.common.context.Configuration;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.webfeature.WebFeatures;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class AlternativeCpwebListener implements RequestBeginEventListener {

    @Override
    public void onRequestBegin(ServiceRequestCycle cycle) {
        if (!(cycle instanceof RequestCycle)) {
            return;
        }

        RequestCycle reqCycle = (RequestCycle) cycle;

        Configuration config = DwsRealmHelper.getRealmConfiguration(cycle);

        if (config == null) {
            return;
        }

        if (WebFeatures.getFeatures(cycle).hasFeature("altcpweb")) {
            String base = config.getValue("cpweb-alt.baseurl");
            
            final WebRequest request = reqCycle.getRequest();
            String newUrl = base + request.getRequestPath();

            UrlBuilder ub = new UrlBuilder(newUrl);
            ub.addParameters(request.getParameterMap());

            String target = ub.toString();

            throw new RetargetException(new RedirectUrlRequestTarget(target));
        }
    }

}
