/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.site.user_portal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hivemind.Messages;
import org.apache.lucene.queryParser.ParseException;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.Persist;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.valid.ValidationConstraint;
import org.apache.tapestry.web.WebResponse;
import org.sipfoundry.sipxconfig.common.SipUri;
import org.sipfoundry.sipxconfig.common.User;
import org.sipfoundry.sipxconfig.components.SipxValidationDelegate;
import org.sipfoundry.sipxconfig.components.TapestryUtils;
import org.sipfoundry.sipxconfig.domain.DomainManager;
import org.sipfoundry.sipxconfig.phone.SipService;
import org.sipfoundry.sipxconfig.phonebook.Phonebook;
import org.sipfoundry.sipxconfig.phonebook.PhonebookEntry;
import org.sipfoundry.sipxconfig.phonebook.PhonebookManager;

public abstract class UserPhonebookPage extends UserBasePage implements PageBeginRenderListener {
    private static final Log LOG = LogFactory.getLog(UserPhonebookPage.class);

    @InjectObject(value = "service:tapestry.globals.WebResponse")
    public abstract WebResponse getResponse();

    @InjectObject("spring:phonebookManager")
    public abstract PhonebookManager getPhonebookManager();

    @InjectObject("spring:sip")
    public abstract SipService getSipService();

    @InjectObject("spring:domainManager")
    public abstract DomainManager getDomainManager();

    @Persist
    public abstract void setQuery(String query);

    public abstract String getQuery();

    @Persist
    public abstract Collection<PhonebookEntry> getPhonebookEntries();

    public abstract void setPhonebookEntries(Collection<PhonebookEntry> entries);

    /**
     * comma-separated list of extensions for the user in the current row
     */
    public abstract String getExtensionsForCurrentEntry();

    public abstract void setExtensionsForCurrentEntry(String value);

    /**
     * comma-separated list of sip id's for the user in the current row
     */
    public abstract String getSipIdsForCurrentEntry();

    public abstract void setSipIdsForCurrentEntry(String value);

    public abstract void setCurrentNumber(String number);

    public void pageBeginRender(PageEvent event) {
        super.pageBeginRender(event);

        if (getPhonebookEntries() == null) {
            initializeEntries();
        }
    }

    private void initializeEntries() {
        String query = getQuery();
        Collection<Phonebook> phonebooks = getPhonebooks();
        Collection<PhonebookEntry> entries = null;
        if (StringUtils.isEmpty(query)) {
            entries = getPhonebookManager().getEntries(phonebooks);
        } else {
            entries = getPhonebookManager().search(phonebooks, query);
        }
        setPhonebookEntries(entries);
    }

    /**
     * Implements click to call link
     * 
     * @param number number to call - refer is sent to current user
     */
    public void call(String number) {
        String domain = getDomainManager().getDomain().getName();
        String userAddrSpec = getUser().getAddrSpec(domain);
        String destAddrSpec = SipUri.format(number, domain, false);
        getSipService().sendRefer(userAddrSpec, destAddrSpec);
    }

    /**
     * Filters the phonebook entries based on the value of getQuery()
     */
    public void search() throws IOException, ParseException {
        setPhonebookEntries(null);
    }

    public void reset() {
        setQuery(StringUtils.EMPTY);
        setPhonebookEntries(null);
    }

    private Collection<Phonebook> getPhonebooks() {
        User user = getUser();
        return getPhonebookManager().getPhonebooksByUser(user);
    }

    private User getUserForEntry(PhonebookEntry entry) {
        return getCoreContext().loadUserByUserName(entry.getNumber());
    }

    /**
     * Called whenever new row is about to displayed. Sorts entries into extensions (that look
     * like phone numbers) and sipIds (that look like SIP usernames)
     * 
     * @param entry phone book entry
     */
    public void setPhonebookEntry(PhonebookEntry entry) {
        User user = getUserForEntry(entry);
        AliasSorter aliasSorter = new AliasSorter(user, entry);
        Messages messages = getMessages();
        setCurrentNumber(entry.getNumber());
        setExtensionsForCurrentEntry(aliasSorter.getExtensions(messages));
        setSipIdsForCurrentEntry(aliasSorter.getSipIds(messages));
    }

    static class AliasSorter {
        private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\d*");

        private List<String> m_sipIds = new ArrayList<String>();
        private List<String> m_extensions = new ArrayList<String>();

        public AliasSorter(User user, PhonebookEntry entry) {
            if (user == null) {
                addAlias(entry.getNumber());

            } else {
                addAlias(user.getName());
                for (String alias : user.getAliases()) {
                    addAlias(alias);
                }
            }
        }

        public String getSipIds(Messages messages) {
            return asString(m_sipIds, messages);
        }

        public String getExtensions(Messages messages) {
            return asString(m_extensions, messages);
        }

        private void addAlias(String alias) {
            Matcher m = EXTENSION_PATTERN.matcher(alias);
            List<String> l = m.matches() ? m_extensions : m_sipIds;
            l.add(alias);
        }

        private String asString(List<String> aliases, Messages messages) {
            if (aliases.isEmpty()) {
                return messages.getMessage("label.unknown");
            }

            return StringUtils.join(aliases, ", ");
        }
    }

    public void export() {
        SipxValidationDelegate validator = (SipxValidationDelegate) TapestryUtils.getValidator(this);
        try {
            String name = String.format("phonebook_%s.vcf", getUser().getUserName());
            OutputStream stream = TapestryUtils.getResponseOutputStream(getResponse(), name, "text/x-vcard");
            Collection<PhonebookEntry> entries = getPhonebookEntries();
            getPhonebookManager().exportPhonebook(entries, stream);
            stream.close();
        } catch (IOException e) {
            LOG.error("Cannot export phonebook", e);
            Messages messages = getMessages();
            validator.record(messages.format("msg.exportError", e.getLocalizedMessage()),
                    ValidationConstraint.CONSISTENCY);
        }
    }
}
