package org.artifactory.webapp.wicket;

import org.artifactory.repo.LocalRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.webapp.wicket.browse.BrowseRepoPage;
import org.artifactory.webapp.wicket.deploy.DeployArtifactPage;
import org.artifactory.webapp.wicket.importexport.ImportExportPage;
import org.artifactory.webapp.wicket.search.ArtifactLocatorPage;
import org.artifactory.webapp.wicket.security.LogoutPage;
import org.artifactory.webapp.wicket.widget.SecuredPageLink;
import wicket.Component;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.BookmarkablePageLink;
import wicket.markup.html.link.PageLink;
import wicket.model.Model;

@AuthorizeInstantiation("guest")
public abstract class ArtifactoryPage extends BasePage {

    public ArtifactoryPage() {
        //Add page links
        add(new PageLink("locatePage", ArtifactLocatorPage.class));
        add(new PageLink("browsePage", BrowseRepoPage.class));
        PageLink deployLink = new SecuredPageLink("deployPage", DeployArtifactPage.class);
        MetaDataRoleAuthorizationStrategy.authorize(deployLink, ENABLE, "admin");
        add(deployLink);
        PageLink importExportLink = new SecuredPageLink("importExportPage", ImportExportPage.class);
        MetaDataRoleAuthorizationStrategy.authorize(importExportLink, ENABLE, "admin");
        add(importExportLink);
        add(new BookmarkablePageLink("logoutPage", LogoutPage.class));
        add(new Label("pageTitle", new Model() {
            @Override
            public Object getObject(Component component) {
                String serverName = getCc().getServerName();
                String pageName = getPageName();
                return "Artifactory@" + serverName + " :: " + pageName;
            }
        }).setRenderBodyOnly(true));
    }

    protected String getArtifactMetadataContent(ArtifactResource pa) {
        String repositoryKey = pa.getRepoKey();
        LocalRepo repo = getCc().localOrCachedRepositoryByKey(repositoryKey);
        String pom = repo.getPomContent(pa);
        if (pom == null) {
            pom = "No POM file found for '" + pa.getName() + "'.";
        }
        String artifactMetadata = pa.getActualArtifactXml();
        StringBuilder result = new StringBuilder();
        if (artifactMetadata != null && artifactMetadata.trim().length() > 0) {
            result.append("------ ARTIFACT EFFECTIVE METADATA BEGIN ------\n")
                    .append(artifactMetadata)
                    .append("------- ARTIFACT EFFECTIVE METADATA END -------\n\n");
        }
        result.append(pom);
        return result.toString();
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    protected final ArtifactorySession getArtifactorySession() {
        ArtifactorySession as = (ArtifactorySession) getSession();
        return as;
    }

    protected abstract String getPageName();
}

