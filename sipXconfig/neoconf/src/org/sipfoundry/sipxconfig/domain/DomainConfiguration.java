/*
 * 
 * 
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.  
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.domain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.sipfoundry.sipxconfig.admin.dialplan.config.ConfigFileType;
import org.sipfoundry.sipxconfig.admin.dialplan.config.ConfigurationFile;

public class DomainConfiguration implements ConfigurationFile {

    private VelocityEngine m_velocityEngine;
    private String m_templateLocation = "commserver/domain-config.vm";
    private String m_domainConfig;

    public void generate(Domain domain, String realm, String language) {
        StringWriter writer = new StringWriter();
        generate(domain, realm, language, writer);
        m_domainConfig = writer.toString();
    }

    public void generate(Domain domain, String realm, String language, Writer output) {
        try {
            VelocityContext context = new VelocityContext();
            context.put("domain", domain);
            context.put("realm", realm);
            context.put("language", language);
            m_velocityEngine.mergeTemplate(getTemplate(), context, output);
            output.flush();
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException(e);
        } catch (ParseErrorException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setVelocityEngine(VelocityEngine velocityEngine) {
        m_velocityEngine = velocityEngine;
    }

    public String getTemplate() {
        return m_templateLocation;
    }

    public void setTemplate(String template) {
        m_templateLocation = template;
    }

    public String getFileContent() {
        return "file";
    }

    public ConfigFileType getType() {
        return ConfigFileType.DOMAIN_CONFIG;
    }

    public void write(Writer writer) throws IOException {
        writer.write(m_domainConfig);
    }

    public void writeToFile(File configDir, String filename) throws IOException {
        File outputFile = new File(configDir, filename);
        FileWriter writer = new FileWriter(outputFile);
        writer.write(m_domainConfig);
    }
}
