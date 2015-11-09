/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.state;

import se.dabox.service.common.mailsender.SendMailRequest;

/**
 *
 * @author Jerker Klang (jerker.klang@dabox.se)
 */
public class SendMailTemplate {
    private final String subject;
    private final String body;
    private final String format;

    public SendMailTemplate(String subject, String body, String format) {
        this.subject = subject;
        this.body = body;
        this.format = format;
    }

    public String getBody() {
        return body;
    }

    public String getFormat() {
        return format;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "SendMailTemplate{" + "subject=" + subject + ", body=" + body +
                ", format=" + format + '}';
    }

    public void toSendMailRequest(SendMailRequest req) {
        req.setHtmlBody(body);
        req.setSubject(subject);
        req.setTemplateType(format);
    }
    
}
