/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.material;

import se.dabox.cocobox.cpweb.module.deeplink.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import net.unixdeveloper.druwa.RequestCycle;
import org.codehaus.jackson.JsonGenerator;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.user.UserAccountNameHelper;
import se.dabox.service.common.ccbc.org.OrgProductLinkToken;
import se.dabox.service.webutils.json.DataTablesJson;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class OrgProductLinkTokenJson {

    static ByteArrayOutputStream encode(final RequestCycle cycle,
            List<OrgProductLinkToken> tokens) {

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, userLocale);

        final UserAccountNameHelper nameHelper = new UserAccountNameHelper(cycle);

        return new DataTablesJson<OrgProductLinkToken>(df) {
            @Override
            protected void encodeItem(OrgProductLinkToken item) throws IOException {
                JsonGenerator g = generator;
                g.writeNumberField("amount", item.getAmount());
                writeDateField("created", item.getCreated());
                g.writeStringField("createdBy", nameHelper.getName(item.getCreatedBy()));
                writeDateField("deleted", item.getDeleted());
                g.writeStringField("deletedBy", nameHelper.getName(item.getDeletedBy()));
                g.writeNumberField("linkTokenId", item.getLinkTokenId());
                writeDateField("updated", item.getUpdated());
                g.writeStringField("updatedBy", nameHelper.getName(item.getUpdatedBy()));
                final String deleteLink = cycle.urlFor(ProductMaterialJsonModule.class, 
                        "removeLinkToken", 
                        Long.toString(item.getOrgProductLinkId()),
                        Long.toString(item.getLinkTokenId()));
                
                g.writeStringField("deleteLink", deleteLink);
            }
        }.encodeToStream(tokens);

    }
}
