/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import se.dabox.util.ParamUtil;

/**
 * Container of a sender of an email. Can contain both name and always an e-mail address.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class MailSender {
    private final String name;
    private final String email;

    /**
     * Creates a MailSender object with a name and e-mail address
     *
     * @param name The display name of the sender. Can be <code>null</code>
     * @param email The e-mail address of the sender.
     *
     * @throws IllegalArgumentException Thrown if <code>email</code> is <code>null</code>.
     */
    public MailSender(String name, String email) {
        this.name = name;
        this.email = email;
        ParamUtil.required(email,"email");
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "MailSender{" + "name=" + name + ", email=" + email + '}';
    }
    
}
