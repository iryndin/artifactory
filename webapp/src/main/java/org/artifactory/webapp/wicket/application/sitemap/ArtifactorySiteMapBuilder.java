package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.wicket.Page;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.admin.AdminPage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.page.config.general.GeneralConfigPage;
import org.artifactory.webapp.wicket.page.config.proxy.ProxyConfigPage;
import org.artifactory.webapp.wicket.page.config.repos.RepositoryConfigPage;
import org.artifactory.webapp.wicket.page.config.security.LdapKeyListPage;
import org.artifactory.webapp.wicket.page.config.services.BackupsListPage;
import org.artifactory.webapp.wicket.page.config.services.IndexerConfigPage;
import org.artifactory.webapp.wicket.page.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.importexport.repos.ImportExportReposPage;
import org.artifactory.webapp.wicket.page.importexport.system.ImportExportSystemPage;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.page.search.ArtifactSearchPage;
import org.artifactory.webapp.wicket.page.security.acl.AclsPage;
import org.artifactory.webapp.wicket.page.security.group.GroupsPage;
import org.artifactory.webapp.wicket.page.security.user.UsersPage;

/**
 * @author Yoav Aharoni
 */
public class ArtifactorySiteMapBuilder extends SiteMapBuilder {

    @SuppressWarnings({"OverlyCoupledMethod"})
    @Override
    public void buildSiteMap() {

        SiteMap siteMap = getSiteMap();
        MenuNode root = new MenuNode("Artifactory", Page.class);
        siteMap.setRoot(root);

        MenuNode homePage = new MenuNode("Home", HomePage.class);
        root.addChild(homePage);

        MenuNode browseRepoPage = new MenuNode("Browse", BrowseRepoPage.class);
        root.addChild(browseRepoPage);
        browseRepoPage.addChild(new MenuNode("Tree Browser", BrowseRepoPage.class));
        browseRepoPage.addChild(new MenuNode("Simple Browser", SimpleBrowserRootPage.class));
        browseRepoPage.addChild(new MenuNode("Search Artifactory", ArtifactSearchPage.class));

        DeployArtifactPageNode deployPage = new DeployArtifactPageNode("Deploy");
        root.addChild(deployPage);
        deployPage.addChild(new DeployArtifactPageNode("Deploy Artifacts"));

        MenuNode adminPage = new AdminPageNode("Admin");
        root.addChild(adminPage);

        MenuNode adminConfiguration = new MenuNode("Configuration");
        adminPage.addChild(adminConfiguration);
        adminConfiguration.addChild(new MenuNode("General", GeneralConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Repositories", RepositoryConfigPage.class));
        adminConfiguration.addChild(new MenuNode("Proxy", ProxyConfigPage.class));

        MenuNode services = new MenuNode("Services");
        adminPage.addChild(services);
        services.addChild(new MenuNode("Backups", BackupsListPage.class));
        services.addChild(new MenuNode("Indexer", IndexerConfigPage.class));

        MenuNode security = new MenuNode("Security");
        adminPage.addChild(security);
        security.addChild(new MenuNode("Users", UsersPage.class));
        security.addChild(new MenuNode("Groups", GroupsPage.class));
        security.addChild(new MenuNode("Permissions", AclsPage.class));
        security.addChild(new MenuNode("LDAP Settings", LdapKeyListPage.class));

        MenuNode adminImportExport = new MenuNode("Import & Export");
        adminPage.addChild(adminImportExport);
        adminImportExport.addChild(new MenuNode("Repositories", ImportExportReposPage.class));
        adminImportExport.addChild(new MenuNode("System", ImportExportSystemPage.class));

        MenuNode logs = new MenuNode("Logs");
        adminPage.addChild(logs);
        logs.addChild(new MenuNode("System Logs", SystemLogsPage.class));
    }

    private static class DeployArtifactPageNode extends SecuredPageNode {
        private DeployArtifactPageNode(String name) {
            super(DeployArtifactPage.class, name);
        }

        @Override
        public boolean isEnabled() {
            return getAuthorizationService().hasDeployPermissions();
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
            return authService.isAdmin() || authService.canAdminPermissionTarget();
        }
    }
}
