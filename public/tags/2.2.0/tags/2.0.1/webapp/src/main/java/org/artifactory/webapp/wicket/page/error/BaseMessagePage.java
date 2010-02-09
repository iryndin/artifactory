package org.artifactory.webapp.wicket.page.error;

import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.home.HomePage;

/**
 * @author Yoav Aharoni
 */
public class BaseMessagePage extends HomePage {

    @Override
    protected Class<? extends BasePage> getMenuPageClass() {
        return HomePage.class;
    }
}
