/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.converter;

import se.dabox.util.converter.ConversionContext;
import se.dabox.util.converter.impl.RawConverter;
import se.dabox.util.email.SimpleEmailValidator;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class SimpleEmailConverter implements RawConverter {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convert(ConversionContext context, Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        String str = (String) source;

        if (SimpleEmailValidator.getInstance().isValidEmail(str)) {
            return (T) str;
        }

        return null;
    }

    @Override
    public boolean canConvert(
            Class<?> sourceClass,
            Class<?> targetClass) {
        return String.class.isAssignableFrom(targetClass);
    }

}
