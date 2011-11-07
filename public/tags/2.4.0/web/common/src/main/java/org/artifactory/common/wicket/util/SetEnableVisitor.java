package org.artifactory.common.wicket.util;

import org.apache.wicket.Component;

/**
 * @author Yoav Aharoni
 */
public class SetEnableVisitor<T extends Component> implements Component.IVisitor<T> {
    private boolean enabled;

    public SetEnableVisitor(boolean enabled) {
        this.enabled = enabled;
    }

    public Object component(T component) {
        component.setEnabled(enabled);
        return CONTINUE_TRAVERSAL;
    }
}
