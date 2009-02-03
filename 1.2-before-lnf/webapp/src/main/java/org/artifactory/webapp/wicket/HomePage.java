package org.artifactory.webapp.wicket;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: yoavl
 */
public class HomePage extends ArtifactoryPage {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(HomePage.class);

    /*public HomePage() {
        add(new PageLink("locate", ArtifactLocatorPage.class));
        add(new PageLink("deploy", DeployArtifactPage.class));
    }*/

    protected String getPageName() {
        return "Home";
    }
}
