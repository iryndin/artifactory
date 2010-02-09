package org.artifactory.webapp.wicket.browse;

import org.artifactory.webapp.wicket.ArtifactoryPage;

public class BrowseRepoPage extends ArtifactoryPage {

    public BrowseRepoPage() {
        add(new BrowseRepoPanel("browseRepoPanel"));
    }

    protected String getPageName() {
        return "Repository Browser";
    }
}
