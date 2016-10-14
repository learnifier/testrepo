package se.dabox.cocobox.cpweb.module.project.command;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.material.ProjectMaterialCoordinatorClient;
import se.dabox.service.common.coursedesign.CourseDesignClient;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.coursecatalog.client.CourseCatalogClient;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
class AbstractCopyCommand {
    protected final RequestCycle cycle;

    AbstractCopyCommand(RequestCycle cycle) {
        this.cycle = cycle;
    }
    
    CourseCatalogClient getCourseCatalogClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseCatalogClient.class);
    }

    CourseDesignClient getCourseDesignClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CourseDesignClient.class);
    }

    CocoboxCoordinatorClient getCocoboxCoordinatorClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);
    }

    ProductDirectoryClient getProductDirectoryClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, ProductDirectoryClient.class);
    }

    ProjectMaterialCoordinatorClient getProjectMaterialCoordinatorClient(ServiceRequestCycle cycle) {
        return CacheClients.getClient(cycle, ProjectMaterialCoordinatorClient.class);
    }
}
