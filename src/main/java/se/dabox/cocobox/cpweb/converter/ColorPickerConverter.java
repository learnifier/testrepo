/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.converter;

import java.util.Locale;
import java.util.regex.Pattern;
import se.dabox.util.converter.ConversionContext;
import se.dabox.util.converter.impl.RawConverter;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class ColorPickerConverter implements RawConverter {
    private static final Pattern COLOR_PATTERN = Pattern.compile("#[0-9A-F]{6}");

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(ConversionContext context, Object source,
            Class<T> targetClass) {

        String str = (String)source;
        str = str.toUpperCase(Locale.ENGLISH);

        if (COLOR_PATTERN.matcher(str).matches()) {
            return (T) str;
        }

        return null;
    }

    @Override
    public boolean canConvert(Class<?> sourceClass,
            Class<?> targetClass) {
        return String.class.isAssignableFrom(targetClass);
    }

}
