/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.annotation.WebAction;
import net.unixdeveloper.druwa.annotation.mount.WebModuleMountpoint;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import se.dabox.cocobox.cpweb.NavigationUtil;
import se.dabox.cocobox.cpweb.module.core.AbstractJsonAuthModule;
import static se.dabox.cocobox.cpweb.module.core.AbstractModule.getCocoboxCordinatorClient;
import se.dabox.cocobox.cpweb.module.project.ProjectModule;
import se.dabox.cocobox.cpweb.module.user.UserModule;
import se.dabox.cocobox.security.CocoboxSecurityConstants;
import se.dabox.cocosite.date.DateFormatters;
import se.dabox.cocosite.druwa.CocoSiteConstants;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.login.CocositeUserHelper;
import se.dabox.cocosite.org.MiniOrgInfo;
import se.dabox.cocosite.project.ProjectThumbnail;
import se.dabox.cocobox.security.permission.CocoboxPermissions;
import se.dabox.cocosite.user.MiniUserAccountHelper;
import se.dabox.service.client.CacheClients;
import se.dabox.service.common.ccbc.CocoboxCoordinatorClient;
import se.dabox.service.common.ccbc.OrgUser;
import se.dabox.service.common.ccbc.ProjectStatus;
import se.dabox.service.common.ccbc.alert.ProjectAlertInfo;
import se.dabox.service.common.ccbc.alert.ProjectAlertRequest;
import se.dabox.cocobox.security.user.OrgRoleName;
import se.dabox.cocosite.webfeature.CocositeWebFeatureConstants;
import se.dabox.service.common.ccbc.project.GetProjectAdministrativeName;
import se.dabox.service.common.ccbc.project.MailBounceInfo;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.OrgProjectPredicates;
import se.dabox.service.common.ccbc.project.ProjectSubtypeConstants;
import se.dabox.service.common.io.RuntimeIOException;
import se.dabox.service.common.webfeature.WebFeatures;
import se.dabox.service.login.client.UserAccount;
import se.dabox.service.login.client.UserAccountService;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.service.webutils.json.JsonEncoding;
import se.dabox.service.webutils.login.LoginUserAccountHelper;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.MapUtil;
import se.dabox.util.collections.ValueUtils;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
@WebModuleMountpoint("/json")
public class CpJsonModule extends AbstractJsonAuthModule {

    public static final int DEFAULT_BYTE_SIZE = JsonEncoding.DEFAULT_BYTE_SIZE;
    public static final JsonFactory FACTORY = JsonEncoding.FACTORY;

    @WebAction
    public RequestTarget onListOrgProjects(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);


        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        List<Long> favoriteIds = ccbc.getFavorites(userId, orgId);

        List<OrgProject> projects =
                ccbc.listOrgProjects(orgId);

        if ("t".equals(cycle.getRequest().getParameter("filter")) &&
                WebFeatures.getFeatures(cycle).hasFeature(CocositeWebFeatureConstants.COURSE_CATALOG)) {

            projects = CollectionsUtil.sublist(projects, p -> p.getCourseSessionId() == null);
        }

        projects = CollectionsUtil.sublist(projects, OrgProjectPredicates.
                getSubtypePredicate(ProjectSubtypeConstants.MAIN));
        projects = filterProjects(cycle.getRequest().getParameter("f"), projects);

        ByteArrayOutputStream os = toJsonObjectProjects(cycle, projects, favoriteIds);

        return jsonTarget(os);
    }

    @WebAction
    public RequestTarget onSearchOrgProjects(RequestCycle cycle, String strOrgId, String term)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);


        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        List<Long> favoriteIds = ccbc.getFavorites(userId, orgId);

        List<OrgProject> projects = Collections.emptyList();

        if (hasOrgPermission(cycle, orgId, CocoboxPermissions.CP_LIST_PROJECTS)) {
            projects = ccbc.searchOrgProjects(userId, term, Collections.singletonList(orgId));
        }

        ByteArrayOutputStream os = toJsonObjectProjects(cycle, projects, favoriteIds);

        return jsonTarget(os);
    }

    @WebAction
    public RequestTarget onListProjectAlerts(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        List<MailBounceInfo> bounces = ccbc.getOrgBounceInfo(orgId);
        List<ProjectAlertInfo> alerts =
                ccbc.getProjectAlerts(ProjectAlertRequest.forOrgUnit(orgId));

        Map<Long, ComboProjectAlert> map = MapUtil.createHash(Math.
                max(bounces.size(), alerts.size()));

        for (MailBounceInfo mailBounce : bounces) {
            OrgProject prj = ccbc.getProject(mailBounce.getProjectId());
            if (prj == null || ProjectSubtypeConstants.IDPROJECT.equals(prj.getSubtype())) {
                continue;
            }

            ComboProjectAlert combo = getOrgCreate(map, mailBounce.getOrgId(), mailBounce.
                    getProjectId());

            combo.mailBounces = mailBounce.getBounced();

            combo.setMaxDate(mailBounce.getNewest());
            combo.setMinDate(mailBounce.getOldest());
        }

        for (ProjectAlertInfo alert : alerts) {
            ComboProjectAlert combo = getOrgCreate(map, alert.getOrgId(), alert.
                    getProjectId());

            combo.alerts = alert.getAlertCount();

            combo.setMaxDate(alert.getNewest());
            combo.setMinDate(alert.getOldest());
        }

        List<ComboProjectAlert> list = new ArrayList<>(map.values());

        for (ComboProjectAlert combo : list) {
            OrgProject project = getCocoboxCordinatorClient(cycle).getProject(combo.projectId);
            combo.projectName = getProjectName(cycle, project);
            combo.thumbnail = new ProjectThumbnail(cycle, project).getUrl();
        }

        Collections.sort(list);

        ByteArrayOutputStream baos = toMailBounceInfoJson(cycle, list);
        return jsonTarget(baos);
    }

    @WebAction
    public RequestTarget onListFavoriteOrgProjects(RequestCycle cycle, String strOrgId)
            throws Exception {
        checkOrgPermission(cycle, strOrgId);
        long orgId = Long.valueOf(strOrgId);

        final CocoboxCoordinatorClient ccbc = getCocoboxCordinatorClient(cycle);

        long userId = LoginUserAccountHelper.getUserId(cycle);
        List<Long> favoriteIds = ccbc.getFavorites(userId, orgId);

        List<OrgProject> projects = CollectionsUtil.transformList(favoriteIds, ccbc::getProject);

        ByteArrayOutputStream os = toJsonObjectProjects(cycle, projects, favoriteIds);

        return jsonTarget(os);

    }

    @WebAction
    public RequestTarget onListUsers(RequestCycle cycle, String strOrgId) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        List<UserAccount> uas = getCompleteOrgUserAccountList(org, cycle);
        uas = CollectionsUtil.sublist(uas, (u) -> !u.isAnonymized());

        return jsonTarget(toJsonUserAccounts(cycle, uas, org.getId()));
    }

    @WebAction
    public RequestTarget onSearchUsers(RequestCycle cycle, String strOrgId, String query) {
        MiniOrgInfo org = secureGetMiniOrg(cycle, strOrgId);

        final long userId = LoginUserAccountHelper.getUserId(cycle);
        final String orgRole = OrgRoleName.forOrg(org.getId()).toString();

        List<UserAccount> uas = Collections.emptyList();

        if (hasOrgPermission(cycle, Long.parseLong(strOrgId), CocoboxPermissions.CP_LIST_USERS)) {
            uas = getUserAccountService(cycle).searchUserAccounts(userId, query,
                    CocoSiteConstants.UA_PROFILE, orgRole, CocoboxSecurityConstants.USER_ROLE);
        }

        return jsonTarget(toJsonUserAccounts(cycle, uas, org.getId()));
    }

    private byte[] toJsonUserAccounts(final RequestCycle cycle,
            final List<UserAccount> uas, final long orgId) {

        final String orgRoleName = OrgRoleName.forOrg(orgId).toString();

        final MiniUserAccountHelper accHelper = new MiniUserAccountHelper(cycle);

        return new JsonEncoding() {
            @Override
            protected void encodeData(JsonGenerator generator) throws IOException {
                generator.writeStartObject();
                generator.writeArrayFieldStart("aaData");

                for (UserAccount userAccount : uas) {
                    generator.writeStartObject();

                    generator.writeNumberField("uid", userAccount.getUserId());
                    String name = StringUtils.trimToEmpty(userAccount.getDisplayName());
                    generator.writeStringField("name", name);
                    generator.writeStringField("email", userAccount.
                            getPrimaryEmail());
                    generator.writeStringField("link",
                            cycle.urlFor(UserModule.class, "overview",
                            Long.toString(orgId),
                            Long.toString(userAccount.getUserId())));

                    generator.writeStringField("imagelink24", accHelper.createInfo(userAccount).
                            getThumbnail());

                    generator.writeStringField("role", userAccount.
                            getProfileValue(CocoSiteConstants.UA_PROFILE,
                            orgRoleName));

                    generator.writeEndObject();
                }

                generator.writeEndArray();
                generator.writeEndObject();
            }
        }.encode();
    }


    private ByteArrayOutputStream toJsonObjectProjects(RequestCycle cycle, List<OrgProject> projects,
            List<Long> favoriteIds) {
        ByteArrayOutputStream baos =
                new ByteArrayOutputStream(DEFAULT_BYTE_SIZE);

        final Set<Long> favoriteSet = new HashSet<>(favoriteIds);

        Locale userLocale = CocositeUserHelper.getUserLocale(cycle);

        projects = sortProjects(userLocale, projects);

        NumberFormat nf = NumberFormat.getIntegerInstance(userLocale);
        InfoCacheHelper icHelper = InfoCacheHelper.getInstance(cycle);

        final GetProjectAdministrativeName projectNameHelper = new GetProjectAdministrativeName(
                cycle);

        DateFormat df = getUserListDateFormat(cycle);

        try {

            new DataTablesJson<OrgProject>(df, userLocale) {
                @Override
                protected void encodeItem(OrgProject project) throws IOException {
                    generator.writeNumberField("id", project.getProjectId());
                    generator.writeStringField("name", projectNameHelper.getName(project));
                    generator.writeStringField("added", nf.format(project.getUserCount()));
                    generator.writeStringField("invited", nf.format(project.getInvited()));
                    generator.writeStringField("link",
                            NavigationUtil.toProjectPageUrl(cycle, project.getProjectId()));
                    generator.writeNumberField("createdBy", project.getCreatedBy());
                    generator.writeStringField("createdByName", icHelper.getUserDisplayName(project.getCreatedBy()));
                    writeLongNullField(generator, "updatedBy",
                            project.getUpdatedBy());
                    generator.writeStringField("updatedByName",
                            project.getUpdatedBy() == null ? null
                                    : icHelper.getUserDisplayName(project.getUpdatedBy()));
                    generator.writeStringField("locale", project.getLocale().
                            toString());
                    generator.writeNumberField("ptype", project.getType().getPtype());
                    generator.
                            writeBooleanField("favorite", favoriteSet.contains(project.
                            getProjectId()));

                    writeDateField("created", project.getCreated());
                    writeDateField("updated", project.getUpdated());

                    if(project.getCourseSessionId() != null) {
                        generator.writeNumberField("courseSessionId", project.getCourseSessionId());
                    }
                }
            }.writeTo(projects, baos);

            return baos;
        } catch (IOException ex) {
            throw new RuntimeIOException("Failed to encode JSON", ex);
        }
    }

    private ByteArrayOutputStream toMailBounceInfoJson(final RequestCycle cycle,
            List<ComboProjectAlert> bounces) {

        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM,
                CocositeUserHelper.
                getUserLocale(cycle));

        return new DataTablesJson<ComboProjectAlert>(dateFormat) {
            @Override
            protected void encodeItem(ComboProjectAlert item) throws IOException {
                generator.writeNumberField("projectId", item.projectId);
                generator.writeNumberField("orgId", item.orgId);
                generator.writeStringField("thumbnail", item.thumbnail);
                generator.writeNumberField("bounceCount", item.mailBounces);
                generator.writeNumberField("alerts", item.alerts);
                generator.writeStringField("projectName", item.projectName);
                generator.writeStringField("link",
                        cycle.urlFor(ProjectModule.class.getName(), "roster",
                        Long.toString(item.projectId)));
                writeDateField("newest", item.maxDate);
                if (item.maxDate != null) {
                    String isoDate = DateFormatters.JQUERYAGO_FORMAT.format(item.maxDate);
                    generator.writeStringField("newestAgo", isoDate);
                }
                writeDateField("oldest", item.minDate);
                if (item.minDate != null) {
                    String isoDate = DateFormatters.JQUERYAGO_FORMAT.format(item.minDate);
                    generator.writeStringField("oldestAgo", isoDate);
                }
            }
        }.encodeToStream(bounces);
    }

    private List<UserAccount> getOutsideOrgParticipationUsers(RequestCycle cycle, long orgId,
            Set<UserAccount> loaded) {

        Set<Long> loadedIds =
                CollectionsUtil.transform(loaded, UserAccount::getUserId);

        List<OrgUser> orgUsers = getCocoboxCordinatorClient(cycle).listOrgUsers(orgId);
        Set<Long> userIds = CollectionsUtil.transform(orgUsers, OrgUser::getUserId);
        orgUsers = null;

        userIds.removeAll(loadedIds);

        //Do we want to use cache for this?
        //A big company would flush out all other data from the cache (which has capacity for 1000 or
        //so entries
        return getUserAccountService(cycle).getUserAccounts(userIds);
    }

    private UserAccountService getUserAccountService(RequestCycle cycle) {
        return CacheClients.getClient(cycle, UserAccountService.class);
    }

    private List<UserAccount> getCompleteOrgUserAccountList(MiniOrgInfo org, RequestCycle cycle) {
        CharSequence orgRoleName = OrgRoleName.forOrg(org.getId());
        List<UserAccount> orgUsers =
                getUserAccountService(cycle).
                getUserAccountsWithProfileSetting(CocoSiteConstants.UA_PROFILE,
                orgRoleName);
        Set<UserAccount> accountSet = new HashSet<>(orgUsers);
        accountSet.addAll(orgUsers);

        List<UserAccount> participationUsers = getOutsideOrgParticipationUsers(cycle, org.getId(),
                accountSet);

        accountSet.addAll(participationUsers);

        ArrayList<UserAccount> completeList =
                new ArrayList<>(accountSet);
        return completeList;
    }

    private static ComboProjectAlert getOrgCreate(Map<Long, ComboProjectAlert> map, long orgId,
            long projectId) {
        ComboProjectAlert item = map.get(projectId);

        if (item == null) {
            item = new ComboProjectAlert(projectId, orgId);
            map.put(projectId, item);
        }

        return item;
    }

    private String getProjectName(final RequestCycle cycle, final OrgProject project) {
        return new GetProjectAdministrativeName(cycle).getName(project);
    }

    private List<OrgProject> filterProjects(String parameter, List<OrgProject> projects) {
        final boolean ongoing = decodeStatus(parameter);

        return CollectionsUtil.sublist(projects, (OrgProject item) -> {
            if (ongoing) {
                return item.getStatus() != ProjectStatus.DISABLED;
            } else {
                return item.getStatus() == ProjectStatus.DISABLED;
            }
        });

    }

    private boolean decodeStatus(String parameter) {
        if (parameter == null) {
            return true;
        }

        return !"archived".equals(parameter);
    }

    private List<OrgProject> sortProjects(Locale userLocale, List<OrgProject> projects) {
        if (projects == null || projects.isEmpty()) {
            return projects;
        }

        final Collator collator = Collator.getInstance(userLocale);
        collator.setStrength(Collator.SECONDARY);

        List<OrgProject> sortedList = new ArrayList<>(projects);

        //TODO: This does not take computed names into account. Shouldn't matter much since subprojects
        //normally shouldn't be listed anywhere.
        Collections.sort(sortedList, (OrgProject o1, OrgProject o2) ->
                new CompareToBuilder().
                        append(o1.getName(), o2.getName(), collator).
                        append(o1.getProjectId(), o2.getProjectId()).build());

        return sortedList;
    }

    private static class ComboProjectAlert implements Comparable<ComboProjectAlert> {

        public final long projectId;
        public final long orgId;
        public long mailBounces;
        public long alerts;
        public Date minDate;
        public Date maxDate;
        public String projectName;
        public String thumbnail;

        public ComboProjectAlert(long projectId, long orgId) {
            this.projectId = projectId;
            this.orgId = orgId;
        }

        public void setMinDate(Date date) {
            minDate = ValueUtils.min(minDate, date);
        }

        public void setMaxDate(Date date) {
            maxDate = ValueUtils.min(minDate, date);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 11 * hash + (int) (this.projectId ^ (this.projectId >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ComboProjectAlert other = (ComboProjectAlert) obj;
            if (this.projectId != other.projectId) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(ComboProjectAlert o) {
            return new CompareToBuilder().append(this.projectName, o.projectName).
                    append(this.projectId, o.projectId).build();
        }

    }
}
