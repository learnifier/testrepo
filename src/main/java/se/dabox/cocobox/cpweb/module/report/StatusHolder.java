/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.report;

import se.dabox.service.common.ajaxlongrun.Status;

/**
 * Interface for classes that holds a status object
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public interface StatusHolder {

    public void setStatus(Status status);
}
