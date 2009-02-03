package org.artifactory.webapp.wicket.common.component.modal;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public class ModalCloseBehavior extends AttributeAppender {
    public ModalCloseBehavior() {
        super("onlick", true, new Model("try {Wicket.Window.current.close();} catch(e) {};"), ";");
    }
}
