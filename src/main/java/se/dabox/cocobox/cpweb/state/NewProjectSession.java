/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebSession;
import se.dabox.cocobox.cpweb.formdata.project.CreateProjectGeneral;
import se.dabox.cocobox.cpweb.formdata.project.MatListProjectDetailsForm;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class NewProjectSession implements Serializable {
    private static final long serialVersionUID = 2L;

    public static String getSessionName(String strNpsId) {
        return "nps."+strNpsId;
    }

    private final UUID uuid = UUID.randomUUID();
    private final Date created = new Date();

    private final String type;
    private List<Long> orgmats;
    private List<String> prods;
    private final NewProjectSessionProcessor processor;
    private final String cancelTarget;
    private final Long designId;
    private boolean editMode;
    private CreateProjectGeneral createProjectGeneral;
    private String productId;

    public NewProjectSession(String type,
            List<Long> orgmats,
            List<String> products, NewProjectSessionProcessor processor, String cancelUrl,
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

    @Override
    public String toString() {
        return "NewProjectSession{" + "uuid=" + uuid + ", created=" + created + ", type=" + type +
                ", orgmats=" + orgmats + ", prods=" + prods + ", processor=" + processor +
                ", cancelTarget=" + cancelTarget + ", designId=" + designId + ", editMode=" +
                editMode + ", createProjectGeneral=" + createProjectGeneral + ", productId=" +
                productId + '}';
    }
    
    public RequestTarget process(RequestCycle cycle,
            MatListProjectDetailsForm matListDetails) {
        return processor.processSession(cycle, this, matListDetails);
    }
    
}
