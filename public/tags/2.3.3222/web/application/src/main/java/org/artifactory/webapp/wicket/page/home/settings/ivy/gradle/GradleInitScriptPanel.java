/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.home.settings.ivy.gradle;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.wicket.FilteredResourcesWebAddon;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.home.settings.ivy.base.BaseIvySettingsGeneratorPanel;
import org.artifactory.webapp.wicket.page.home.settings.ivy.base.IvySettingsRepoSelectorPanel;
import org.slf4j.Logger;
import org.springframework.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

/**
 * Enables the user to select a virtual repo that are configured in the system and if needed, to modify resolver name,
 * artifact patterns and ivy patterns to generate an artifactory gradle init script
 *
 * @author Noam Y. Tenne
 */
public class GradleInitScriptPanel extends BaseIvySettingsGeneratorPanel {

    private static final Logger log = LoggerFactory.getLogger(GradleInitScriptPanel.class);
    private IvySettingsRepoSelectorPanel libsPanel;
    private IvySettingsRepoSelectorPanel pluginsPanel;
    private final String servletContextUrl;

    /**
     * Main constructor
     *
     * @param id                     ID to assign to the panel
     * @param servletContextUrl      Running context URL
     * @param virtualRepoDescriptors List of virtual repository descriptors
     */
    protected GradleInitScriptPanel(String id, String servletContextUrl,
            List<VirtualRepoDescriptor> virtualRepoDescriptors) {
        super(id, servletContextUrl);
        this.servletContextUrl = servletContextUrl;

        libsPanel = new IvySettingsRepoSelectorPanel("libsPanel", virtualRepoDescriptors, servletContextUrl,
                IvySettingsRepoSelectorPanel.RepoType.LIBS);
        form.add(libsPanel);

        pluginsPanel = new IvySettingsRepoSelectorPanel("pluginsPanel", virtualRepoDescriptors, servletContextUrl,
                IvySettingsRepoSelectorPanel.RepoType.PLUGINS);
        form.add(pluginsPanel);
    }

    public String generateSettings() {
        Properties templateProperties = getTemplateProperties();

        return replaceValuesAndGetTemplateValue("/init.gradle.template", templateProperties);
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

    private Properties getTemplateProperties() {
        Properties repoDetailsProperties = new Properties();

        String pluginsResolverName = libsPanel.getResolverName();
        pluginsResolverName = StringUtils.isNotBlank(pluginsResolverName) ? pluginsResolverName : "plugins-repo";
        String libsResolverName = pluginsPanel.getResolverName();
        libsResolverName = StringUtils.isNotBlank(libsResolverName) ? libsResolverName : "libs-repo";

        repoDetailsProperties.setProperty("plugins.resolver.name", pluginsResolverName);
        repoDetailsProperties.setProperty("plugins.resolver.m2Compatible",
                Boolean.toString(pluginsPanel.isM2Compatible()));
        repoDetailsProperties.setProperty("plugins.repository.url", pluginsPanel.getFullRepositoryUrl());

        repoDetailsProperties.setProperty("libs.resolver.name", libsResolverName);
        repoDetailsProperties.setProperty("libs.resolver.m2Compatible", Boolean.toString(libsPanel.isM2Compatible()));
        repoDetailsProperties.setProperty("libs.repository.url", libsPanel.getFullRepositoryUrl());

        boolean pluginsUsesIbiblio = pluginsPanel.useIbiblioResolver();

        if (!pluginsUsesIbiblio) {
            repoDetailsProperties.setProperty("plugins.artifact.pattern", pluginsPanel.getFullArtifactPattern());
            repoDetailsProperties.setProperty("plugins.ivy.pattern", pluginsPanel.getFullDescriptorPattern());
        }

        boolean libsUsesIbiblio = libsPanel.useIbiblioResolver();

        if (!libsUsesIbiblio) {
            repoDetailsProperties.setProperty("libs.artifact.pattern", libsPanel.getFullArtifactPattern());
            repoDetailsProperties.setProperty("libs.ivy.pattern", libsPanel.getFullDescriptorPattern());
        }


        Properties credentialProperties = new Properties();

        if (!authorizationService.isAnonymous() || !authorizationService.isAnonAccessEnabled()) {
            credentialProperties.setProperty("plugins.creds.line.break", "\n          ");
            credentialProperties.setProperty("libs.creds.line.break", "\n        ");

            credentialProperties.setProperty("auth.host", getHost());

            credentialProperties.setProperty("auth.host", getHost());

            FilteredResourcesWebAddon filteredResourcesWebAddon = addonsManager.addonByType(
                    FilteredResourcesWebAddon.class);

            credentialProperties.setProperty("auth.username",
                    filteredResourcesWebAddon.getGeneratedSettingsUsernameTemplate());

            credentialProperties.setProperty("auth.password",
                    filteredResourcesWebAddon.getGeneratedSettingsUserCredentialsTemplate(false));
        }

        Properties repoTemplateValues = new Properties();

        repoTemplateValues.setProperty("initscript.repo",
                getTemplateValue(pluginsUsesIbiblio, "initscript", repoDetailsProperties));
        repoTemplateValues.setProperty("plugins.repo",
                getTemplateValue(pluginsUsesIbiblio, "plugins", repoDetailsProperties));
        repoTemplateValues.setProperty("libs.repo",
                getTemplateValue(libsUsesIbiblio, "libs", repoDetailsProperties));

        repoTemplateValues.setProperty("plugins.creds", credentialProperties.isEmpty() ? "" :
                replaceValuesAndGetTemplateValue("/init.gradle.plugins.creds.template", credentialProperties));
        repoTemplateValues.setProperty("libs.creds", credentialProperties.isEmpty() ? "" :
                replaceValuesAndGetTemplateValue("/init.gradle.libs.creds.template", credentialProperties));

        return repoTemplateValues;
    }

    private String getTemplateValue(boolean useIbiblio, String templatePrefix, Properties repoDetailsProperties) {
        StringBuilder templatePath = new StringBuilder("/init.gradle.");
        if (useIbiblio) {
            templatePath.append("ibiblio");
        } else {
            templatePath.append("url");
        }

        templatePath.append(".").append(templatePrefix).append(".repo.template");

        return replaceValuesAndGetTemplateValue(templatePath.toString(), repoDetailsProperties);
    }

    private String replaceValuesAndGetTemplateValue(String templateResourcePath, Properties templateProperties) {
        InputStream gradleInitTemplateStream = null;
        try {
            gradleInitTemplateStream = getClass().getResourceAsStream(templateResourcePath);
            String gradleTemplate = IOUtils.toString(gradleInitTemplateStream);
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            return propertyPlaceholderHelper.replacePlaceholders(gradleTemplate, templateProperties);
        } catch (IOException e) {
            String errorMessage = "An error occurred while preparing the Gradle Init Script template: ";
            error(errorMessage + e.getMessage());
            log.error(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(gradleInitTemplateStream);
        }
        return "";
    }

    private String getHost() {
        try {
            return new URI(servletContextUrl).getHost();
        } catch (URISyntaxException e) {
            log.warn("Unable to resolve host for generated gradle init script: " + e.getMessage());
        }

        return "unknown";
    }
}