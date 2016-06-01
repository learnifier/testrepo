/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.coursedesign;

import com.fasterxml.jackson.core.JsonGenerator;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.lang3.builder.CompareToBuilder;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import se.dabox.cocosite.coursedesign.CourseDesignThumbnail;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.service.client.Clients;
import se.dabox.service.common.coursedesign.BucketCourseDesignInfo;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.coursedesign.GetCourseDesignBucketCommand;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.util.ParamUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/coursedesign.json")
public class CourseDesignJsonModule extends AbstractJsonAuthModule {

    @WebAction
    public RequestTarget onList(RequestCycle cycle, String strOrgId) {

        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        long bucketId = new GetCourseDesignBucketCommand(cycle).forOrg(org.getId());

        CourseDesignClient cdClient =
                Clients.getClient(cycle, CourseDesignClient.class);
        List<BucketCourseDesignInfo> designs = cdClient.listDesigns(bucketId);
        designs = sortDesigns(cycle, designs);

        ByteArrayOutputStream stream = toJsonResponse(cycle, strOrgId, designs);

        return jsonTarget(stream);
    }

    private ByteArrayOutputStream toJsonResponse(final RequestCycle cycle, final String strOrgId,
            List<BucketCourseDesignInfo> designs) {
        DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, CocositeUserHelper.
                getUserLocale(cycle));

        return new DataTablesJson<BucketCourseDesignInfo>(format) {
            @Override
            protected void encodeItem(BucketCourseDesignInfo item) throws IOException {
                JsonGenerator g = generator;

                g.writeNumberField("designId", item.getDesignId());
                g.writeStringField("name", item.getName());
                g.writeNumberField("createdBy", item.getCreatedBy());
                writeDateField("created", item.getCreated());
                g.writeBooleanField("sticky", item.isSticky());
                g.writeBooleanField("enabled", item.isEnabled());
                g.writeStringField("description", item.getDescription());
                g.writeStringField("editlink", NavigationUtil.toDesignPageUrl(cycle,
                        strOrgId, item.getDesignId()));
                g.writeStringField("thumbnail",
                        new CourseDesignThumbnail(cycle, item.getDesignId()).get());
                writeDateField("created", item.getCreated());
            }
        }.encodeToStream(designs);
    }

    private List<BucketCourseDesignInfo> sortDesigns(RequestCycle cycle,
            List<BucketCourseDesignInfo> designs) {

        List<BucketCourseDesignInfo> newList = new ArrayList<>(designs);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        Collections.sort(newList, new BucketCourseDesignInfoSorter(userLocale));

        return newList;
    }

    private static class BucketCourseDesignInfoSorter implements Comparator<BucketCourseDesignInfo> {
        private final Collator collator;
        
        public BucketCourseDesignInfoSorter(Locale userLocale) {
            ParamUtil.required(userLocale,"userLocale");
            this.collator = Collator.getInstance(userLocale);
            collator.setStrength(Collator.SECONDARY);
        }

        @Override
        public int compare(BucketCourseDesignInfo o1, BucketCourseDesignInfo o2) {
            return new CompareToBuilder().append(o1.getName(), o2.getName(), collator).append(o1.
                    getDesignId(), o2.getDesignId()).build();
        }

    }
}
