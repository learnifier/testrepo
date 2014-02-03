/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.report;

import net.unixdeveloper.druwa.ServiceApplication;
import se.dabox.service.common.RealmBackgroundCallable;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 * @param <T>
 */
public abstract class ReportCallable<T> extends RealmBackgroundCallable<T> implements
        StatusCallable<T> {

    public ReportCallable(ServiceApplication app) {
        super(app);
    }


}
