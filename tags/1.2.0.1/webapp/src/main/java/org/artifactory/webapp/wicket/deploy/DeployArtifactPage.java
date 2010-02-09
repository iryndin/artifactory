package org.artifactory.webapp.wicket.deploy;

import org.artifactory.webapp.wicket.ArtifactoryPage;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;

@AuthorizeInstantiation("ADMIN")
public class DeployArtifactPage extends ArtifactoryPage {

    /**
     * Constructor.
     */
    public DeployArtifactPage() {
        add(new DeployArtifactPanel("deployArtifactPanel"));
    }

    protected String getPageName() {
        return "Artifact Deployer";
    }
}
