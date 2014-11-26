/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.user;

import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.util.cache.LazyCache;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class LazyProjectName {
    private final LazyCache<Long,String> cache;

    public LazyProjectName(final CocoboxCoordinatorClient ccbc) {
        this.cache = new LazyCache<>(new Transformer<Long, String>() {

            @Override
            public String transform(Long pid) {
                OrgProject project = ccbc.getProject(pid);

                return project.getName();
            }
        });

    }

    public String forProject(long projectId) {
        return cache.get(projectId);
    }

}
