package org.artifactory.webapp.wicket.common.component.panel.shortcutkey;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;

/**
 * @author Yoav Aharoni
 */
public class KeyReleasedEvent implements Serializable {
    private AjaxRequestTarget target;
    private int keyCode;

    public KeyReleasedEvent(int keyCode, AjaxRequestTarget target) {
        this.target = target;
        this.keyCode = keyCode;
    }

    public AjaxRequestTarget getTarget() {
        return target;
    }

    public int getKeyCode() {
        return keyCode;
    }
}
