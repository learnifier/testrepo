/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject.transformer;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class SubprojectParticipant {
    private final long participationId;
    private final long projectId;
    private final long userId;

    private String name;
    private String email;

    private Long masterProject;
    private String masterProjectName;

    private List<ReportActivityExtendedStatus> activity = Collections.emptyList();

    public SubprojectParticipant(long participationId, long projectId, long userId) {
        this.participationId = participationId;
        this.projectId = projectId;
        this.userId = userId;
    }

    public long getParticipationId() {
        return participationId;
    }

    public long getProjectId() {
        return projectId;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getMasterProject() {
        return masterProject;
    }

    public void setMasterProject(Long masterProject) {
        this.masterProject = masterProject;
    }

    public String getMasterProjectName() {
        return masterProjectName;
    }

    public void setMasterProjectName(String masterProjectName) {
        this.masterProjectName = masterProjectName;
    }

    public List<ReportActivityExtendedStatus> getActivity() {
        return activity;
    }

    public void setActivity(List<ReportActivityExtendedStatus> activity) {
        this.activity = activity;
    }

    public long getUserId() {
        return userId;
    }

}
