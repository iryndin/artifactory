package org.artifactory.common.wicket.util;

import org.apache.wicket.Component;

/**
 * @author Yoav Aharoni
 */
public class SetEnableVisitor implements Component.IVisitor {
    private boolean enabled;

    public SetEnableVisitor(boolean enabled) {
        this.enabled = enabled;
    }

    public Object component(Component component) {
        component.setEnabled(enabled);
        return CONTINUE_TRAVERSAL;
    }
}
