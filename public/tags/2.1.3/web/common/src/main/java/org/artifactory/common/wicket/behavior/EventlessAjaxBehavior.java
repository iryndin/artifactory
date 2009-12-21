package org.artifactory.common.wicket.behavior;

import org.apache.wicket.ajax.AjaxEventBehavior;

/**
 * @author Yoav Aharoni
 */
public abstract class EventlessAjaxBehavior extends AjaxEventBehavior {
    public EventlessAjaxBehavior() {
        super("wicket:ajax");
    }

    public String getCallScript() {
        return String.format("(%s)();", getCallFunction());
    }

    public String getCallFunction() {
        return String.format("function() {%s}", getCallbackScript());
    }
}
