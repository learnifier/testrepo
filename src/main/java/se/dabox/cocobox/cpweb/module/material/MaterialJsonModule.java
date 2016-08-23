/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.material;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
import net.unixdeveloper.druwa.request.StringRequestTarget;
import org.apache.commons.lang3.StringUtils;
import se.dabox.cocobox.cpweb.formdata.material.AddLinkCreditsForm;
import se.dabox.cocobox.cpweb.module.OrgMaterialJsonModule;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.dws.client.langservice.LangBundle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.AllocationFailureException;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.org.AddOrgProductLinkRequest;
import se.dabox.service.common.ccbc.org.AddOrgProductLinkTokenRequest;
import se.dabox.service.common.ccbc.org.AddOrgProductLinkTokenResponse;
import se.dabox.service.common.ccbc.org.OrgProduct;
import se.dabox.service.common.ccbc.org.OrgProductLink;
import se.dabox.service.common.ccbc.org.OrgProductLinkToken;
import se.dabox.service.common.ccbc.org.OrgProductLinkTokenPredicates;
import se.dabox.service.common.ccbc.org.UpdateOrgProductLinkRequest;
import se.dabox.service.proddir.data.ProductType;
import se.dabox.service.proddir.data.ProductTypes;
import se.dabox.service.tokenmanager.client.AccountBalance;
import se.dabox.service.tokenmanager.client.TokenManagerClient;
import se.dabox.service.webutils.druwa.FormbeanJsRequestTargetFactory;
import se.dabox.service.webutils.freemarker.text.LangServiceClientFactory;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.NotPredicate;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/material.json")
public class MaterialJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onListRealmProductTypes(RequestCycle cycle) {
        ProductTypes types = getProductDirectoryClient(cycle).listTypes();
        final Map<String, String> productTypes = types.getProductTypes().stream().collect(Collectors.toMap(
                p -> p.getId().getId(),
                ProductType::getTitle));
        return jsonTarget(productTypes);
    }
}
