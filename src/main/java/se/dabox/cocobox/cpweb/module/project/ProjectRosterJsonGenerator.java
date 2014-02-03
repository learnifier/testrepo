/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import se.dabox.cocobox.cpweb.CpwebConstants;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocosite.coursedesign.GetDatabankFacadeCommand;
import se.dabox.cocosite.coursedesign.GetProjectCourseDesignCommand;
import se.dabox.cocosite.date.DateFormatters;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.util.PercentageCalculator;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.client.CacheClients;
import se.dabox.service.client.ClientFactoryException;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.ParticipationProgress;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.ProjectParticipation;
import se.dabox.service.common.ccbc.project.ProjectType;
import se.dabox.service.common.ccbc.project.ProjectTypeRunnable;
import se.dabox.service.common.ccbc.project.ProjectTypeUtil;
import se.dabox.service.common.ccbc.project.cddb.DatabankEntry;
import se.dabox.service.common.context.DwsRealmHelper;
import se.dabox.service.common.coursedesign.CourseDesignType;
import se.dabox.service.common.coursedesign.DatabankFacade;
import se.dabox.service.common.coursedesign.activity.ActivityCourse;
import se.dabox.service.common.coursedesign.activity.CourseDesignDefinitionActivityCourseFactory;
import se.dabox.service.common.coursedesign.v1.CddCodec;
import se.dabox.service.common.coursedesign.v1.CourseDesignDefinition;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountTransformers;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.service.webutils.webfeature.WebFeatures;
import se.dabox.util.collections.CollectionsUtil;

/**
 * Generates the project roster json.
 *
 * Only use this object one time. Not thread safe.
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class ProjectRosterJsonGenerator {

    private RequestCycle cycle;
    private OrgProject project;
    private CourseDesignDefinition cdd;
    private DatabankFacade databankFacade;
    private List<ProjectParticipation> participations;

    ByteArrayOutputStream toJson(
            final RequestCycle cycle, List<ProjectParticipation> participations,
            List<UserAccount> users, final String strOrgId, final OrgProject prj) {
        this.cycle = cycle;
        this.project = prj;
        this.participations = participations;

        prepare();

        final Long mainUserId = getOwnerUserId(cycle, prj);

        final Set<Long> sendParticipations = getSendParticipations(cycle);

        final Map<Long, UserAccount> uaMap =
                CollectionsUtil.createMap(users, UserAccountTransformers.getUserIdTransformer());
        
        Locale locale = CocositeUserHelper.getUserLocale(cycle);

        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        dateFormat.setTimeZone(prj.getTimezone());

        final DateFormat dateTimeFormat =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
        dateTimeFormat.setTimeZone(prj.getTimezone());

        return new DataTablesJson<ProjectParticipation>(dateFormat) {
            @Override
            protected void encodeItem(ProjectParticipation item) throws IOException {
                final JsonGenerator g = generator;

                g.writeNumberField("id", item.getParticipationId());
                final UserAccount ua = uaMap.get(item.getUserId());
                //TODO: Temporary hack for users that has been removed though not allowed
                if (ua == null) {
                    g.writeStringField("displayName", "(Removed User)");
                    g.writeStringField("email", "removed.user@email.com");
                } else {
                    String name = ua.getDisplayName();
                    if (StringUtils.isBlank(name)) {
                        name = ua.getPrimaryEmail();
                    }
                    g.writeStringField("displayName", name);
                    g.writeStringField("email", ua.getPrimaryEmail());
                }
                boolean main = mainUserId != null && mainUserId.longValue() == item.getUserId();
                g.writeBooleanField("main", main);
                writeDateField("firstEmail", item.getFirstMail());
                writeDateField("lastEmail", item.getLastMail());
                writeDateField("firstAccess", item.getFirstAccess(), dateTimeFormat);

                writeDateField("lastAccess", item.getLastAccess(), dateTimeFormat);
                if (item.getLastAccess() != null) {
                    String isoDate = DateFormatters.JQUERYAGO_FORMAT.format(item.getLastAccess());
                    g.writeStringField("lastAccessAgo", isoDate);
                }
                int completedCids = 0;
                if (item.getCompletedCids() != null) {
                    completedCids = item.getCompletedCids();
                }
                g.writeNumberField("status", percentage(prj, completedCids, item));
                g.writeStringField("link", NavigationUtil.toUserPageUrl(cycle,
                        strOrgId, item.getUserId())+'/'+item.getParticipationId());
                g.writeBooleanField("bounced", item.isMailBounced());

                String participationLink = getConfValue(cycle, "cocobox.user.loginsite") + '/'
                        + item.getStringId();

                g.writeStringField("participationLink", participationLink);

                g.writeBooleanField("inError", item.isInError());
                g.writeBooleanField("activated", item.isActivated());

                String detailsUrl = cycle.urlFor(ParticipationJsonModule.class,
                        "participationDetails",
                        Long.toString(item.getParticipationId()));

                g.writeStringField("detailsUrl", detailsUrl);

                g.writeBooleanField("sending",
                        sendParticipations.contains(item.getParticipationId()));

                g.writeStringField("errorMsg", toErrorMessageHtml(item.getErrorMessage()));

                g.writeBooleanField("overdue", isOverdue(item));
            }
        }.encodeToStream(participations);
    }

    private int percentage(OrgProject project, Integer completed,
            ProjectParticipation participation) {

        if (project.getType() != ProjectType.DESIGNED_PROJECT || !getDesignType().isActivityBased()) {
            return PercentageCalculator.percentage(project.getProgressComponentCount(), completed);
        }

        if (participation.getActivityCount() == null || participation.getActivitiesCompleted()
                == null) {
            return PercentageCalculator.percentage(project.getProgressComponentCount(), completed);
        }

        return PercentageCalculator.percentage(
                participation.getActivityCount().intValue(),
                participation.getActivitiesCompleted().intValue());
    }

    public CourseDesignType getDesignType() {
        WebFeatures features = WebFeatures.getFeatures(cycle);

        if (features.hasFeature(CocositeWebFeatureConstants.CDESIGN2)) {
            return CourseDesignType.C2;
        } else if (features.hasFeature(CocositeWebFeatureConstants.MDESIGN)) {
            return CourseDesignType.MDESIGN;
        }

        return CourseDesignType.CLASSIC;
    }

    private String getConfValue(RequestCycle cycle, String name) {
        return DwsRealmHelper.getRealmConfiguration(cycle).getValue(name);
    }

    private Set<Long> getSendParticipations(RequestCycle cycle) {
        @SuppressWarnings("unchecked")
        Set<Long> set = (Set<Long>) cycle.getSession().getFlashAttribute(
                CpwebConstants.SEND_PARTICIPATIONS_FLASH);

        if (set == null) {
            return Collections.emptySet();
        }

        return set;
    }

    private Long getOwnerUserId(RequestCycle cycle, OrgProject prj) {
        if (prj.getParticipationOwner() == null) {
            return null;
        }

        CocoboxCordinatorClient ccbcClient =
                getCocobocCordinatorClient(cycle);

        ProjectParticipation part = ccbcClient.getProjectParticipation(prj.getParticipationOwner());

        if (part == null) {
            return null;
        }

        return part.getUserId();
    }

    private CocoboxCordinatorClient getCocobocCordinatorClient(RequestCycle cycle) throws ClientFactoryException {
        CocoboxCordinatorClient ccbcClient =
                CacheClients.getClient(cycle, CocoboxCordinatorClient.class);
        return ccbcClient;
    }

    private static String toErrorMessageHtml(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }

        String msg = StringEscapeUtils.escapeXml(errorMessage);
        return msg.replace("\n", "<br/>");
    }

    private boolean isOverdue(ProjectParticipation item) {
        if (cdd == null) {
            return false;
        }

        List<ParticipationProgress> progress
                = getCocobocCordinatorClient(cycle).getParticipationProgress(item.
                        getParticipationId());

        ActivityCourse activityCourse
                = new CourseDesignDefinitionActivityCourseFactory().newActivityCourse(cdd,
                        databankFacade, progress);

        return activityCourse.isOverdue();
    }

    private void prepare() {
        ProjectTypeUtil.run(project.getType(), new ProjectTypeRunnable() {

            @Override
            public void runDesignedProject() {
                ProjectRosterJsonGenerator.this.cdd = getCourseDesign();
                ProjectRosterJsonGenerator.this.databankFacade =
                        getDatabankFacade();
                        
            }

            @Override
            public void runMaterialListProject() {
                //Nothing to prepare
            }

            private CourseDesignDefinition getCourseDesign() {
                //Workaround for the situation when there are users but no design yet

                if (project.getDesignId() == null) {
                    return CddCodec.decode(cycle, CddCodec.getBlankXml());
                } else {
                    return new GetProjectCourseDesignCommand(cycle).
                            forProject(project);
                }
            }

            private DatabankFacade getDatabankFacade() {
                if (project.getMasterDatabank() == null) {
                    return new DatabankFacade(Collections.<DatabankEntry>emptyList(), project);
                }

                return new GetDatabankFacadeCommand(cycle).get(project);
            }

        });
    }
}
