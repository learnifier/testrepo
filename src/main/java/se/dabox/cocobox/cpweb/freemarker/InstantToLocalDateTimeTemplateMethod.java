/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.freemarker;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class InstantToLocalDateTimeTemplateMethod implements TemplateMethodModelEx {
    private final ZoneId zoneId;

    public InstantToLocalDateTimeTemplateMethod(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        Object arg = arguments.get(0);

        if (arg == null) {
            return null;
        }

        Instant instant = (Instant) DeepUnwrap.permissiveUnwrap((TemplateModel) arg);

        return LocalDateTime.ofInstant(instant, zoneId);
    }

}
