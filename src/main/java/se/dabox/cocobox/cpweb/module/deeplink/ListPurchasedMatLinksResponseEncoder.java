/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.deeplink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import net.unixdeveloper.druwa.RequestCycle;
import org.codehaus.jackson.JsonGenerator;
import se.dabox.cocosite.date.DatePickerDateConverter;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.service.common.ccbc.org.OrgProductLink;
import se.dabox.service.webutils.json.DataTablesJson;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class ListPurchasedMatLinksResponseEncoder {

    static ByteArrayOutputStream encode(RequestCycle cycle, final List<OrgProductLink> links,
            final String deliveryBase) {
        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);
        DateFormat dateFormat =
                DateFormat.getDateInstance(DateFormat.FULL, userLocale);

        return new DataTablesJson<OrgProductLink>(dateFormat) {
            @Override
            protected void encodeItem(OrgProductLink item) throws IOException {
                JsonGenerator g = generator;

                g.writeNumberField("opid", item.getOrgProductId());
                g.writeNumberField("linkid", item.getLinkId());
                g.writeBooleanField("defaultLink", item.isDefaultLink());
                g.writeBooleanField("autoAdd", item.isAutoAdd());
                g.writeBooleanField("active", item.isActive());
                writeDateField("activeTo", item.getActiveTo());
                g.writeStringField("activeto", DatePickerDateConverter.toDatePickerDate(item.
                        getActiveTo()));
                g.writeStringField("link", deliveryBase.concat(item.getCdLinkId()));
                g.writeNumberField("balance", item.getBalance());
                g.writeStringField("title", item.getTitle());
            }
        }.encodeToStream(links);
    }
}
