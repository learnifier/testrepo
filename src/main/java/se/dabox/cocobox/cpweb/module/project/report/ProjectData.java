/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.util.List;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectProduct;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ProjectData {

    private final OrgProject project;
    private final CourseDesignDefinition definition;
    private final DatabankFacade databankFacade;
    public List<ProjectProduct> projProducts;

    public ProjectData(OrgProject project, CourseDesignDefinition definition,
            DatabankFacade databankFacade) {
        this.project = project;
        this.definition = definition;
        this.databankFacade = databankFacade;
    }

    public OrgProject getProject() {
        return project;
    }

    public CourseDesignDefinition getDefinition() {
        return definition;
    }

    public DatabankFacade getDatabankFacade() {
        return databankFacade;
    }
}
