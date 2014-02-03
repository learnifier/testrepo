/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.module.mail;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import se.dabox.service.common.mailsender.mailtemplate.MailTemplate;
import se.dabox.util.ParamUtil;
import se.dabox.util.collections.CollectionsUtil;
import se.dabox.util.collections.Predicate;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class TemplateLists {
    private final Locale locale;

    private List<MailTemplate> mailTemplates;

    private List<MailTemplate> stickyList;
    private List<MailTemplate> unstickyList;
    private boolean initialized;

    public TemplateLists(Locale locale) {
        this.locale = locale;
    }

    public TemplateLists addTemplate(MailTemplate template) {
        ParamUtil.required(template,"template");
        ensureList();
        mailTemplates.add(template);
        reset();

        return this;
    }

    public TemplateLists addTemplates(Collection<MailTemplate> templates) {
        if (templates == null) {
            return this;
        }
        
        if (mailTemplates == null) {
            mailTemplates = new ArrayList<MailTemplate>(templates);
        } else {
            mailTemplates.addAll(templates);
        }
        
        reset();

        return this;
    }

    public List<MailTemplate> getStickyList() {
        ensureInnerState();

        return stickyList;
    }

    public List<MailTemplate> getUnstickyList() {
        ensureInnerState();

        return unstickyList;
    }

    private void ensureList() {
        if (mailTemplates == null) {
            mailTemplates = new ArrayList<MailTemplate>();
        }
    }

    private void reset() {
        initialized = false;
        stickyList = null;
        unstickyList = null;
    }

    private void ensureInnerState() {
        if(initialized) {
            return;
        }

        if (mailTemplates == null) {
            stickyList = Collections.emptyList();
            unstickyList = Collections.emptyList();
            return;
        }

        final Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.TERTIARY);
        Collections.sort(mailTemplates, new Comparator<MailTemplate>() {


            @Override
            public int compare(MailTemplate o1, MailTemplate o2) {
                int diff = collator.compare(o1.getName(), o2.getName());

                if (diff != 0) {
                    return diff;
                }

                return o1.getId() < o2.getId() ? -1 : 1;
            }
        });

        stickyList = CollectionsUtil.sublist(mailTemplates, new Predicate<MailTemplate>() {

            @Override
            public boolean evalute(MailTemplate obj) {
                return obj.getSticky();
            }
        });

        unstickyList = CollectionsUtil.sublist(mailTemplates, new Predicate<MailTemplate>() {

            @Override
            public boolean evalute(MailTemplate obj) {
                return !obj.getSticky();
            }
        });

        initialized = true;        
    }

}
