package org.artifactory.webapp.wicket.common.behavior;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public class JavascriptEvent extends AttributeAppender {
    public JavascriptEvent(String event, String javascript) {
        this(event, new Model(javascript));
    }

    public JavascriptEvent(String event, IModel javascriptModel) {
        super(event, true, javascriptModel, ";");
    }
}
