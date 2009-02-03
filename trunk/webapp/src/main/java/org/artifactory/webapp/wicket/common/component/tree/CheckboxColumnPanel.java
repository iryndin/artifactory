package org.artifactory.webapp.wicket.common.component.tree;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public class CheckboxColumnPanel extends Panel {
    public CheckboxColumnPanel(String id) {
        super(id);
    }

    public CheckboxColumnPanel(String id, IModel model) {
        super(id, model);
    }
}
