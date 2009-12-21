package org.artifactory.webapp.wicket.components.container;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.artifactory.common.wicket.component.image.ExternalImage;

/**
 * @author Tomer Cohen
 */
public class LogoDisplayContainer extends BookmarkablePageLink {

    public LogoDisplayContainer(String id, Class homePage, String logoPath) {
        super(id, homePage);
        setOutputMarkupId(true);
        add(new ExternalImage("logoImage", logoPath));
    }
}
