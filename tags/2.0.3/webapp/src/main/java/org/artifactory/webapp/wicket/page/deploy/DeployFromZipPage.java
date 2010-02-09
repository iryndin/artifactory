package org.artifactory.webapp.wicket.page.deploy;

import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * The page to display the Deploy Artifacts From Zip panel
 *
 * @author Noam Tenne
 */
public class DeployFromZipPage extends AuthenticatedPage {
    public DeployFromZipPage() {
        add(new DeployFromZipPanel("deployFromZipPanel"));
    }

    protected String getPageName() {
        return "Zip Deployer";
    }
}
