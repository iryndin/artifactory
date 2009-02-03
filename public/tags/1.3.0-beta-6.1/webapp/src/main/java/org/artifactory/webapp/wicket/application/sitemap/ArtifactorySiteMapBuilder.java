package org.artifactory.webapp.wicket.application.sitemap;

import org.apache.wicket.Page;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.wicket.page.admin.AdminPage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.page.config.ConfigHomePage;
import org.artifactory.webapp.wicket.page.config.general.GeneralConfigPage;
import org.artifactory.webapp.wicket.page.config.proxy.ProxyConfigPage;
import org.artifactory.webapp.wicket.page.config.repos.RepositoryConfigPage;
import org.artifactory.webapp.wicket.page.config.security.LdapKeyListPage;
import org.artifactory.webapp.wicket.page.config.services.ServicesConfigPage;
import org.artifactory.webapp.wicket.page.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.page.home.HomePage;
import org.artifactory.webapp.wicket.page.importexport.ImportExportHomePage;
import org.artifactory.webapp.wicket.page.importexport.repos.ImportExportReposPage;
import org.artifactory.webapp.wicket.page.importexport.system.ImportExportSystemPage;
import org.artifactory.webapp.wicket.page.logs.SystemLogsHomePage;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.page.search.ArtifactSearchPage;
import org.artifactory.webapp.wicket.page.security.SecurityHomePage;
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
        PageNode root = new PageNode(Page.class, "Artifactory");
        siteMap.setRoot(root);

        PageNode homePage = new PageNode(HomePage.class, "Home");
        root.addChild(homePage);

        PageNode browseRepoPage = new PageNode(BrowseRepoPage.class, "Browse");
        root.addChild(browseRepoPage);
        browseRepoPage.addChild(new PageNode(BrowseRepoPage.class, "Tree Browser"));
        browseRepoPage.addChild(new PageNode(SimpleBrowserRootPage.class, "Simple Browser"));
        browseRepoPage.addChild(new PageNode(ArtifactSearchPage.class, "Search Artifactory"));

        DeployArtifactPageNode deployPage = new DeployArtifactPageNode("Deploy");
        root.addChild(deployPage);
        deployPage.addChild(new DeployArtifactPageNode("Deploy Artifacts"));

        PageNode adminPage = new AdminPageNode("Admin");
        root.addChild(adminPage);

        PageNode adminConfiguration = new PageNode(ConfigHomePage.class, "Configuration");
        adminPage.addChild(adminConfiguration);
        adminConfiguration.addChild(new PageNode(GeneralConfigPage.class, "General"));
        adminConfiguration.addChild(new PageNode(RepositoryConfigPage.class, "Repositories"));
        adminConfiguration.addChild(new PageNode(ProxyConfigPage.class, "Proxy"));
        adminConfiguration.addChild(new PageNode(ServicesConfigPage.class, "Services"));

        PageNode security = new PageNode(SecurityHomePage.class, "Security");
        adminPage.addChild(security);
        security.addChild(new PageNode(UsersPage.class, "Users"));
        security.addChild(new PageNode(GroupsPage.class, "Groups"));
        security.addChild(new PageNode(AclsPage.class, "Permissions"));
        security.addChild(new PageNode(LdapKeyListPage.class, "LDAP Settings"));

        PageNode adminImportExport = new PageNode(ImportExportHomePage.class, "Import & Export");
        adminPage.addChild(adminImportExport);
        adminImportExport.addChild(new PageNode(ImportExportReposPage.class, "Repositories"));
        adminImportExport.addChild(new PageNode(ImportExportSystemPage.class, "System"));

        PageNode logs = new PageNode(SystemLogsHomePage.class, "Logs");
        adminPage.addChild(logs);
        logs.addChild(new PageNode(SystemLogsPage.class, "System Logs"));
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
