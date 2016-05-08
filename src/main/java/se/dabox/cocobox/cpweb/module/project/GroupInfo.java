/*
 * Copyright (c) Dabox AB 2015 All Rights Reserved.
 *
 *
 */

package se.dabox.cocobox.cpweb.module.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import se.dabox.service.cug.client.ClientUserGroup;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class GroupInfo {
    private final long groupId;
    private final String name;
    private final List<GroupInfo> children;

    public static List<GroupInfo> fromCugs(List<ClientUserGroup> cugs) {
        final Map<Long, ClientUserGroup> cugHash = cugs.stream().
                collect(Collectors.toMap(ClientUserGroup::getGroupId, Function.identity()));
        final Map<Long, List<Long>> childrenHash = new HashMap<>();
        cugs.stream()
                .filter(cug -> cug.getParent() != null)
                .forEach(cug -> {
                    if (childrenHash.containsKey(cug.getParent())) {
                        childrenHash.get(cug.getParent()).add(cug.getGroupId());
                    } else {
                        childrenHash.put(cug.getParent(), new ArrayList<>(Arrays.asList(cug.getGroupId())));
                    }
                });
        return cugs.stream()
                .filter(cug -> cug.getParent() == null)
                .map(cug -> new GroupInfo(cug, cugHash, childrenHash))
                .collect(Collectors.toList());
    }

    private GroupInfo(ClientUserGroup cug, Map<Long, ClientUserGroup> cugHash, Map<Long, List<Long>> childrenHash) {
        this.groupId = cug.getGroupId();
        this.name = cug.getName();
        if (childrenHash.containsKey(groupId)) {
            children = childrenHash.get(groupId).stream()
                    .map(childId -> new GroupInfo(cugHash.get(childId), cugHash, childrenHash))
                    .collect(Collectors.toList());
        } else {
            children = Collections.emptyList();
        }
    }

    public long getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public List<GroupInfo> getChildren() {
        return children;
    }
}
