/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.publish;

import java.util.List;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.publish.PublishTaskTypeFactory;
import se.dabox.service.common.scheduler.SchedulerServiceClient;
import se.dabox.service.common.scheduler.TaskInfo;
import se.dabox.service.common.scheduler.filter.TaskFilterBuilder;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class IsProjectPublishingCommand {

    /**
     * Determines if a publishing task is running for the specified project.
     *
     * @param project A project
     * @return True if a publishing task is running or false otherwise.
     */
    public boolean isPublishing(OrgProject project) {
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        SchedulerServiceClient scheduler = CacheClients.getClient(cycle,
                SchedulerServiceClient.class);

        TaskFilterBuilder filterBuilder = new TaskFilterBuilder();
        filterBuilder.addType(PublishTaskTypeFactory.forProject(project.getProjectId()));

        List<TaskInfo> tasks = scheduler.listTasks(filterBuilder.build());

        return !tasks.isEmpty();
    }

}
