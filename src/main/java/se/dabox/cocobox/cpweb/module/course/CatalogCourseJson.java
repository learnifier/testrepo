/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.course;

import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocosite.freemarker.util.CdnUtils;
import se.dabox.service.common.cr.DwsContentRepoClient;
import se.dabox.service.coursecatalog.client.course.CatalogCourse;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class CatalogCourseJson {
    private final CatalogCourse course;

    public CatalogCourseJson(CatalogCourse course) {
        this.course = course;
    }

    public CatalogCourse getCourse() {
        return course;
    }

    public String getThumbnailUrl() {
        DwsContentRepoClient repoClient = new DwsContentRepoClient();

        String url = null;

        if (course.getThumbnailUrl() != null) {
            try {
                url = repoClient.getDownloadUrl(url);
            } catch(Exception ex) {
                //Ignore
            }
        }

        if (url == null) {
            url = CdnUtils.getResourceUrl(CpwebConstants.SESSION_DEFAULT_THUMBNAIL);
        }

        return url;
    }

}
