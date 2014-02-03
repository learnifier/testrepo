/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import net.unixdeveloper.druwa.ServiceRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.report.IdProjectStatusReport.IdProjectStatus;
import se.dabox.cocosite.user.MiniUserInfo;
import se.dabox.service.common.RealmBackgroundCallable;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.product.IdProjectProductUtil;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductTypeId;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class IdProjectStatusReport extends AbstractProductReportBuilder<IdProjectStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdProjectStatusReport.class);

    private final Product product;
    private String linkPrefix;

    public IdProjectStatusReport(Product product, ServiceRequestCycle cycle, String linkPrefix) throws IllegalArgumentException {
        super(cycle);
        this.product = product;
        ParamUtil.required(product, "product");

        if (!product.isTypeInstance(new ProductTypeId("idproject"))) {
            throw new IllegalArgumentException("Product " + product.getId()
                    + " is not an idproject product. Is: " + product.getProductTypeId());
        }

        this.linkPrefix = linkPrefix;
    }

    public static FutureTask<byte[]> getReportForOrgUnitTask(final ServiceRequestCycle cycle,
            final Product product, final long ouId, final String linkPrefix) {
        return new FutureTask<>(new RealmBackgroundCallable<byte[]>(cycle.
                getServiceApplication()) {

                    @Override
                    protected byte[] callInCycle(ServiceRequestCycle cycle) {
                        return new IdProjectStatusReport(product, cycle, linkPrefix).getReportForOrgUnit(ouId);
                    }
                });
    }

    public static FutureTask<byte[]> getReportForProjectTask(final ServiceRequestCycle cycle,
            final Product product, final OrgProject project, final String linkPrefix) {
        return new FutureTask<>(new RealmBackgroundCallable<byte[]>(cycle.
                getServiceApplication()) {

                    @Override
                    protected byte[] callInCycle(ServiceRequestCycle cycle) {
                        return new IdProjectStatusReport(product, cycle, linkPrefix).
                        getReportForProject(project);
                    }
                });
    }

    @Override
    protected IdProjectStatus getParticipationData(ProjectData projData,
            ProjectParticipation participant) {
        if (!participant.isActivated()) {
            return null;
        }

        Long projectId = getProjectId(participant);
        if (projectId == null) {
            return null;
        }
        List<ProjectParticipation> participations;

        try {
            participations = ccbcClient.listProjectParticipations(projectId);
        } catch (NotFoundException nfe) {
            LOGGER.warn("Subproject doesn't exist for {}/{}: {}", participant.getParticipationId(),
                    product.getId(), projectId);
            return null;
        }

        int completed = 0;
        int accessed = 0;
        int invited = 0;

        for (ProjectParticipation projPart : participations) {
            if (projPart.isActivated()) {
                invited++;
            }

            if (projPart.getLastAccess() != null) {
                accessed++;
            }

            if (projPart.isActivitiesCompleted()) {
                completed++;
            }
        }

        return new IdProjectStatus(participant, projectId, invited, accessed, completed);
    }

    private Long getProjectId(ProjectParticipation participant) throws NumberFormatException {
        ProjectParticipationState partState
                = ccbcClient.getParticipationState(participant.getParticipationId());
        if (partState == null) {
            return null;
        }

        String stateName = IdProjectProductUtil.getProjectIdStateName(product.getId());
        String stateValue = partState.getMap().get(stateName);
        if (stateValue == null) {
            LOGGER.debug("Should have state for idproject project but none found");
            return null;
        }

        try {
            Long projectId = Long.parseLong(stateValue);
            return projectId;
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Invalid content in state variable part:{}/statename:{}: {}", participant.
                    getParticipationId(), stateName, stateValue);
        }

        return null;
    }

    @Override
    protected void populateJsonData(Map<String, Object> jsonResponse, List<IdProjectStatus> partData) {
        List<Map<String, Object>> aaData = new ArrayList<>();

        for (IdProjectStatus idProjectStatus : partData) {
            Map<String, Object> map = new HashMap<>();
            map.put("participationId", idProjectStatus.getParticipation().getParticipationId());
            map.put("userId", idProjectStatus.getParticipation().getUserId());
            map.put("activated", idProjectStatus.getInvited());
            map.put("accessed", idProjectStatus.getAccessed());
            map.put("completed", idProjectStatus.getCompleted());
            map.put("link", linkPrefix+'/'+idProjectStatus.getProjectId());

            MiniUserInfo miniUser
                    = icHelper.getMiniUserInfo(idProjectStatus.getParticipation().getUserId());

            map.put("name", miniUser.getDisplayName());
            map.put("email", miniUser.getEmail());
            map.put("thumbnail", miniUser.getThumbnail());

            aaData.add(map);
        }

        jsonResponse.put("aaData", aaData);
        jsonResponse.put("product", new ReportProductInformation(cycle).forProduct(product));
    }

    public static class IdProjectStatus {

        private final ProjectParticipation participation;
        private final long projectId;
        private final int invited;
        private final int accessed;
        private final int completed;

        public IdProjectStatus(ProjectParticipation participation, long projectId, int invited,
                int accessed, int completed) {
            this.participation = participation;
            this.projectId = projectId;
            this.invited = invited;
            this.accessed = accessed;
            this.completed = completed;
        }

        public ProjectParticipation getParticipation() {
            return participation;
        }

        public long getProjectId() {
            return projectId;
        }

        public int getInvited() {
            return invited;
        }

        public int getAccessed() {
            return accessed;
        }

        public int getCompleted() {
            return completed;
        }

        @Override
        public String toString() {
            return "IdProjectStatus{" + "participation=" + participation + ", projectId="
                    + projectId + ", invited=" + invited + ", accessed=" + accessed + ", completed="
                    + completed + '}';
        }

    }
}
