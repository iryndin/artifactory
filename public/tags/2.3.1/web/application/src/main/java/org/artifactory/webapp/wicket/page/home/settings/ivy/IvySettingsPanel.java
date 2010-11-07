/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.home.settings.ivy;

import org.apache.commons.lang.StringUtils;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.home.settings.BaseIvySettingsGeneratorPanel;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Enables the user to select a virtual repo that are configured in the system and if needed, to modify resolver name,
 * artifact patterns and Ivy patterns to generate an ivysettings.xml
 *
 * @author Noam Y. Tenne
 */
public class IvySettingsPanel extends BaseIvySettingsGeneratorPanel {

    private static final Logger log = LoggerFactory.getLogger(IvySettingsPanel.class);

    @WicketProperty
    private VirtualRepoEntry repository;

    @WicketProperty
    private String resolverName;

    /**
     * Main constructor
     *
     * @param id                ID to assign to the panel
     * @param servletContextUrl Running context URL
     * @param virtualRepoKeyMap Virtual repo key association map
     */
    protected IvySettingsPanel(String id, String servletContextUrl, Map<String, String> virtualRepoKeyMap) {
        super(id, servletContextUrl, virtualRepoKeyMap);

        addRepoDropDown("repository", this, "libs");
        addTextField("resolverName", this, "", true, false);
    }

    public String generateSettings(String servletContextUrl) {
        Document document = new Document();
        Element rootNode = new Element("ivy-settings");

        Element settingsElement = new Element("settings");
        settingsElement.setAttribute("defaultResolver", "main");
        rootNode.addContent(settingsElement);
        rootNode.addContent(new Comment("Authentication required for publishing (deployment). 'Artifactory Realm' is " +
                "the realm used by Artifactory so don't change it."));

        Element credentialsElement = new Element("CREDENTIALS");
        try {
            credentialsElement.setAttribute("HOST", new URL(servletContextUrl).getHost());
        } catch (MalformedURLException e) {
            String errorMessage =
                    "An error occurred while decoding the servlet context URL for the credentials host attribute: ";
            error(errorMessage + e.getMessage());
            log.error(errorMessage, e);
        }
        credentialsElement.setAttribute("REALM", "Artifactory Realm");

        credentialsElement.setAttribute("USERNAME", authorizationService.currentUsername());
        credentialsElement.setAttribute("PASSWD", "yourPassword");

        rootNode.addContent(credentialsElement);

        Element resolversElement = new Element("resolvers");

        Element chainElement = new Element("chain");
        chainElement.setAttribute("name", "main");

        String selectedRepoKey = getRepository().getRepoKey();
        String resolverName = getResolverName();
        resolverName = StringUtils.isNotBlank(resolverName) ? resolverName : "public";

        if (isM2Compatible()) {

            Element ibiblioElement = new Element("ibiblio");
            ibiblioElement.setAttribute("name", resolverName);
            ibiblioElement.setAttribute("m2compatible", Boolean.TRUE.toString());
            ibiblioElement.setAttribute("root", getFullRepositoryUrl(selectedRepoKey));
            chainElement.addContent(ibiblioElement);
        } else {

            Element urlElement = new Element("url");
            urlElement.setAttribute("name", resolverName);

            Element artifactPatternElement = new Element("artifact");
            artifactPatternElement.setAttribute("pattern", getFullArtifactPattern(selectedRepoKey));
            urlElement.addContent(artifactPatternElement);

            Element ivyPatternElement = new Element("ivy");
            ivyPatternElement.setAttribute("pattern", getFullIvyPattern(selectedRepoKey));
            urlElement.addContent(ivyPatternElement);

            chainElement.addContent(urlElement);
        }

        resolversElement.addContent(chainElement);

        rootNode.addContent(resolversElement);

        document.setRootElement(rootNode);

        return new XMLOutputter(Format.getPrettyFormat()).outputString(document);
    }

    public VirtualRepoEntry getRepository() {
        return repository;
    }

    public void setRepository(VirtualRepoEntry repository) {
        this.repository = repository;
    }

    public String getResolverName() {
        return resolverName;
    }

    public void setResolverName(String resolverName) {
        this.resolverName = resolverName;
    }

    @Override
    protected String getGenerateButtonTitle() {
        return "Generate Settings";
    }

    @Override
    protected String getSettingsWindowTitle() {
        return "Ivy Settings";
    }

    @Override
    protected Syntax getSettingsSyntax() {
        return Syntax.xml;
    }

    @Override
    protected String getSettingsMimeType() {
        return "application/xml";
    }

    @Override
    protected String getSaveToFileName() {
        return "ivysettings.xml";
    }
}