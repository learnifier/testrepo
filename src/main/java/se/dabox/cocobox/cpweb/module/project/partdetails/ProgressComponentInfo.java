/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.project.partdetails;

import java.util.Date;
import java.util.UUID;
import se.dabox.service.common.ccbc.material.OrgMaterial;
import se.dabox.service.common.ccbc.project.TemporalProgressComponent;
import se.dabox.service.common.coursedesign.v1.Component;
import se.dabox.service.proddir.data.Product;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
class ProgressComponentInfo {

    private final UUID cid;
    private final ProgressComponentType type;
    private Product product;
    private Component component;
    private OrgMaterial orgMat;
    private Date completed;

    private ProgressComponentInfo(UUID cid, ProgressComponentType type, Product product,
            Component component, OrgMaterial orgMat, Date completed) {
        ParamUtil.required(cid,"cid");
        ParamUtil.required(type,"type");
        this.cid = cid;
        this.type = type;
        this.product = product;
        this.component = component;
        this.orgMat = orgMat;
        this.completed = completed;
    }

    static ProgressComponentInfo forProduct(UUID cid, ProgressComponentType type, Product product) {
        return new ProgressComponentInfo(cid, type, product, null, null, null);
    }

    static ProgressComponentInfo forOrgMat(UUID cid, OrgMaterial orgMat) {
        return new ProgressComponentInfo(cid, ProgressComponentType.ORGMAT,
                null, null, orgMat, null);
    }

    static ProgressComponentInfo forTemporal(TemporalProgressComponent tempComp, Component comp) {
        return new ProgressComponentInfo(comp.getCid(), ProgressComponentType.TEMPORAL, null, comp,
                null, null);
    }

    public UUID getCid() {
        return cid;
    }

    public ProgressComponentType getType() {
        return type;
    }

    public Product getProduct() {
        return product;
    }

    public Component getComponent() {
        return component;
    }

    public OrgMaterial getOrgMat() {
        return orgMat;
    }

    public Date getCompleted() {
        return completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public void setOrgMat(OrgMaterial orgMat) {
        this.orgMat = orgMat;
    }
    
    @Override
    public String toString() {
        return "ProgressComponentInfo{" + "cid=" + cid + ", type=" + type + ", product=" + product +
                ", component=" + component + ", orgMat=" + orgMat + ", completed=" + completed + '}';
    }

    
}
