/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.details;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class DateTimeFormatter {
    private final DateFormat formatter;

    public DateTimeFormatter(TimeZone tz, Locale locale) {
        this.formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
        formatter.setTimeZone(tz);
    }

    public String format(Date date) {
        return formatter.format(date);
    }

}
