/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.unixdeveloper.druwa.DruwaApplication;
import net.unixdeveloper.druwa.RequestCycle;
import net.unixdeveloper.druwa.RequestTarget;
import net.unixdeveloper.druwa.WebSession;
import net.unixdeveloper.druwa.request.WebModuleRedirectRequestTarget;
import se.dabox.cocobox.cpweb.module.mail.AjaxSendMailProcessor;
import se.dabox.cocobox.cpweb.module.mail.RequestTargetGenerator;
import se.dabox.cocobox.cpweb.module.mail.SendMailModule;
import se.dabox.cocobox.cpweb.module.mail.SendMailProcessor;
import se.dabox.cocosite.modal.ModalParamsHelper;
import se.dabox.service.common.mailsender.pmt.PortableMailTemplate;
import se.dabox.util.DateUtil;
import se.dabox.util.ParamUtil;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class SendMailSession implements Serializable {
    private static final long serialVersionUID = 201410071105L;

    private final UUID uuid;
    private final Date created = new Date();
    private final ArrayList<Long> receivers = new ArrayList<>();
    private final ArrayList<Receiver> displayReceivers = new ArrayList<>();
    private final ArrayList<Object> extraDatas = new ArrayList<>();
    private SendMailProcessor processor;
    private Map<String,? super Object> metaData;
    private final RequestTargetGenerator completedTargetGenerator;
    private final RequestTargetGenerator cancelTargetGenerator;
    private String stickyTemplateHint;
    private Locale stickyTemplateLocale;
    private boolean stickyHidesDropdown = true;
    private PortableMailTemplate portableMailTemplate;
    private String skin;
    private String sendButtonText;
    private boolean ajaxProcessor;

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
        this.ajaxProcessor = processor instanceof AjaxSendMailProcessor;
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

    /**
     * Sets a sticky template hint for this session. If a hint is sent that
     * template is resolved and presented for the user directly.
     *
     * @param stickyTemplateHint
     */
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

    /**
     * Optimization routine to ensure that internal structures can store the specified
     * amount of receivers.
     *
     * @param minCapacity A positive integer with the numbers of receivers expected to exist in
     * this session.
     */
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

    /**
     * Adds a receiver that is only used for display.
     *
     * @param receiver The receiver to add
     *
     * @throws IllegalArgumentException Thrown if receiver is null
     */
    public void addDisplayReceiver(Receiver receiver) {
        ParamUtil.required(receiver, "receiver");

        displayReceivers.add(receiver);
    }

    /**
     * Returns a list with display receivers.
     *
     * @return A list with the display receivers.
     */
    public List<Receiver> getDisplayReceivers() {
        return displayReceivers;
    }

    public SendMailProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(SendMailProcessor processor) {
        ParamUtil.required(processor,"processor");
        
        this.processor = processor;
    }

    /**
     * Adds some metadata to this session. All entries stored as metadata must be
     * serializable.
     *
     * @param name The name of the metadata
     * @param value The value or the entry
     *
     * @throws IllegalArgumentException Thrown if the value is not null and not Serializable
     */
    public void addMetaData(String name, Object value) {        
        if (value != null && !(value instanceof Serializable)) {
            throw new IllegalArgumentException("Value is not serializable");
        }
        
        if (metaData == null) {
            metaData = new HashMap<>();
        }

        metaData.put(name, value);
    }

    /**
     * Returns a metadata value associated with the specified name.
     *
     * @param <T> The type of the metadata value
     * @param name The name of the metadata entry
     * @return The value or {@code null} if no value with the specified name was found.
     */
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

    /**
     * Sets a portable mail template to this session. If this value is set it is used
     * as the default value for the send session.
     *
     * @param portableMailTemplate
     */
    public void setPortableMailTemplate(PortableMailTemplate portableMailTemplate) {
        this.portableMailTemplate = portableMailTemplate;
    }

    /**
     * Gets the preferred locale of the sticky template. This value has no function
     * unless a sticky template hint is set.
     *
     * @return The preferred locale or {@code null}.
     */
    public Locale getStickyTemplateLocale() {
        return stickyTemplateLocale;
    }

    /**
     * Sets the preferred locale of the sticky template. This value has no function
     * unless a sticky template hint is set.
     *
     * @param stickyTemplateLocale The preferred locale or {@code null}.
     *
     * @see #setStickyTemplateHint(java.lang.String)
     */
    public void setStickyTemplateLocale(Locale stickyTemplateLocale) {
        this.stickyTemplateLocale = stickyTemplateLocale;
    }

    /**
     * Determines if a sticky template should hide the dropdown of mail templates
     * (true is default).
     *
     * @return True if a sticky template should hide the drop down
     */
    public boolean isStickyHidesDropdown() {
        return stickyHidesDropdown;
    }

    public void setStickyHidesDropdown(boolean stickyHidesDropdown) {
        this.stickyHidesDropdown = stickyHidesDropdown;
    }

    /**
     * Determines if the dropdown of mail templates is enabled.
     *
     * @return True if the dropdown with mail templates should be visible.
     */
    public boolean isDropdownEnabled() {
        return stickyTemplateHint == null || !stickyHidesDropdown;
    }

    @Override
    public String toString() {
        return "SendMailSession{" + "uuid=" + uuid + ", created=" + created + ", receivers=" +
                receivers + ", extraDatas=" + extraDatas + ", processor=" + processor + ", metaData=" +
                metaData + ", completedTargetGenerator=" + completedTargetGenerator +
                ", cancelTargetGenerator=" + cancelTargetGenerator + ", stickyTemplateHint=" +
                stickyTemplateHint + ", stickyTemplateLocale=" + stickyTemplateLocale +
                ", stickyHidesDropdown=" + stickyHidesDropdown + ", portableMailTemplate=" +
                portableMailTemplate + '}';
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

    /**
     * Stores this object into a web session.
     *
     * This method does roughly the same as {@code storeInSession(cycle.getSession())}.
     *
     * @param cycle A RequestCycle
     */
    public void storeInSession(RequestCycle cycle) {
        storeInSession(cycle.getSession());
    }

    /**
     * Stores this object into the specified web session.
     *
     *
     * @param session The session to store the data in
     */
    public void storeInSession(WebSession session) {
        session.setAttribute(getSessionName(uuid.toString()), this);
    }

    /**
     * Remove this object from the session if it is bound to it.
     *
     * @param session The session to unbound this object from.
     */
    public void removeFromSession(WebSession session) {
        session.removeAttribute(getSessionName(uuid.toString()));
    }

    private static String getSessionName(String str) {
        return "sendmail."+str;
    }

    /**
     * Returns the skin the edit screen should use. This method never returns null.
     *
     * @return The skin name
     */
    public String getSkin() {
        return skin == null ? "CPAuth3" : skin;
    }

    /**
     * Sets the skin the edit screen should use.
     *
     * @param skin The name of the skin; {@code null} for default.
     */
    public void setSkin(String skin) {
        this.skin = skin;
    }

    /**
     * Gets the overriden send button text.
     *
     * @return The overriden send button text or {@code null} if the default should be used.
     */
    public String getSendButtonText() {
        return sendButtonText;
    }

    /**
     * Overrides the text to show on the send button.
     *
     * @param sendButtonText The button text or {@code null} to override.
     */
    public void setSendButtonText(String sendButtonText) {
        this.sendButtonText = sendButtonText;
    }



    /**
     * Returns a SendMailSession from a WebSession that match the specified uuid.
     *
     * @param session The WebSession to retreive the session from
     * @param uuidString The uuid string that matches a SendMailSession uuid
     * @return A SendMailSession or {@code null} if no matching entry was found.
     */
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
        WebModuleRedirectRequestTarget target
                = new WebModuleRedirectRequestTarget(SendMailModule.class,
                        SendMailModule.VIEW_SENDMAIL_ACTION, Long.toString(orgId),
                        uuid.toString());

        RequestCycle cycle = DruwaApplication.getCurrentRequestCycle();
        String extraParams = ModalParamsHelper.getParameterString(cycle);

        target.setExtraTargetParameterString(extraParams);

        return target;
    }

    /**
     * Determines if the mail processor for this session should be executed as a
     * ajax longrun task. The default is autodetected from the processor.
     *
     * @return True if the mail processor for this session should be executed as a
     * ajax longrun task
     */
    public boolean isAjaxProcessor() {
        return ajaxProcessor;
    }

    /**
     * Sets if the mail processor for this session should be executed as a
     * ajax longrun task. The default is autodetected from the processor.
     *
     * @param ajaxProcessor True if the mail processor for this session should be executed as a
     * ajax longrun task.
     */
    public void setAjaxProcessor(boolean ajaxProcessor) {
        this.ajaxProcessor = ajaxProcessor;
    }

}
