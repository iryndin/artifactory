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

package org.artifactory.webapp.wicket.page.home;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenService;
import org.artifactory.api.maven.MavenSettings;
import org.artifactory.api.maven.MavenSettingsMirror;
import org.artifactory.api.maven.MavenSettingsRepository;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.panel.maven.settings.MavenSettingsGenerator;
import org.artifactory.common.wicket.panel.maven.settings.MavenSettingsPanel;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.HttpUtils;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Noam Tenne
 */
public class MavenSettingsPage extends AuthenticatedPage {

    private static final Logger log = LoggerFactory.getLogger(MavenSettingsPage.class);

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repositoryService;

    @SpringBean
    private MavenService mavenService;

    private MavenSettingsPanel mavenSettingsPanel;

    public MavenSettingsPage() {
        addMavenSettings();
    }

    //Adds the Maven Settings generator panel

    private void addMavenSettings() {

        //Add a markup container, in case we don't need to add the panel
        WebMarkupContainer markupContainer = new WebMarkupContainer("mavenSettingsPanel");
        add(markupContainer);

        List<VirtualRepoDescriptor> virtualRepoDescriptors = repositoryService.getVirtualRepoDescriptors();
        if (!virtualRepoDescriptors.isEmpty()) {
            //Get context URL
            ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
            HttpServletRequest request = servletWebRequest.getHttpServletRequest();
            final String servletContextUrl = HttpUtils.getServletContextUrl(request);

            //Build map of virtual repo keys + descriptions
            Map<String, String> map = new HashMap<String, String>();
            for (VirtualRepoDescriptor virtualRepoDescriptor : virtualRepoDescriptors) {
                map.put(virtualRepoDescriptor.getKey(), virtualRepoDescriptor.getDescription());
            }

            //Init maven settings panel
            mavenSettingsPanel = new MavenSettingsPanel("mavenSettingsPanel", map, new MavenSettingsGenerator() {
                //Local implementation

                /**
                 * {@inheritDoc}
                 */
                public String generateSettings() {
                    try {
                        //Build local MavenSettings object
                        MavenSettings mavenSettings = getSettings(servletContextUrl);

                        //Generate XML settings content
                        return mavenService.generateSettings(mavenSettings);
                    } catch (IOException ioe) {
                        String message = ioe.getMessage();
                        if (message == null) {
                            log.error("Maven settings generator error", ioe);
                            String logs;
                            if (authorizationService.isAdmin()) {
                                CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                                logs = "<a href=\"" + systemLogsPage + "\">log</a>";
                            } else {
                                logs = "log";
                            }
                            message = "Please review the " + logs + " for further information.";
                        }
                        error("An error has occurred during maven setting generation: " + message);
                        return "Maven settings could no be generated.";
                    }
                }
            });
            markupContainer.replaceWith(mavenSettingsPanel);
        }
    }

    /**
     * Builds a local MavenSettings object from the data input in the MavenSettingsPanel
     *
     * @param contextUrl System URL
     * @return MavenSettings - Inputted maven settings
     */
    private MavenSettings getSettings(String contextUrl) {
        //Make sure URL ends with slash
        if (!contextUrl.endsWith("/")) {
            contextUrl += "/";
        }

        //Build settings object from the user selections in the panel
        MavenSettings mavenSettings = new MavenSettings(contextUrl);

        //Add release and snapshot choices
        String releases = mavenSettingsPanel.getReleasesRepoKey();
        mavenSettings.addReleaseRepository(new MavenSettingsRepository("central", releases, false));
        String snapshots = mavenSettingsPanel.getSnapshotsRepoKey();
        mavenSettings.addReleaseRepository(new MavenSettingsRepository("snapshots", snapshots, true));

        //Add plugin release and snapshot choices
        String pluginReleases = mavenSettingsPanel.getPluginReleasesRepoKey();
        mavenSettings.addPluginRepository(new MavenSettingsRepository("central", pluginReleases, false));
        String pluginSnapshots = mavenSettingsPanel.getPluginSnapshotsRepoKey();
        mavenSettings.addPluginRepository(new MavenSettingsRepository("snapshots", pluginSnapshots, true));

        //Add the "mirror any" repo, if the user has selected it
        if (mavenSettingsPanel.isMirrorAny()) {
            String mirror = mavenSettingsPanel.getMirrorAnyKey();
            mavenSettings.addMirrorRepository(new MavenSettingsMirror(mirror, mirror, "*"));
        }

        return mavenSettings;
    }

    @Override
    public String getPageName() {
        return "Maven Settings";
    }
}