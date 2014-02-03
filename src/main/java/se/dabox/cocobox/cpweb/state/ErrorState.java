/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import se.dabox.service.proddir.data.Product;
import se.dabox.util.ParamUtil;

/**
 * Creates an error state. orgId (used for branding) and exception are mandatory.
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class ErrorState {

    private final Product product;
    private final Exception exception;
    private final Long projectId;
    private final long orgId;

    public ErrorState(long orgId, Product product, Exception exception, Long projectId) {
        this.orgId = orgId;
        this.product = product;
        this.exception = exception;
        this.projectId = projectId;
        ParamUtil.required(exception, "exception");
    }

    /**
     * Returns the org unit branding. 
     * @return 
     */
    public long getOrgId() {
        return orgId;
    }

    public Product getProduct() {
        return product;
    }

    public Exception getException() {
        return exception;
    }

    public Long getProjectId() {
        return projectId;
    }

    @Override
    public String toString() {
        return "ErrorState{" + "product=" + product + ", exception=" + exception + ", projectId="
                + projectId + '}';
    }
}
