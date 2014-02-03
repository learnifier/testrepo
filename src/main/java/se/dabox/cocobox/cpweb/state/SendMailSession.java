/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebSession;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.cocobox.cpweb.module.mail.RequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.mail.SendMailModule;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.util.DateUtil;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class SendMailSession implements Serializable {
    private static final long serialVersionUID = 201204201114L;

    private final UUID uuid;
    private final Date created = new Date();
    private final ArrayList<Long> receivers = new ArrayList<>();
    private final ArrayList<Object> extraDatas = new ArrayList<>();
    private SendMailProcessor processor;
    private Map<String,? super Object> metaData;
    private final RequestTargetGenerator completedTargetGenerator;
    private final RequestTargetGenerator cancelTargetGenerator;
    private String stickyTemplateHint;
    private PortableMailTemplate portableMailTemplate;

    public SendMailSession(SendMailProcessor processor,
            RequestTargetGenerator completedTargetGenerator,
            RequestTargetGenerator cancelTargetGenerator) {
        this(processor, completedTargetGenerator, true, cancelTargetGenerator);
    }

    private SendMailSession(SendMailProcessor processor,
            RequestTargetGenerator completedTargetGenerator, boolean requiredTargetGenerator,
            RequestTargetGenerator cancelTargetGenerator) {
        ParamUtil.required(processor,"processor");
        if (requiredTargetGenerator) {
            ParamUtil.required(completedTargetGenerator, "completedTargetGenerator");
        }
        this.processor = processor;
        this.completedTargetGenerator = completedTargetGenerator;
        this.uuid = UUID.randomUUID();
        this.cancelTargetGenerator = cancelTargetGenerator;
    }

    /**
     * Creates a simple mail session without a completed target generator and cancel
     * target generator.
     * 
     * @param processor
     * @return
     */
    public static SendMailSession createSimple(SendMailProcessor processor) {
        return new SendMailSession(processor, null, false, null);
    }

    public String getStickyTemplateHint() {
        return stickyTemplateHint;
    }

    public void setStickyTemplateHint(String stickyTemplateHint) {
        this.stickyTemplateHint = stickyTemplateHint;
    }

    public List<Long> getReceivers() {
        return receivers;
    }

    public List<Object> getExtraDatas() {
        return extraDatas;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Date getCreated() {
        return DateUtil.clone(created);
    }

    public void ensureCapacity(int minCapacity) {
        receivers.ensureCapacity(minCapacity);
        extraDatas.ensureCapacity(minCapacity);
    }

    public void addReceiver(long receiver) {
        addReceiver(receiver, null);
    }

    public void addReceiver(long receiver, Object extraData) {
        receivers.add(receiver);
        extraDatas.add(extraData);
    }

    public SendMailProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(SendMailProcessor processor) {
        ParamUtil.required(processor,"processor");
        
        this.processor = processor;
    }

    public void addMetaData(String name, Object value) {
        if (metaData == null) {
            metaData = new HashMap<>();
        }

        metaData.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetaData(String name) {
        if (metaData == null) {
            return null;
        }

        return (T) metaData.get(name);
    }

    public PortableMailTemplate getPortableMailTemplate() {
        return portableMailTemplate;
    }

    public void setPortableMailTemplate(PortableMailTemplate portableMailTemplate) {
        this.portableMailTemplate = portableMailTemplate;
    }



    @Override
    public String toString() {
        return "SendMailSession{" + "uuid=" + uuid + ", created=" + created +
                ", receivers=" + receivers + ", extraDatas=" + extraDatas +
                ", processor=" + processor + ", metaData=" + metaData +
                ", stickyTemplateHint=" + stickyTemplateHint +
                ", completedTargetGenerator=" + completedTargetGenerator +
                ", portableMailTemplate=" + portableMailTemplate + '}';
    }

    public RequestTarget getCompletedRequestTarget(RequestCycle cycle) {
        return completedTargetGenerator.generateTarget(cycle);
    }

    public void processSendMail(RequestCycle cycle,
            SendMailTemplate smt) {
        getProcessor().processSendMail(cycle, this, smt);
    }

    public boolean verifySendMail(RequestCycle cycle, SendMailTemplate smt) {
        SendMailProcessor proc = getProcessor();

        if (proc instanceof SendMailVerifier) {
            SendMailVerifier verifier = (SendMailVerifier) proc;

            return verifier.verifySendMail(cycle, this, smt);
        }

        return true;
    }


    public void storeInSession(RequestCycle cycle) {
        storeInSession(cycle.getSession());
    }

    public void storeInSession(WebSession session) {
        session.setAttribute(getSessionName(uuid.toString()), this);
    }

    public void removeFromSession(WebSession session) {
        session.removeAttribute(getSessionName(uuid.toString()));
    }

    private static String getSessionName(String str) {
        return "sendmail."+str;
    }

    public static SendMailSession getFromSession(WebSession session, String uuidString) {
        if (session == null) {
            return null;
        }

        String name = getSessionName(uuidString);

        return (SendMailSession) session.getAttribute(name);
    }

    public RequestTargetGenerator getCancelTargetGenerator() {
        return cancelTargetGenerator;
    }
    
    public RequestTarget getPreSendTarget(long orgId) {
        return new WebModuleRedirectRequestTarget(SendMailModule.class,
                SendMailModule.VIEW_SENDMAIL_ACTION, Long.toString(orgId),
                uuid.toString());
    }

}
