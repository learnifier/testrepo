/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import net.unixdeveloper.druwa.RequestCycle;
import se.dabox.cocosite.project.ProjectThumbnail;
import se.dabox.service.common.ccbc.project.ProjectDetails;
import se.dabox.service.webutils.freemarker.LazyTemplateScalarModel;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class LazyProjectThumbnail extends LazyTemplateScalarModel {
    private final RequestCycle cycle;
    private final ProjectDetails project;

    public LazyProjectThumbnail(RequestCycle cycle, ProjectDetails project) {
        this.cycle = cycle;
        this.project = project;
    }

    @Override
    protected String generateString() {
        return new ProjectThumbnail(cycle, project).getUrl();
    }

}
