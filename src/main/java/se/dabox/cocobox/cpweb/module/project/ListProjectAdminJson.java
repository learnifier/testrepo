/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.unixdeveloper.druwa.RequestCycle;
import org.apache.commons.lang3.builder.CompareToBuilder;
import se.dabox.cocosite.infocache.InfoCacheHelper;
import se.dabox.cocosite.security.role.CocoboxRoleUtil;
import se.dabox.cocosite.security.role.RoleUuidNamePair;
import se.dabox.cocosite.user.MiniUserInfo;
import se.dabox.service.common.ccbc.CocoboxCordinatorClient;
import se.dabox.service.common.ccbc.project.OrgProject;
import se.dabox.service.common.ccbc.project.role.ProjectUserRole;
import se.dabox.service.common.ccbc.project.role.ProjectUserRoleSearch;
import se.dabox.service.webutils.json.DataTablesJson;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Transformer;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ListProjectAdminJson {

    private final RequestCycle cycle;
    private final CocoboxCordinatorClient ccbc;
    private Map<Long, Set<String>> userRolesMap;
    private OrgProject prj;
    private List<MiniUserInfo> users;
    private List<RoleUuidNamePair> projectRoles;
    private String strProjectId;
    private Map<String, String> projectRolesMap;


    ListProjectAdminJson(RequestCycle cycle, CocoboxCordinatorClient ccbc) {
        this.cycle = cycle;
        this.ccbc = ccbc;
    }

    ByteArrayOutputStream forProject(OrgProject project) {
        prj = project;
        strProjectId = Long.toString(prj.getProjectId());
        prepareData();
        return generateJson();
    }

    private void prepareData() {
        getCpRoles();
        createUserRolesMap();
        getUsers();
        sortUsers();
    }

    private void createUserRolesMap() {
        ProjectUserRoleSearch search = ProjectUserRoleSearch.forProjectId(prj.getProjectId());
        List<ProjectUserRole> roles = ccbc.searchProjectUserRoles(search);

        userRolesMap = CollectionsUtil.createMapSetNotNull(roles,
                new Transformer<ProjectUserRole, Long>() {

                    @Override
                    public Long transform(ProjectUserRole item) {
                        return item.getUserId();
                    }
                }, new Transformer<ProjectUserRole, String>() {

                    @Override
                    public String transform(ProjectUserRole item) {
                        if (projectRolesMap.containsKey(item.getRole())) {
                            return item.getRole();
                        }

                        return null;
                    }
                });
    }

    private void getUsers() {
        Map<Long, MiniUserInfo> userMap
                = InfoCacheHelper.getInstance(cycle).getMiniUserInfos(userRolesMap.keySet());

        ArrayList<MiniUserInfo> u = new ArrayList<>(userMap.values());

        for (Iterator<MiniUserInfo> it = u.iterator(); it.hasNext();) {
            MiniUserInfo miniUserInfo = it.next();
            if (!userRolesMap.containsKey(miniUserInfo.getUserId())) {
                it.remove();
            }
        }

        users = u;
    }

    private void sortUsers() {
        Collections.sort(users, new Comparator<MiniUserInfo>() {

            @Override
            public int compare(MiniUserInfo o1, MiniUserInfo o2) {
                return new CompareToBuilder().
                        append(o1.getDisplayName(), o2.getDisplayName()).
                        append(o1.getEmail(), o2.getEmail()).
                        build();
            }
        });
    }

    private ByteArrayOutputStream generateJson() {
        return new DataTablesJson<MiniUserInfo>() {

            @Override
            protected void encodeItem(MiniUserInfo item) throws IOException {
                generator.writeNumberField("userId", item.getUserId());
                generator.writeStringField("displayName", item.getDisplayName());
                generator.writeStringField("email", item.getEmail());
                generator.writeStringField("thumbnail", item.getThumbnail());
                generator.writeArrayFieldStart("roles");
                Set<String> userRoles = userRolesMap.get(item.getUserId());
                if (userRoles == null) {
                    userRoles = Collections.emptySet();
                }
                for (RoleUuidNamePair roleUuidNamePair : projectRoles) {
                    if (!userRoles.contains(roleUuidNamePair.getUuid())) {
                        continue;
                    }

                    generator.writeStartObject();
                    generator.writeStringField("role", roleUuidNamePair.getUuid());
                    generator.writeStringField("roleName", roleUuidNamePair.getName());
                    generator.writeStringField("deleteRoleLink", cycle.urlFor(
                            ProjectModificationModule.class, "revokeRole", strProjectId));
                    generator.writeEndObject();
                }
                generator.writeEndArray();
            }
        }.encodeToStream(users);
    }

    private void getCpRoles() {
        final CocoboxRoleUtil cru = new CocoboxRoleUtil();
        projectRolesMap = cru.getProjectRoles(cycle);
        projectRoles = cru.toSortedRoleUuidNamePairList(cycle, projectRolesMap);
    }

}
