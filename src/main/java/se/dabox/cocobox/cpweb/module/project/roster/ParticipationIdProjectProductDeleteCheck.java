/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.roster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.module.project.DeleteFailure;
import se.dabox.cocosite.coursedesign.GetProjectCourseDesignCommand;
import se.dabox.cocosite.user.UserIdentifierHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.NotFoundException;
import se.dabox.service.common.ccbc.product.IdProjectProductUtil;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectParticipationState;
import se.dabox.service.common.ccbc.project.ProjectTypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.common.proddir.CocoboxProductTypeConstants;
import se.dabox.service.common.proddir.ProductDirectoryClient;
import se.dabox.service.proddir.data.Product;
import se.dabox.service.proddir.data.ProductId;
import se.dabox.service.webutils.listform.ListformContext;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ParticipationIdProjectProductDeleteCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ParticipationIdProjectProductDeleteCheck.class);

    private final CocoboxCordinatorClient cocoboxCordinatorClient;
    private final long projectId;
    private Set<ProductId> idProductIds;
    private UserIdentifierHelper helper;

    ParticipationIdProjectProductDeleteCheck(CocoboxCordinatorClient cocoboxCordinatorClient,
            long projectId) {
        this.cocoboxCordinatorClient = cocoboxCordinatorClient;
        this.projectId = projectId;
    }

    public List<DeleteFailure> check(final ListformContext context, final List<Long> values) {
        helper = new UserIdentifierHelper(context.getCycle());
        final OrgProject project = cocoboxCordinatorClient.getProject(projectId);

        return ProjectTypeUtil.call(project.getType(),
                new ProjectTypeCallable<List<DeleteFailure>>() {
                    @Override
                    public List<DeleteFailure> callDesignedProject() {
                        idProductIds = getIdProductIds(context, project);

                        if (idProductIds.isEmpty()) {
                            return null;
                        }

                        List<DeleteFailure> failures = new ArrayList<>();

                        for (Long participationId : values) {
                            if (!canDeleteParticipation(participationId)) {
                                failures.add(new DeleteFailure(participationId,
                                                getParticipationName(participationId),
                                                "There are participants in a sub project"));
                            }
                        }

                        return failures.isEmpty() ? null : failures;
                    }

                    @Override
                    public List<DeleteFailure> callMaterialListProject() {
                        return null;
                    }

                    private String getParticipationName(Long participationId) {
                        ProjectParticipation participant = cocoboxCordinatorClient.
                        getProjectParticipation(participationId);

                        return helper.getName(participant.getUserId());
                    }
                });
    }

    private boolean canDeleteParticipation(Long participationId) {
        for (ProductId productId : idProductIds) {
            if (!canDeleteProductForParticipant(participationId, productId)) {
                return false;
            }
        }

        return true;
    }

    private boolean canDeleteProductForParticipant(Long participationId, ProductId productId) {
        ProjectParticipationState state = cocoboxCordinatorClient.getParticipationState(
                participationId);

        if (state == null) {
            return true;
        }

        String stateName = IdProjectProductUtil.getProjectIdStateName(productId);

        String strProjectId = state.getMap().get(stateName);

        if (strProjectId == null) {
            return true;
        }

        long subProjectId = Long.parseLong(strProjectId);

        List<ProjectParticipation> subParticipants;
        try {
            subParticipants = cocoboxCordinatorClient.
                    listProjectParticipations(subProjectId);
        } catch (NotFoundException nfe) {
            LOGGER.info("Subproject {} for product {} doesn't exist enymore. Product can be deleted.", subProjectId,
                    productId);

            return true;
        }

        if (subParticipants == null) {
            LOGGER.warn(
                    "No participation list from subproject {} though a mapping existed for participant {}",
                    strProjectId, participationId);
            return true;
        }

        return subParticipants.isEmpty();
    }

    private Set<ProductId> getIdProductIds(ListformContext context, OrgProject project) {
        CourseDesignDefinition cdd = new GetProjectCourseDesignCommand(context.getCycle()).
                setFallbackToStageDesign(true).
                forProject(project);

        ProductDirectoryClient pdClient = CacheClients.getClient(context.getCycle(),
                ProductDirectoryClient.class);

        List<Product> products = pdClient.getProducts(cdd.getAllProductIdStringSet());

        Set<ProductId> productIds = new HashSet<>();

        for (Product product : products) {
            if (product.getProductTypeId().equals(CocoboxProductTypeConstants.IDPROJECT)) {
                productIds.add(product.getId());
            }
        }

        return productIds;
    }
}
