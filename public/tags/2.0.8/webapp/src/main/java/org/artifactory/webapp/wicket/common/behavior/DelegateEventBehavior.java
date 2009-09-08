package org.artifactory.webapp.wicket.common.behavior;

import org.apache.wicket.Component;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * @author Yoav Aharoni
 */
public class DelegateEventBehavior extends JavascriptEvent {
    public DelegateEventBehavior(final String event, final Component delegate) {
        super(event, new AbstractReadOnlyModel() {
            public Object getObject() {
                return "dojo.byId('" + delegate.getMarkupId() + "')." + event + "(event);";
            }
        });
        delegate.setOutputMarkupId(true);
    }
}
