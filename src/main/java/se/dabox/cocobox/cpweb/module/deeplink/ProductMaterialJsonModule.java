/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.deeplink;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.formbean.DruwaFormValidationSession;
import net.unixdeveloper.druwa.formbean.validation.ValidationConstraint;
import net.unixdeveloper.druwa.formbean.validation.ValidationError;
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
import se.dabox.service.tokenmanager.client.AccountBalance;
import se.dabox.service.tokenmanager.client.TokenManagerClient;
import se.dabox.service.webutils.druwa.FormbeanJsRequestTargetFactory;
import se.dabox.service.webutils.freemarker.text.LangServiceClientFactory;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.NotPredicate;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/pmaterial.json")
public class ProductMaterialJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onListPurchasedMatLinks(RequestCycle cycle, String strOrgId)
            throws Exception {

        checkOrgPermission(cycle, strOrgId);
        
        long opid = Long.valueOf(cycle.getRequest().getParameter("opid"));
        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        List<OrgProductLink> links = ccbc.getOrgProductLinks(opid);

        //Add a link so there is always at least one in the list
        if (links.isEmpty()) {
            long userId = LoginUserAccountHelper.getUserId(cycle);
            ccbc.addOrgProductLink(new AddOrgProductLinkRequest(userId, opid));
            links = ccbc.getOrgProductLinks(opid);
        }

        String deliveryBase = OrgMaterialJsonModule.getDeliveryBase(cycle);
        return jsonTarget(ListPurchasedMatLinksResponseEncoder.encode(cycle, links, deliveryBase));
    }

    @WebAction
    public RequestTarget onChangeLinkTitle(RequestCycle cycle) {
        Long linkid = Long.valueOf(cycle.getRequest().getParameter("linkid"));
        String title = StringUtils.trimToNull(cycle.getRequest().getParameter("title"));
        
        if (title == null) {
            return jsonTarget(Collections.singletonMap("status", "OK"));
        }

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateOrgProductLinkRequest update = new UpdateOrgProductLinkRequest(userId, linkid);
        update.setTitle(title);
        getCocoboxCordinatorClient(cycle).updateOrgProductLink(update);

        return null;
    }

    @WebAction
    public RequestTarget onChangeLinkStatus(RequestCycle cycle) {
        //There's an action like this for orgmats too
        Long linkid = Long.valueOf(cycle.getRequest().getParameter("linkid"));
        boolean active = Boolean.valueOf(cycle.getRequest().getParameter("active"));

        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateOrgProductLinkRequest update = new UpdateOrgProductLinkRequest(userId, linkid);
        update.setActive(active);
        getCocoboxCordinatorClient(cycle).updateOrgProductLink(update);
        OrgProductLink link = getCocoboxCordinatorClient(cycle).getOrgProductLink(linkid);

        Map<String, Object> map = createMap();

        map.put("status", "OK");
        map.put("active", active);
        map.put("opid", link.getOrgProductId());

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onChangeAutoAddStatus(RequestCycle cycle) {
        //There's an action like this for orgmats too
        Long linkid = Long.valueOf(cycle.getRequest().getParameter("linkid"));
        boolean autoadd = Boolean.valueOf(cycle.getRequest().getParameter("autoadd"));

        long userId = LoginUserAccountHelper.getUserId(cycle);
        OrgProductLink link = getCocoboxCordinatorClient(cycle).getOrgProductLink(linkid);

        if (!link.isDefaultLink()) {
            Map<String, Object> map = createMap();
            map.put("status", "notdefault");

            return jsonTarget(map);
        }

        UpdateOrgProductLinkRequest update = new UpdateOrgProductLinkRequest(userId, linkid);
        update.setAutoAdd(autoadd);
        getCocoboxCordinatorClient(cycle).updateOrgProductLink(update);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onAddCredits(RequestCycle cycle) {

        DruwaFormValidationSession<AddLinkCreditsForm> formsess =
                getValidationSession(AddLinkCreditsForm.class, cycle);

        LangBundle langBundle = getLangBundle(cycle, CocoSiteConstants.DEFAULT_LANG_BUNDLE);
        final FormbeanJsRequestTargetFactory formbeanResp =
                new FormbeanJsRequestTargetFactory(cycle, langBundle, "cpweb");

        if (!formsess.process()) {
            return formbeanResp.
                    getRequestTarget(formsess);
        }

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        long linkId = formsess.getObject().getOplid();
        long amount = formsess.getObject().getCredits();

        AllocationFailureException afex = null;
        
        AddOrgProductLinkTokenResponse addResp = null;

        try {
            addResp = ccbc.addOrgProductLinkToken(
                    new AddOrgProductLinkTokenRequest(userId, linkId, amount));
        } catch (AllocationFailureException ex) {
            afex = ex;
        }

        if (afex != null || !addResp.isSuccessful()) {
            formsess.addError(new ValidationError(ValidationConstraint.TOO_LARGE, "credits",
                    "credit.overdraft"));

            return formbeanResp.getRequestTarget(formsess);
        }

        Map<String, Object> map = new HashMap<String, Object>();

        OrgProductLink mainLink = ccbc.getOrgProductLink(linkId);

        List<OrgProductLink> allLinks = ccbc.getOrgProductLinks(mainLink.getOrgProductId());

        map.put("balance", mainLink.getBalance());

        long totalBalance = 0;
        for (OrgProductLink link : allLinks) {
            totalBalance += link.getBalance();
        }

        map.put("totalBalance", totalBalance);

        map.put("opid", mainLink.getOrgProductId());
        map.put("linkid", mainLink.getLinkId());

        return formbeanResp.getSuccessfulMap(map);
    }

    @WebAction
    public RequestTarget onChangeLinkActiveTo(RequestCycle cycle) {
        //There's an action like this for orgmats too
        Long linkid = Long.valueOf(cycle.getRequest().getParameter("linkid"));
        Date activeTo = OrgMaterialJsonModule.getActiveTo(cycle);

        if (activeTo == null) {
            Map<String, Object> map = createMap();
            map.put("status", "ERROR_INPUT");
            return jsonTarget(map);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(activeTo);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);


        long userId = LoginUserAccountHelper.getUserId(cycle);
        UpdateOrgProductLinkRequest update = new UpdateOrgProductLinkRequest(userId, linkid);
        update.setActiveTo(cal.getTime());
        getCocoboxCordinatorClient(cycle).updateOrgProductLink(update);

        Map<String, Object> map = createMap();
        map.put("status", "OK");

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onNewProdLink(RequestCycle cycle, String strOrgId) {
        long orgmatId = Long.valueOf(cycle.getRequest().getParameter("orgmatid"));

        checkOrgPermission(cycle, strOrgId);

        final CocoboxCoordinatorClient client = getCocoboxCordinatorClient(cycle);

        long userId = getCurrentUser(cycle);

        long linkId = client.addOrgProductLink(new AddOrgProductLinkRequest(userId, orgmatId));

        OrgProductLink link = client.getOrgProductLink(linkId);
        List<OrgProductLink> links = Collections.singletonList(link);

        String deliveryBase = OrgMaterialJsonModule.getDeliveryBase(cycle);

        return jsonTarget(ListPurchasedMatLinksResponseEncoder.encode(cycle, links, deliveryBase));
    }

    @WebAction
    public RequestTarget onDeleteLink(RequestCycle cycle, String strOrgId) {
        long prodlink = Long.valueOf(cycle.getRequest().getParameter("prodlink"));

        checkOrgPermission(cycle, strOrgId);

        final CocoboxCoordinatorClient client = getCocoboxCordinatorClient(cycle);

        long userId = getCurrentUser(cycle);

        OrgProductLink link = client.getOrgProductLink(prodlink);

        if (link.isDefaultLink()) {
            return singleJsonResponse("error", "Default link can't be removed");
        } else if (link.getBalance() != 0) {
            return singleJsonResponse("error", "Unable to delete link with assigned credits");
        }

        client.deleteOrgProductLink(userId, prodlink);


        Map<String, Object> map = createMap();
        map.put("status", "OK");
        map.put("opid", link.getOrgProductId());

        return jsonTarget(map);
    }

    @WebAction
    public RequestTarget onListLinkTokens(RequestCycle cycle, String strOrgProductLinkId) {

        long orgProductLinkId = Long.parseLong(strOrgProductLinkId);

        List<OrgProductLinkToken> allTokens =
                getCocoboxCordinatorClient(cycle).getOrgProductLinkTokens(orgProductLinkId);

        List<OrgProductLinkToken> liveTokens = CollectionsUtil.sublist(allTokens,
                new NotPredicate<OrgProductLinkToken>(OrgProductLinkTokenPredicates.
                getDeletedPredicate()));

        ByteArrayOutputStream baos = OrgProductLinkTokenJson.encode(cycle, liveTokens);

        return jsonTarget(baos);
    }

    @WebAction
    public RequestTarget onRemoveLinkToken(RequestCycle cycle, String strOrgProductLinkId, String tokenId) {
        long orgProductLinkId = Long.parseLong(strOrgProductLinkId);

        List<OrgProductLinkToken> tokens =
                getCocoboxCordinatorClient(cycle).getOrgProductLinkTokens(orgProductLinkId);

        final long linkTokenId = Long.valueOf(tokenId);

        for (Iterator<OrgProductLinkToken> it = tokens.iterator(); it.hasNext();) {
            OrgProductLinkToken token = it.next();
            if (token.getLinkTokenId() == linkTokenId) {
                releaseLinkToken(cycle, token);
                it.remove();
                break;
            }
        }

        ByteArrayOutputStream baos = OrgProductLinkTokenJson.encode(cycle, tokens);

        return jsonTarget(baos);
    }

    @WebAction
    public RequestTarget onProductBalance(RequestCycle cycle, String strOrgId, String strOrgProdId) {
        final long orgId = Long.valueOf(strOrgId);

        checkOrgPermission(cycle, orgId);

        long orgprodId = Long.valueOf(strOrgProdId);

        final CocoboxCoordinatorClient client = getCocoboxCordinatorClient(cycle);

        List<OrgProduct> orgProds = client.listOrgProducts(orgId);

        OrgProduct orgProd = null;

        for (OrgProduct op : orgProds) {
            if (op.getOrgProductId() == orgprodId) {
                orgProd = op;
                break;
            }
        }

        AccountBalance balance =
                CacheClients.getClient(cycle, TokenManagerClient.class).getAccountBalance(orgProd.
                getTokenManagerAccountId());

        Map<String, Object> responseMap = createMap();
        responseMap.put("available", balance.getAvailable());
        responseMap.put("expired", balance.getExpired());
        responseMap.put("used", balance.getUsed());

        return jsonTarget(responseMap);
    }

    private RequestTarget singleJsonResponse(String name,
            String value) {
        Map<String, String> map = Collections.singletonMap(name, value);

        return jsonTarget(map);
    }

    private LangBundle getLangBundle(RequestCycle cycle, String bundleName) {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        return LangServiceClientFactory.getInstance(cycle).getLangBundle(bundleName, userLocale.
                toString(),
                true);
    }

    private void releaseLinkToken(RequestCycle cycle, OrgProductLinkToken token) {
        final long userId = LoginUserAccountHelper.getUserId(cycle);
        getCocoboxCordinatorClient(cycle).deleteOrgProductLinkToken(userId, token.getLinkTokenId());
    }
}
