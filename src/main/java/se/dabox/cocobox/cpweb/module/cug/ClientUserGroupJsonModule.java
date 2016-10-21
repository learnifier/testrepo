/*
* (c) Dabox AB 2015 All Rights Reserved
*/
package se.dabox.cocobox.cpweb.module.cug;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocobox.cpweb.module.user.UserModule;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.cocobox.vfs.InvalidArgumentException;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.cocosite.webmessage.WebMessage;
import se.dabox.cocosite.webmessage.WebMessageType;
import se.dabox.cocosite.webmessage.WebMessages;
import se.dabox.service.client.CacheClients;
import se.dabox.service.cug.client.ClientUserGroup;
import se.dabox.service.cug.client.ClientUserGroupClient;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;

import static com.segment.analytics.messages.Message.Type.group;

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
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);

        List<ClientUserGroup> cugs = getClientUserGroupService(cycle).listGroups(org.getId())
                .stream().filter(
                        m -> m.getParent() == null
                ).collect(Collectors.toList());
        
        return jsonTarget(toJsonUserClientUserGroups(cycle, cugs));
    }
    
    @WebAction
    public RequestTarget onListClientUserGroupChildren(RequestCycle cycle, String strCugId) {
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        final Long groupId;
        try {
            groupId = Long.valueOf(strCugId);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Invalid group id");
        }

        final ClientUserGroup group = cugService.getGroup(groupId);
        if(group == null) {
            throw new IllegalArgumentException("Can not find group" + strCugId);
        }

        final MiniOrgInfo org = secureGetMiniOrg(cycle, group.getOrgId());
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);

        List<ClientUserGroup> cugs = cugService.listChildren(Long.parseLong(strCugId));
        
        return jsonTarget(toJsonUserClientUserGroups(cycle, cugs));
    }
    
    @WebAction
    public RequestTarget onListClientUserGroupMembers(RequestCycle cycle, String strCugId) {
        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        final Long groupId;
        try {
            groupId = Long.valueOf(strCugId);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Invalid group id");
        }

        final ClientUserGroup group = cugService.getGroup(groupId);
        if(group == null) {
            throw new IllegalArgumentException("Can not find group" + strCugId);
        }

        final MiniOrgInfo org = secureGetMiniOrg(cycle, group.getOrgId());
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_VIEW_USER);


        List<UserAccount> members = cugService.listGroupMembers(Long.valueOf(strCugId));
        
        return jsonTarget(toJsonUserAccounts(cycle, members, org.getId()));
    }


    @WebAction
    public RequestTarget onRemoveClientUserGroupMembers(RequestCycle cycle, String strCugId) {
        long caller = LoginUserAccountHelper.getCurrentCaller(cycle);
        final String[] strIds = cycle.getRequest().getParameterValues("id[]");

        final Long groupId;
        try {
            groupId = Long.valueOf(strCugId);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Invalid group id");
        }

        ClientUserGroupClient cugService = getClientUserGroupService(cycle);
        final ClientUserGroup group = cugService.getGroup(groupId);
        if(group == null) {
            throw new IllegalArgumentException("Can not find group" + strCugId);
        }

        final MiniOrgInfo org = secureGetMiniOrg(cycle, group.getOrgId());
        checkOrgPermission(cycle, org.getId(), CocoboxPermissions.CP_EDIT_USER);

        List<String> succeeded = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        if(strIds != null && strIds.length != 0) {
            for(String strId: strIds) {
                try {
                    Long id = Long.valueOf(strId);
                    cugService.removeGroupMember(caller, groupId, id);
                    succeeded.add(strId);
                } catch(NumberFormatException e) {
                    failed.add(strId);
                }
            }
        }
        WebMessages webMessages = WebMessages.getInstance(cycle);
        if(succeeded.size() > 0) {
            webMessages.addMessage(WebMessage.createTextMessage("Removed " + succeeded.size() + (succeeded.size()==1?" user":" users") + " from group.", WebMessageType.info));
        }
        if(failed.size() > 0) {
            webMessages.addMessage(WebMessage.createTextMessage("Failed to remove " + failed.size() + (failed.size()==1?" user":" users") + " from group.", WebMessageType.error));
        }

        return jsonTarget(ImmutableMap.of(
                "status", "ok",
                "removed", succeeded,
                "failed", failed
        ));
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
