package org.artifactory.webapp.wicket.common.component;

import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * @author Yoav Aharoni
 */
public class PlaceHolder extends WebMarkupContainer {
    public PlaceHolder(String id) {
        super(id);
        setVisible(false);
    }
}
