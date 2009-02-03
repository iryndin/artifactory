package org.artifactory.webapp.wicket.search;

import org.artifactory.webapp.wicket.ArtifactoryPage;

public class ArtifactLocatorPage extends ArtifactoryPage {

    /**
     * Constructor.
     */
    public ArtifactLocatorPage() {
        add(new ArtifactLocatorPanel("artifactLocatorPanel"));
    }

    protected String getPageName() {
        return "Artifact Locator";
    }


}
