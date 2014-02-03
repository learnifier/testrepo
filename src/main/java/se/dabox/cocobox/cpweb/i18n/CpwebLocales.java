/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.i18n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public final class CpwebLocales {

    private CpwebLocales() {
    }

    public static List<Locale> getProjectLocales() {
        return new ArrayList<Locale>(Arrays.asList(new Locale("en"), new Locale(
                "de"), new Locale("fr"), new Locale("es")));
    }
}
