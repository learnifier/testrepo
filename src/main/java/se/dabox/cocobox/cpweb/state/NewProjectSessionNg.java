/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebSession;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.cocobox.cpweb.module.project.productconfig.ExtraProductConfig;
import se.dabox.service.common.ccbc.project.Project;
import se.dabox.util.ParamUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Magnus Andersson (magnus.andersson@learnifier.com)
 */
public class NewProjectSessionNg implements Serializable {
    private static final long serialVersionUID = 2L;

    public static String getSessionName(String strNpsId) {
        return "nps."+strNpsId;
    }

    private final UUID uuid = UUID.randomUUID();
    private final Date created = new Date();

    private final String type;
    private List<Long> orgmats;
    private List<String> prods;
    private final NewProjectSessionProcessorNg processor;
    private final String cancelTarget;
    private final Long designId;
    private boolean editMode;
    private CreateProjectGeneral createProjectGeneral;
    private final String productId;
    private List<ExtraProductConfig> extraConfig;
    private Integer courseId;


    public NewProjectSessionNg(String type,
                               List<Long> orgmats,
                               List<String> products, NewProjectSessionProcessorNg processor, String cancelUrl,
                               Long designId,
                               String productId) {
        ParamUtil.required(processor,"processor");
        this.type = type;
        this.orgmats = orgmats;
        this.prods = products;
        this.processor = processor;
        this.cancelTarget = cancelUrl;
        this.designId = designId;
        this.productId = productId;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public Date getCreated() {
        return created;
    }

    public List<Long> getOrgmats() {
        return orgmats;
    }

    public List<String> getProds() {
        return prods;
    }
    
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getType() {
        return type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void storeInSession(WebSession session) {
        session.setAttribute(getSessionName(getUuid().toString()), this);
    }

    public String getCancelTarget() {
        return cancelTarget;
    }

    public Long getDesignId() {
        return designId;
    }

    public void setOrgmats(List<Long> orgmats) {
        this.orgmats = orgmats;
    }

    public void setProds(List<String> prods) {
        this.prods = prods;
    }

    public CreateProjectGeneral getCreateProjectGeneral() {
        return createProjectGeneral;
    }

    public void setCreateProjectGeneral(CreateProjectGeneral createProjectGeneral) {
        this.createProjectGeneral = createProjectGeneral;
    }

    public String getProductId() {
        return productId;
    }

    public List<ExtraProductConfig> getExtraConfig() {
        return extraConfig;
    }

    public void setExtraConfig(List<ExtraProductConfig> extraConfig) {
        this.extraConfig = extraConfig;
    }

    public Map<String, String> getProductExtraConfig(String productId) {
        for (ExtraProductConfig config : extraConfig) {
            if (config.getProductId().equals(productId)) {
                return config.getSettings();
            }
        }

        return Collections.emptyMap();
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getCourseId() {
        return courseId;
    }


    @Override
    public String toString() {
        return "NewProjectSession{" +
                "uuid=" + uuid +
                ", created=" + created +
                ", type='" + type + '\'' +
                ", orgmats=" + orgmats +
                ", prods=" + prods +
                ", processor=" + processor +
                ", cancelTarget='" + cancelTarget + '\'' +
                ", designId=" + designId +
                ", editMode=" + editMode +
                ", createProjectGeneral=" + createProjectGeneral +
                ", productId='" + productId + '\'' +
                ", extraConfig=" + extraConfig +
                ", courseId=" + courseId +
                '}';
    }

    public Project process(RequestCycle cycle,
                           MatListProjectDetailsForm matListDetails) {

        return processor.processSession(cycle, this, matListDetails);
    }
    
}
