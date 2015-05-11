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
//        checkOrgPermission(cycle, strOrgId);

        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }

        
        final long userId = LoginUserAccountHelper.getUserId(cycle);
        
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);
        PartnerId pid = getOrCreatePartnerId(cycle, userId, orgId, pc);
        List<PublicApiKeyPair> partnerKeyPairs = pc.getPartnerKeyPairs(pid);
        
        return jsonTarget(toJsonPublicApiKeyPairs(cycle, partnerKeyPairs));
    }
    
    @WebAction
    public RequestTarget onCreateApiKeyPair(final RequestCycle cycle, final String strOrgId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_CREATE_APIKEY);
//        checkOrgPermission(cycle, strOrgId);

        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }


        final long userId = LoginUserAccountHelper.getUserId(cycle);
        WebRequest webReq = cycle.getRequest();
        
        String name = DruwaParamHelper.getMandatoryParam(LOGGER, webReq, "name");
      
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);
        PartnerId pid = getOrCreatePartnerId(cycle, userId, orgId, pc);

        PublicApiKeyPair keyPair = pc.createApiKeyPair(userId, pid, name);

        return jsonTarget(toJsonPublicApiKeyPair(cycle, keyPair));
    }

        @WebAction
    public RequestTarget onUpdateApiKeyPairName(final RequestCycle cycle, final String strOrgId, final String apiKeyPairId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_CREATE_APIKEY); // EDIT?
//        checkOrgPermission(cycle, strOrgId);

        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }
       // TODO: Check that the ID we are trying to manipulate belongs to orgId 

 

        
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

        UpdatePublicApiKeyPairRequest papiRequest = new UpdatePublicApiKeyPairRequest(EnumSet.of(PublicApiKeyPairField.NAME), userId, apiKeyPairIdLong, name);
        pc.updateApiKeyPair(papiRequest);

        return jsonTarget(Collections.singletonMap("success", true));
    }

    @WebAction
    public RequestTarget onDeleteApiKeyPair(final RequestCycle cycle, final String strOrgId, final String apiKeyPairId) {
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_DELETE_APIKEY);
//        checkOrgPermission(cycle, strOrgId);
        
        Long orgId;
        try {
             orgId = Long.parseLong(strOrgId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }
       // TODO: Check that the ID we are trying to manipulate belongs to orgId 

        final long userId = LoginUserAccountHelper.getUserId(cycle);
        Long apiKeyPairIdLong;
        try {
             apiKeyPairIdLong = Long.parseLong(apiKeyPairId);
        } catch (NumberFormatException ex) {
            return new ErrorCodeRequestTarget(400); // Bad request
        }
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);
        pc.deleteApiKeyPair(userId, apiKeyPairIdLong);

        return jsonTarget(Collections.singletonMap("success", true));
    }

    @WebAction
    public RequestTarget onGetApiKeyPairSecret(final RequestCycle cycle, final String strOrgId) {
        //checkOrgPermission(cycle, strOrgId);
        checkOrgPermission(cycle, strOrgId, CocoboxPermissions.BO_VIEW_APIKEY_SECRET);
        WebRequest webReq = cycle.getRequest();
        final long userId = LoginUserAccountHelper.getUserId(cycle);
                
        String publicKey = DruwaParamHelper.getMandatoryParam(LOGGER, webReq, "publicKey");

        
        PublicApiKeyAdminClient pc = CacheClients.getClient(cycle, PublicApiKeyAdminClient.class);

        PublicApiKeyPair keyPair = pc.getPartnerKeyPair(publicKey);

        // Verify partnerId from userId <-> keyPair.getPartner()
        // verify strOrgId
        return jsonTarget(Collections.singletonMap("secretKey", keyPair.getSecretKey()));
    }

    
    private PartnerId getOrCreatePartnerId(RequestCycle cycle, long userId, long orgId, final PublicApiKeyAdminClient pc) {
        PapiScope papiScope = PapiScope.newOrgUnitScope(orgId);
        PublicApiPartner partnerInfo;
        try {
            partnerInfo = pc.getPartnerInfo(papiScope);
        } catch(NotFoundException e) {
            partnerInfo = pc.createApiPartner(userId, papiScope);
        }
        return PartnerId.valueOf(partnerInfo.getId());
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
