package org.artifactory.webapp.wicket.behavior;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public class CssClass extends AttributeAppender {
    public CssClass(IModel cssModel) {
        super("class", true, cssModel, " ");
    }

    public CssClass(String cssClass) {
        this(new Model(cssClass));
    }
}
