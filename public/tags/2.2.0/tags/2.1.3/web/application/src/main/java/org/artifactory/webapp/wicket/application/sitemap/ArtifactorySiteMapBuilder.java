/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Page;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.*;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.common.wicket.model.sitemap.MenuNodeVisitor;
import org.artifactory.common.wicket.model.sitemap.SiteMap;
import org.artifactory.common.wicket.model.sitemap.SiteMapBuilder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.page.admin.AdminPage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.page.build.BuildBrowserRootPage;
import org.artifactory.webapp.wicket.page.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.page.deploy.fromzip.DeployFromZipPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.home.MavenSettingsPage;
import org.artifactory.webapp.wicket.page.search.archive.ArchiveSearchPage;
import org.artifactory.webapp.wicket.page.search.artifact.ArtifactSearchPage;
import org.artifactory.webapp.wicket.page.search.gavc.GavcSearchPage;
import org.artifactory.webapp.wicket.page.search.metadata.MetadataSearchPage;

import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ArtifactorySiteMapBuilder extends SiteMapBuilder {

    @SpringBean
    private AddonsManager addons;

    @Override
    public void buildSiteMap() {
        WebApplicationAddon applicationAddon = addons.addonByType(WebApplicationAddon.class);

        SiteMap siteMap = getSiteMap();
        MenuNode root = new MenuNode("Artifactory", Page.class);
        siteMap.setRoot(root);

        MenuNode homePage = applicationAddon.getHomeButton("Home");
        root.addChild(homePage);

        MenuNode homeGroup = new OpenedMenuNode("Home");
        homePage.addChild(homeGroup);

        MenuNode welcomePage = new MenuNode("Welcome", HomePage.class);
        homeGroup.addChild(welcomePage);
        homeGroup.addChild(new MenuNode("Maven Settings", MavenSettingsPage.class));

        MenuNode browseRepoPage = new MenuNode("Artifacts", BrowseRepoPage.class);
        root.addChild(browseRepoPage);

        MenuNode browseGroup = new OpenedMenuNode("Browse");
        browseRepoPage.addChild(browseGroup);

        browseGroup.addChild(new MenuNode("Tree Browser", BrowseRepoPage.class));
        browseGroup.addChild(new MenuNode("Simple Browser", SimpleBrowserRootPage.class));
        SearchAddon searchAddon = addons.addonByType(SearchAddon.class);
        browseGroup.addChild(searchAddon.getBrowserSearchMenuNode());
        browseGroup.addChild(new MenuNode("Builds", BuildBrowserRootPage.class));

        MenuNode searchGroup = new OpenedMenuNode("Search");
        browseRepoPage.addChild(searchGroup);
        searchGroup.addChild(new MenuNode("Quick Search", ArtifactSearchPage.class));
        searchGroup.addChild(new MenuNode("Class Search", ArchiveSearchPage.class));
        searchGroup.addChild(new MenuNode("GAVC Search", GavcSearchPage.class));
        PropertiesAddon propertiesAddon = addons.addonByType(PropertiesAddon.class);
        searchGroup.addChild(propertiesAddon.getPropertySearchMenuNode("Property Search"));
        searchGroup.addChild(new MenuNode("POM/XML Search", MetadataSearchPage.class));

        DeployArtifactPageNode deployPage = new DeployArtifactPageNode(DeployArtifactPage.class, "Deploy");
        root.addChild(deployPage);
        MenuNode deployGroup = new OpenedMenuNode("Deploy");
        deployPage.addChild(deployGroup);
        deployGroup.addChild(new DeployArtifactPageNode(DeployArtifactPage.class, "Single Artifact"));
        deployGroup.addChild(new DeployArtifactPageNode(DeployFromZipPage.class, "Artifacts Bundle"));

        MenuNode adminPage = new AdminPageNode("Admin");
        root.addChild(adminPage);

        MenuNode adminConfiguration = applicationAddon.getConfigurationMenuNode(propertiesAddon);
        adminPage.addChild(adminConfiguration);

        WebstartWebAddon webstartAddon = addons.addonByType(WebstartWebAddon.class);
        HttpSsoAddon httpSsoAddon = addons.addonByType(HttpSsoAddon.class);
        MenuNode securityConfiguration = applicationAddon.getSecurityMenuNode(webstartAddon, httpSsoAddon);
        adminPage.addChild(securityConfiguration);

        MenuNode adminServices = applicationAddon.getServicesMenuNode();
        adminPage.addChild(adminServices);

        MenuNode adminImportExport = applicationAddon.getImportExportMenuNode();
        adminPage.addChild(adminImportExport);

        MenuNode adminAdvanced = applicationAddon.getAdvancedMenuNode();
        adminPage.addChild(adminAdvanced);

        siteMap.visitPageNodes(new RemoveUnauthorizedNodesVisitor());
    }

    private static class DeployArtifactPageNode extends SecuredPageNode {
        private DeployArtifactPageNode(Class<? extends Page> pageClass, String name) {
            super(pageClass, name);
        }

        @Override
        public boolean isEnabled() {
            if (!getAuthorizationService().hasPermission(ArtifactoryPermission.DEPLOY)) {
                return false;
            }

            List<LocalRepoDescriptor> repoDescriptorList = getRepositoryService().getDeployableRepoDescriptors();
            return CollectionUtils.isNotEmpty(repoDescriptorList);
        }
    }

    private static class AdminPageNode extends SecuredPageNode {
        private AdminPageNode(String name) {
            super(AdminPage.class, name);
        }

        @Override
        public boolean isEnabled() {
            // allow only admins or users with admin permissions on a permission target
            AuthorizationService authService = getAuthorizationService();
            return authService.isAdmin() || authService.hasPermission(ArtifactoryPermission.ADMIN);
        }
    }

    private static class OpenedMenuNode extends MenuNode {
        public OpenedMenuNode(String name) {
            super(name);
        }

        @Override
        public Boolean isOpened() {
            return true;
        }
    }

    private class RemoveUnauthorizedNodesVisitor implements MenuNodeVisitor {
        public void visit(MenuNode node, Iterator<MenuNode> iterator) {
            // check if page instantiation is allowed
            Class<? extends Page> pageClass = node.getPageClass();
            if (pageClass != null) {
                WebApplicationAddon applicationAddon = addons.addonByType(WebApplicationAddon.class);
                boolean instantiationAuthorized = applicationAddon.isInstantiationAuthorized(pageClass);
                if (!instantiationAuthorized) {
                    iterator.remove();
                }
            }
        }
    }
}