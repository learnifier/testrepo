/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

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

    /**
     * Performs a detailed check if the email is valid. A well formed email is not guaranteed to be
     * valid. This method utilizes existing email validation libraries.
     *
     * @param string A String to check
     *
     * @return True if the string is a well formed email string.
     */
    public boolean isValidEmailDetailed(String string) {
        return isValidEmail(string);
    }

    /**
     * Performs a basic check if the email is well formed. A well formed email is not guaranteed to
     * be valid. The following conditions are checked.
     *
     * <ul>
     * <li>Not valid if the string contains , (comma) characters (although strictly speaking it's legal)</li>
     * <li>Not valid if the string contains whitespace characters</li>
     * <li>Not valid if the numbers of @ chars are not exactly 1</li>
     * <li>Not valid if there are two consecutive . (dot) characters</li>
     * </ul>
     *
     *
     * @param string A String to check
     * @return True if the string is a well formed email string.
     */
    @Override
    public boolean isValidEmail(String string) {
        return  !StringUtils.contains(string, ',') &&
                !StringUtils.containsWhitespace(string) &&
                StringUtils.countMatches(string, "@") == 1 &&
                StringUtils.countMatches(string, "..") == 0;
    }

}
