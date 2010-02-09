package org.artifactory.webapp.wicket.home;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactoryPage;

/**
 * Created by IntelliJ IDEA.
 * User: yoavl
 */
public class HomePage extends ArtifactoryPage {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(HomePage.class);


    public HomePage() {
        add(new WelcomePanel("welcomePanel"));
    }

    protected String getPageName() {
        return "Home";
    }
}
