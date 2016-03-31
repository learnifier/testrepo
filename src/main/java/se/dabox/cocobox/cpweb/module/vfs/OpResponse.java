/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.vfs;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class OpResponse {
    private final String status;
    private final Object response;

    public OpResponse(String status, Object response) {
        this.status = status;
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public Object getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "OpResponse{" + "status=" + status + ", response=" + response + '}';
    }
    
}
