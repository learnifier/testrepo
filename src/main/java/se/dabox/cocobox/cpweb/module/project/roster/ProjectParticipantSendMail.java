/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.roster;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.mail.RequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.mail.UrlRequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.project.AbstractRosterListCommand;
import se.dabox.cocobox.cpweb.module.project.ProjectSendMailProcessor;
import se.dabox.cocobox.cpweb.module.project.credit.CpCreditCheck;
import se.dabox.cocobox.cpweb.module.project.credit.CreditAllocationFailure;
import se.dabox.cocobox.cpweb.state.SendMailSession;
import se.dabox.cocosite.security.CocoboxPermissions;
import se.dabox.cocosite.security.Permission;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectSubtypeCallable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.orgdir.client.OrgUnitInfo;
import se.dabox.service.orgdir.client.OrganizationDirectoryClient;
import se.dabox.service.webutils.listform.ListformContext;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ProjectParticipantSendMail extends AbstractRosterListCommand implements
        PermissionListformCommand<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectParticipantSendMail.class);
    private SendMailSession sms;
    private Map<Long, ProjectParticipation> partMap;

    @Override
    public RequestTarget execute(ListformContext context,
            final List<Long> values) {

        partMap = getParticipationMap(context);

        final long projectId = context.getAttribute("projectId", Long.class);

        OrgProject prj = context.getAttribute("project", OrgProject.class);

        OrgUnitInfo org = getOrg(context.getCycle(), prj.getOrgId());

        List<CreditAllocationFailure> failures = creditCheck(context, org, projectId, values);

        if (!failures.isEmpty()) {
            context.getCycle().getSession().setFlashAttribute(CpwebConstants.CREDIT_ALLOC_FLASH,
                    failures);
            return NavigationUtil.toProjectPage(projectId);
        }

        RequestTargetGenerator target = new RequestTargetGenerator() {
            @Override
            public RequestTarget generateTarget(RequestCycle cycle) {
                Set<Long> valueSet = new HashSet<>(values);

                cycle.getSession().setFlashAttribute(
                        CpwebConstants.SEND_PARTICIPATIONS_FLASH, valueSet);

                return NavigationUtil.toProjectPage(projectId);
            }
        };

        UrlRequestTargetGenerator cancelTarget = new UrlRequestTargetGenerator(NavigationUtil.
                toProjectPageUrl(context.getCycle(),
                        projectId));

        sms = new SendMailSession(new ProjectSendMailProcessor(projectId, org.getId()),
                target, cancelTarget);
        sms.ensureCapacity(values.size());

        return super.execute(context, values);
    }

    private Map<Long, ProjectParticipation> getParticipationMap(ListformContext context) {
        OrgProject prj = context.getAttribute("project", OrgProject.class);
        CocoboxCordinatorClient ccbc = getCocoboxCordinatorClient(context);
        List<ProjectParticipation> parts = ccbc.listProjectParticipations(prj.getProjectId());
        Map<Long, ProjectParticipation> map = CollectionsUtil.createMap(parts,
                new Transformer<ProjectParticipation, Long>() {
                    @Override
                    public Long transform(ProjectParticipation obj) {
                        return obj.getParticipationId();
                    }
                });

        return map;
    }

    @Override
    protected void executeSingle(ListformContext context, Long value) {
        ProjectParticipation part = partMap.get(value);
        if (part == null) {
            LOGGER.info("Participation disappeared: {}", value);
        } else {
            sms.addReceiver(part.getUserId(), part.getParticipationId());
        }
    }

    @Override
    protected RequestTarget getRequestTarget(ListformContext context,
            List<Long> values) {

        OrgProject prj = context.getAttribute("project", OrgProject.class);

        sms.storeInSession(context.getCycle());

        return sms.getPreSendTarget(prj.getOrgId());
    }

    public SendMailSession getSms() {
        return sms;
    }

    private CocoboxCordinatorClient getCocoboxCordinatorClient(ListformContext context) {
        CocoboxCordinatorClient ccbc = context.getAttribute("ccbcClient",
                CocoboxCordinatorClient.class);
        return ccbc;
    }

    private OrgUnitInfo getOrg(RequestCycle cycle, long orgId) {
        return CacheClients.getClient(cycle, OrganizationDirectoryClient.class).getOrgUnitInfo(
                orgId);
    }

    private List<CreditAllocationFailure> creditCheck(final ListformContext context,
            final OrgUnitInfo org,
            final long projectId,
            final List<Long> values) {

        final CocoboxCordinatorClient cocoboxCordinatorClient = getCocoboxCordinatorClient(context);

        OrgProject project = cocoboxCordinatorClient.getProject(projectId);

        return ProjectTypeUtil.callSubtype(project,
                new ProjectSubtypeCallable<List<CreditAllocationFailure>>() {
                    @Override
                    public List<CreditAllocationFailure> callMainProject() {

                        CpCreditCheck ccc = new CpCreditCheck(context.getCycle(), org, projectId);
                        List<CreditAllocationFailure> failures = ccc.getCreditFailures(values);
                        return failures;
                    }

                    @Override
                    public List<CreditAllocationFailure> callIdProjectProject() {
                        return Collections.emptyList();
                    }
                });
    }

    @Override
    public List<Permission> getPermissionsRequired() {
        return Collections.singletonList(CocoboxPermissions.CP_SEND_MAIL_PROJECT);
    }
}
