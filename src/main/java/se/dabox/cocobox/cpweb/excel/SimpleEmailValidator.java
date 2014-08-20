/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

import java.util.regex.Pattern;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class SimpleEmailValidator implements EmailValidator {
    private static final Pattern PATTERN = Pattern.compile("[^@]+@[^@]+");

    private static final SimpleEmailValidator INSTANCE =
            new SimpleEmailValidator();

    private SimpleEmailValidator() {
    }

    public static SimpleEmailValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidEmail(String string) {
        return PATTERN.matcher(string).matches();
    }

}
