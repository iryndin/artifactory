package org.artifactory.webapp.wicket.security;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.BasePage;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class LogoutPage extends BasePage {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(LogoutPage.class);

    //TODO: [by yl] Do real signout
    //Find out how to expire the session at the end og the request life cycle.
}
