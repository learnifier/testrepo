/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject.transformer;

import java.util.ArrayList;
import java.util.List;
import net.unixdeveloper.druwa.DruwaService;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import se.dabox.cocobox.cpweb.module.report.StatusHolder;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ajaxlongrun.Status;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.participation.filter.FilterParticipationRequestBuilder;
import se.dabox.service.common.ccbc.participation.response.ListParticipationResponse;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Factory;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class FetchOrgProjectParticipants implements Factory<List<SubprojectParticipant>> {
    private final StatusHolder statusHolder;
    private final long orgId;
    private final ProductId productId;

    public FetchOrgProjectParticipants(StatusHolder statusHolder, long orgId, ProductId productId) {
        this.statusHolder = statusHolder;
        this.orgId = orgId;
        this.productId = productId;
    }

    @Override
    public List<SubprojectParticipant> create() {
        statusHolder.setStatus(new Status("Fetching participations"));

        FilterParticipationRequestBuilder reqBuilder = FilterParticipationRequestBuilder.
                newListOrgUnitParticipations(orgId);
        reqBuilder.withActivated(true);
        //Exclude people who are not correctly activated
        reqBuilder.withInError(false);
        reqBuilder.withProjectProduct(productId);

        //We want to always get all participants for the organization we are in. Otherwise
        //it will come out as an empty report for demo orgs for example (JK).
        reqBuilder.withIncludeDemoOrgUnits(true);
        reqBuilder.withIncludeDisabledOrgUnits(true);

        String afterKey = null;
        final ServiceRequestCycle cycle = DruwaService.getCurrentCycle();
        CocoboxCoordinatorClient ccbc
                = CacheClients.getClient(cycle, CocoboxCoordinatorClient.class);

        List<SubprojectParticipant> retlist = new ArrayList<>();
        while (true) {
            reqBuilder.withAfterKey(afterKey);
            ListParticipationResponse resp
                    = ccbc.listProjectParticipations(reqBuilder.createFilterParticipationRequest());

            retlist.addAll(toReportObjects(resp.getParticipations()));
            if (resp.isMoreAvailable()) {
                afterKey = resp.getAfterKey();
            } else {
                break;
            }
        }

        return retlist;
    }

    private List<SubprojectParticipant> toReportObjects(List<ProjectParticipation> participations) {
        return CollectionsUtil.transformList(participations, this::toReportObject);
    }

    private SubprojectParticipant toReportObject(ProjectParticipation participation) {
        return new SubprojectParticipant(participation.getParticipationId(),
                participation.getProjectId(),
                participation.getUserId());
    }

}
