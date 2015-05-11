/*
* (c) Dabox AB 2013 All Rights Reserved
*/
package se.dabox.cocobox.cpweb.module.papi;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebRequest;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import net.unixdeveloper.druwa.request.ErrorCodeRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.druwa.DruwaParamHelper;
import static se.dabox.cocosite.module.core.AbstractCocositeJsModule.jsonTarget;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.service.client.CacheClients;
import se.dabox.service.papi.client.NotFoundException;
import se.dabox.service.papi.client.PapiScope;
import se.dabox.service.papi.client.PartnerId;
import se.dabox.service.papi.client.PublicApiKeyAdminClient;
import se.dabox.service.papi.client.PublicApiKeyPair;
import se.dabox.service.papi.client.PublicApiKeyPairField;
import se.dabox.service.papi.client.PublicApiPartner;
import se.dabox.service.papi.client.UpdatePublicApiKeyPairRequest;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/papi.json")
public class PapiJsonModule extends AbstractJsonAuthModule {
    
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PapiJsonModule.class);
        
    @WebAction
    public RequestTarget onListApiKeys(final RequestCycle cycle, final String strOrgId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_VIEW_APIKEY);

        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }

        
        final long userId = LoginUserAccountHelper.getUserId(cycle);
        
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);
        PartnerId pid = getOrCreatePartnerId(userId, orgId, pc);
        List<PublicApiKeyPair> partnerKeyPairs = pc.getPartnerKeyPairs(pid);
        
        return jsonTarget(toJsonPublicApiKeyPairs(cycle, partnerKeyPairs));
    }
    
    @WebAction
    public RequestTarget onCreateApiKeyPair(final RequestCycle cycle, final String strOrgId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_CREATE_APIKEY);

        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }


        final long userId = LoginUserAccountHelper.getUserId(cycle);
        WebRequest webReq = cycle.getRequest();
        
        String name = webReq.getParameter("name");
        
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);
        PartnerId pid = getOrCreatePartnerId(userId, orgId, pc);

        PublicApiKeyPair keyPair = pc.createApiKeyPair(userId, pid, name);

        return jsonTarget(toJsonPublicApiKeyPair(cycle, keyPair));
    }

        @WebAction
    public RequestTarget onUpdateApiKeyPairName(final RequestCycle cycle, final String strOrgId, final String apiKeyPairId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_CREATE_APIKEY); // EDIT?

        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }
        
        WebRequest webReq = cycle.getRequest();
        final long userId = LoginUserAccountHelper.getUserId(cycle);

        Long apiKeyPairIdLong;
        try {
             apiKeyPairIdLong = Long.parseLong(apiKeyPairId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }
        
        
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, webReq, "name");
      
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);

        // Check that key beloongs to orgId/userId's partner.
        if(verifyPartner(userId, orgId, apiKeyPairIdLong, pc)) {
            UpdatePublicApiKeyPairRequest papiRequest = new UpdatePublicApiKeyPairRequest(EnumSet.of(PublicApiKeyPairField.NAME), userId, apiKeyPairIdLong, name);
            pc.updateApiKeyPair(papiRequest);
            return jsonTarget(Collections.singletonMap("success", true));
        } else {
            return new ErrorCodeRequestTarget(404); // Not found
        }
    }

    @WebAction
    public RequestTarget onDeleteApiKeyPair(final RequestCycle cycle, final String strOrgId, final String apiKeyPairId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_DELETE_APIKEY);
        
        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }

        final long userId = LoginUserAccountHelper.getUserId(cycle);
        Long apiKeyPairIdLong;
        try {
             apiKeyPairIdLong = Long.parseLong(apiKeyPairId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);
        
        // Check that key beloongs to orgId/userId's partner.
        if(verifyPartner(userId, orgId, apiKeyPairIdLong, pc)) {
            pc.deleteApiKeyPair(userId, apiKeyPairIdLong);
            return jsonTarget(Collections.singletonMap("success", true));
        } else {
            return new ErrorCodeRequestTarget(404); // Not found
        }
    }

    @WebAction
    public RequestTarget onGetApiKeyPairSecret(final RequestCycle cycle, final String strOrgId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_VIEW_APIKEY_SECRET);
        
        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }

        final WebRequest webReq = cycle.getRequest();
                
        final String publicKey = DruwaParamHelper.getMandatoryParam(LOGGER, webReq, "publicKey");

        
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);

        PublicApiKeyPair keyPair = pc.getPartnerKeyPair(publicKey);

        // Before returning, verify that the key belongs to userId/orgId's partner.
        PapiScope papiScope = PapiScope.newOrgUnitScope(orgId);
        PublicApiPartner partnerInfo = pc.getPartnerInfo(papiScope);
        if(partnerInfo.getId() == keyPair.getPartner().getId()) {
            return jsonTarget(Collections.singletonMap("secretKey", keyPair.getSecretKey()));
        } else {
            return new ErrorCodeRequestTarget(404); // Not found
        }
    }
    
    private PartnerId getOrCreatePartnerId(long userId, long orgId, final PublicApiKeyAdminClient pc) {
        PapiScope papiScope = PapiScope.newOrgUnitScope(orgId);
        PublicApiPartner partnerInfo;
        try {
            partnerInfo = pc.getPartnerInfo(papiScope);
        } catch(NotFoundException e) {
            partnerInfo = pc.createApiPartner(userId, papiScope);
        }
        return PartnerId.valueOf(partnerInfo.getId());
    }
 
    private boolean verifyPartner(final long userId, long orgId, final long keyId, final PublicApiKeyAdminClient pc) {
        // Instead of reading all keys for a partner, I could read only the interesting id if we had a pc.getPartnerKeyPair(keyId).
        PartnerId pid = getOrCreatePartnerId(userId, orgId, pc);
        List<PublicApiKeyPair> partnerKeyPairs = pc.getPartnerKeyPairs(pid);
        return partnerKeyPairs.stream().anyMatch((keyPair) -> (keyPair.getId() == keyId));
    }
    
    private byte[] toJsonPublicApiKeyPairs(final RequestCycle cycle,
            final List<PublicApiKeyPair> entries) {
        
        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();
                generator.writeArrayFieldStart("data");
                for (PublicApiKeyPair e : entries) {
                    // Note: Do NOT include secretKey here.
                    generator.writeStartObject();
                    generator.writeNumberField("id", e.getId());
                    generator.writeStringField("name", e.getName());
                    generator.writeStringField("publicKey", e.getPublicKey());
                    generator.writeNumberField("partnerId", e.getPartner().getId());
                    generator.writeEndObject();
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }.encode();
    }
    

    private byte[] toJsonPublicApiKeyPair(final RequestCycle cycle,
            final PublicApiKeyPair keyPair) {
        
        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                    // Note: Do NOT include secretKey here.
                    generator.writeStartObject();
                    generator.writeNumberField("id", keyPair.getId());
                    generator.writeStringField("name", keyPair.getName());
                    generator.writeStringField("publicKey", keyPair.getPublicKey());
                    generator.writeNumberField("partnerId", keyPair.getPartner().getId());
                    generator.writeEndObject();
            }
        }.encode();
    }
}
