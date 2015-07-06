/*
* (c) Dabox AB 2015 All Rights Reserved
*/
package se.dabox.cocobox.cpweb.module.cug;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.List;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.user.UserModule;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.org.OrgRoleName;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.json.JsonEncoding;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
@WebModuleMountpoint("/cug.json")
public class ClientUserGroupJsonModule extends AbstractJsonAuthModule {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientUserGroupJsonModule.class);
    
    
    @WebAction
    public RequestTarget onListClientUserGroups(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        
        List<ClientUserGroup> cugs = getClientUserGroupService(cycle).listGroups(org.getId());
        
        return jsonTarget(toJsonUserClientUserGroups(cycle, cugs));
    }
    
    @WebAction
    public RequestTarget onListClientUserGroupChildren(RequestCycle cycle, String strOrgId, String strCugId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        
        List<ClientUserGroup> cugs = getClientUserGroupService(cycle).listChildren(Long.parseLong(strCugId));
        
        return jsonTarget(toJsonUserClientUserGroups(cycle, cugs));
    }
    
    
    @WebAction
    public RequestTarget onListClientUserGroupMembers(RequestCycle cycle, String strOrgId, String strCugId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);
        
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        List<UserAccount> members = cugService.listGroupMembers(Long.valueOf(strCugId));
        
        return jsonTarget(toJsonUserAccounts(cycle, members, org.getId()));
    }
    
    
    private UserAccount getUserAccount(RequestCycle cycle, long userId) {
        return getUserAccountService(cycle).getUserAccount(userId);
    }
    
    private UserAccountService getUserAccountService(RequestCycle cycle) {
        UserAccountService uaClient = CacheClients.getClient(cycle, UserAccountService.class);
        
        return uaClient;
    }
    
    private ClientUserGroupClient getClientUserGroupService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, ClientUserGroupClient.class);
    }


    
    private byte[] toJsonUserClientUserGroups(final RequestCycle cycle,
            final List<ClientUserGroup> cugs) {
        
        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();
                generator.writeArrayFieldStart("aaData");
                
                for (ClientUserGroup cug : cugs) {
                    generator.writeStartObject();
                    
                    generator.writeNumberField("groupId", cug.getGroupId());
                    generator.writeStringField("name", StringUtils.trimToEmpty(cug.getName()));
                    generator.writeNumberField("orgId", cug.getOrgId());
                    writeLongNullField(generator, "parent", cug.getParent());
                    writeDateField(generator, "created", cug.getCreated());
                    generator.writeNumberField("createdBy", cug.getCreatedBy());
                    writeDateField(generator, "updated", cug.getUpdated());
                    writeLongNullField(generator, "updatedBy", cug.getUpdatedBy());
                    
                    generator.writeEndObject();
                }
                
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }.encode();
    }
    
    // TODO: This is a copy from CpJsonModule, refactor
    private byte[] toJsonUserAccounts(final RequestCycle cycle,
            final List<UserAccount> uas, final long orgId) {
        
        final String orgRoleName = OrgRoleName.forOrg(orgId).toString();
        
        final MiniUserAccountHelper accHelper = new MiniUserAccountHelper(cycle);
        
        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();
                generator.writeArrayFieldStart("aaData");
                
                for (UserAccount userAccount : uas) {
                    generator.writeStartObject();
                    
                    generator.writeNumberField("uid", userAccount.getUserId());
                    String name = StringUtils.trimToEmpty(userAccount.getDisplayName());
                    generator.writeStringField("name", name);
                    generator.writeStringField("email", userAccount.
                            getPrimaryEmail());
                    generator.writeStringField("link",
                            cycle.urlFor(UserModule.class, "overview",
                                    Long.toString(orgId),
                                    Long.toString(userAccount.getUserId())));
                    
                    generator.writeStringField("imagelink24", accHelper.createInfo(userAccount).
                            getThumbnail());
                    
                    generator.writeStringField("role", userAccount.
                            getProfileValue(CocoSiteConstants.UA_PROFILE,
                                    orgRoleName));
                    
                    generator.writeEndObject();
                }
                
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }.encode();
    }
    
    
    
}
