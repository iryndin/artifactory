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

package org.artifactory.webapp.wicket.page.home.settings.gradle;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.home.settings.BaseIvySettingsGeneratorPanel;
import org.slf4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Enables the user to select a virtual repo that are configured in the system and if needed, to modify resolver name,
 * artifact patterns and ivy patterns to generate an artifactory gradle init script
 *
 * @author Noam Y. Tenne
 */
public class GradleInitScriptPanel extends BaseIvySettingsGeneratorPanel {

    private static final Logger log = LoggerFactory.getLogger(GradleInitScriptPanel.class);

    @WicketProperty
    private VirtualRepoEntry libsRepository;

    @WicketProperty
    private VirtualRepoEntry pluginsRepository;

    @WicketProperty
    private String libsResolverName;

    @WicketProperty
    private String pluginsResolverName;

    /**
     * Main constructor
     *
     * @param id                ID to assign to the panel
     * @param servletContextUrl Running context URL
     * @param virtualRepoKeyMap Virtual repo key association map
     */
    protected GradleInitScriptPanel(String id, String servletContextUrl,
            Map<String, String> virtualRepoKeyMap) {
        super(id, servletContextUrl, virtualRepoKeyMap);

        addRepoDropDown("libsRepository", this, "libs");
        addRepoDropDown("pluginsRepository", this, "plugins");

        addTextField("libsResolverName", this, "", true, false);
        addTextField("pluginsResolverName", this, "", true, false);
    }

    public String generateSettings(String servletContextUrl) {
        String libsRepoKey = getLibsRepository().getRepoKey();
        String pluginsRepoKey = getPluginsRepository().getRepoKey();

        InputStream gradleInitTemplateStream;

        Properties properties = new Properties();

        String pluginsResolverName = getPluginsResolverName();
        pluginsResolverName = StringUtils.isNotBlank(pluginsResolverName) ? pluginsResolverName : "plugins-repo";
        String libsResolverName = getLibsResolverName();
        libsResolverName = StringUtils.isNotBlank(libsResolverName) ? libsResolverName : "libs-repo";

        properties.setProperty("plugins.resolver.name", pluginsResolverName);
        properties.setProperty("plugins.repository.url", getFullRepositoryUrl(pluginsRepoKey));
        properties.setProperty("libs.resolver.name", libsResolverName);
        properties.setProperty("libs.repository.url", getFullRepositoryUrl(libsRepoKey));

        if (isM2Compatible()) {
            gradleInitTemplateStream = getClass().getResourceAsStream("/init.gradle.m2");
        } else {
            properties.setProperty("plugins.artifact.pattern", getFullArtifactPattern(pluginsRepoKey));
            properties.setProperty("plugins.ivy.pattern", getFullIvyPattern(pluginsRepoKey));
            properties.setProperty("libs.artifact.pattern", getFullArtifactPattern(libsRepoKey));
            properties.setProperty("libs.ivy.pattern", getFullIvyPattern(libsRepoKey));

            gradleInitTemplateStream = getClass().getResourceAsStream("/init.gradle.url");
        }

        try {
            String gradleTemplate = IOUtils.toString(gradleInitTemplateStream);
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            return propertyPlaceholderHelper.replacePlaceholders(gradleTemplate, properties);
        } catch (IOException e) {
            String errorMessage = "An error occurred while preparing the Gradle Init Script template: ";
            error(errorMessage + e.getMessage());
            log.error(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(gradleInitTemplateStream);
        }
        return "";
    }

    public VirtualRepoEntry getLibsRepository() {
        return libsRepository;
    }

    public void setLibsRepository(VirtualRepoEntry libsRepository) {
        this.libsRepository = libsRepository;
    }

    public VirtualRepoEntry getPluginsRepository() {
        return pluginsRepository;
    }

    public void setPluginsRepository(VirtualRepoEntry pluginsRepository) {
        this.pluginsRepository = pluginsRepository;
    }

    public String getLibsResolverName() {
        return libsResolverName;
    }

    public void setLibsResolverName(String libsResolverName) {
        this.libsResolverName = libsResolverName;
    }

    public String getPluginsResolverName() {
        return pluginsResolverName;
    }

    public void setPluginsResolverName(String pluginsResolverName) {
        this.pluginsResolverName = pluginsResolverName;
    }

    @Override
    protected String getGenerateButtonTitle() {
        return "Generate Init Script";
    }

    @Override
    protected String getSettingsWindowTitle() {
        return "Gradle Init Script";
    }

    @Override
    protected Syntax getSettingsSyntax() {
        return Syntax.groovy;
    }

    @Override
    protected String getSettingsMimeType() {
        return "application/x-groovy";
    }

    @Override
    protected String getSaveToFileName() {
        return "init.gradle";
    }
}