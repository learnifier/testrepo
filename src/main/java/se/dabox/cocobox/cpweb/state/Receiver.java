/*
 * (c) Dabox AB 2013 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import java.io.Serializable;
import se.dabox.util.ParamUtil;

/**
 * Representation of a receiver.
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class Receiver implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String name;
    private final String email;

    /**
     * Creates a receiver with a name and email
     *
     * @param name The name of the receiver
     * @param email The email of the receiver
     *
     * @throws IllegalArgumentException Thrown if {@code email} is null.
     */
    public Receiver(String name, String email) {
        this.name = name;
        this.email = email;
        ParamUtil.required(email, "email");
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "Receiver{" + "name=" + name + ", email=" + email + '}';
    }
    
}
