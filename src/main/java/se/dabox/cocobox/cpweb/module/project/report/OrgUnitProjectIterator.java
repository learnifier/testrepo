/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.OrgProjectPredicates;
import se.dabox.service.common.ccbc.project.OrgProjectTransformers;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectSubtypeConstants;
import se.dabox.util.collections.CollectionsUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
class OrgUnitProjectIterator implements Iterator<ProjectParticipation> {

    private final Iterator<Long> projectIdIterator;

    private Iterator<ProjectParticipation> ppartIterator;
    private final CocoboxCordinatorClient ccbcClient;

    OrgUnitProjectIterator(ServiceRequestCycle cycle, long ouId) {
        ccbcClient
                = CacheClients.getClient(cycle, CocoboxCordinatorClient.class);

        List<OrgProject> projects = ccbcClient.listOrgProjects(ouId);
        projects = CollectionsUtil.sublist(projects, OrgProjectPredicates.
                getSubtypePredicate(ProjectSubtypeConstants.MAIN));
        List<Long> projectIdsList
                = CollectionsUtil.transformList(projects, OrgProjectTransformers.idTransformer());

        projectIdIterator = new ArrayList<>(projectIdsList).iterator();
    }

    @Override
    public boolean hasNext() {
        while (ppartIterator == null || !ppartIterator.hasNext()) {
            fetchNextInternal();

            if (ppartIterator == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ProjectParticipation next() {
        if (ppartIterator == null || !ppartIterator.hasNext()) {
            fetchNextInternal();
        }

        if (ppartIterator == null) {
            throw new NoSuchElementException();
        }

        return ppartIterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    private void fetchNextInternal() {
        while (true) {
            if (!projectIdIterator.hasNext()) {
                ppartIterator = null;
                return;
            }

            Long pid = projectIdIterator.next();

            List<ProjectParticipation> participations = ccbcClient.listProjectParticipations(pid);
            if (participations != null && !participations.isEmpty()) {
                ppartIterator = participations.iterator();
                return;
            }
        }
    }

}
