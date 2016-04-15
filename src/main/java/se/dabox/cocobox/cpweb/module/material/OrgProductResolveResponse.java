/*
 * (c) Dabox AB 2015 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.material;

/**
 *
 * @author Jerker Klang (jerker.klang@learnifier.com)
 */
public class OrgProductResolveResponse {
    private final String status;
    private final String path;

    public OrgProductResolveResponse(String status) {
        this(status, null);
    }

    public OrgProductResolveResponse(String status, String path) {
        this.status = status;
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "OrgProductResolveResponse{" + "status=" + status + ", path=" + path + '}';
    }

}
