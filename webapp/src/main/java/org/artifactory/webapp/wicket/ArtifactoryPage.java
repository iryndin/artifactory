package org.artifactory.webapp.wicket;

import org.acegisecurity.Authentication;
import org.artifactory.security.SecurityHelper;
import org.artifactory.webapp.wicket.browse.BrowseRepoPage;
import org.artifactory.webapp.wicket.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.importexport.ImportExportPage;
import org.artifactory.webapp.wicket.search.ArtifactLocatorPage;
import org.artifactory.webapp.wicket.security.acls.AclsPage;
import org.artifactory.webapp.wicket.security.login.LogoutPage;
import org.artifactory.webapp.wicket.security.users.UsersPage;
import org.artifactory.webapp.wicket.widget.SecuredPageLink;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.html.link.PageLink;
import wicket.model.PropertyModel;

@AuthorizeInstantiation("USER")
public abstract class ArtifactoryPage extends BasePage {

    public ArtifactoryPage() {
        //Add page links
        add(new PageLink("locatePage", ArtifactLocatorPage.class));
        add(new PageLink("browsePage", BrowseRepoPage.class));
        PageLink deployLink = new SecuredPageLink("deployPage", DeployArtifactPage.class);
        add(deployLink);
        PageLink importExportLink = new SecuredPageLink("importExportPage", ImportExportPage.class);
        add(importExportLink);
        PageLink usersLink = new SecuredPageLink("usersPage", UsersPage.class);
        add(usersLink);
        PageLink aclsLink = new SecuredPageLink("aclsPage", AclsPage.class);
        add(aclsLink);
        add(new BookmarkablePageLink("logoutPage", LogoutPage.class));
        add(new Label("pageTitle", new PropertyModel(this, "pageTitle")));
        add(new Label("username", new PropertyModel(this, "userName")));
    }

    protected abstract String getPageName();

    public String getPageTitle() {
        String serverName = getCc().getServerName();
        String pageName = getPageName();
        return "Artifactory@" + serverName + " :: " + pageName;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public String getUserName() {
        Authentication authentication = SecurityHelper.getAuthentication();
        String name = "";
        if (authentication != null) {
            name = authentication.getName();
        }
        return name;
    }
}

