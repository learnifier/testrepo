/*
 * (c) Dabox AB 2013 All Rights Reserved
 */

package se.dabox.cocobox.cpweb.module.project.report;

import java.util.concurrent.Callable;
import se.dabox.service.common.ajaxlongrun.StatusSource;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 * @param <T>
 */
public interface StatusCallable<T> extends Callable<T>, StatusSource {

}
