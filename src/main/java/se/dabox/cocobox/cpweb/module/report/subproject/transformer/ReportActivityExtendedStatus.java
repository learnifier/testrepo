/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report.subproject.transformer;

import se.dabox.service.common.coursedesign.extstatus.ExtendedStatus;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
class ReportActivityExtendedStatus {
    private final String title;
    private final ExtendedStatus extendedStatus;

    public ReportActivityExtendedStatus(String title, ExtendedStatus extendedStatus) {
        this.title = title;
        this.extendedStatus = extendedStatus;
    }

    public String getTitle() {
        return title;
    }

    public ExtendedStatus getExtendedStatus() {
        return extendedStatus;
    }

    @Override
    public String toString() {
        return "ActivityExtendedStatus{" + "title=" + title + ", extendedStatus=" + extendedStatus +
                '}';
    }

}
