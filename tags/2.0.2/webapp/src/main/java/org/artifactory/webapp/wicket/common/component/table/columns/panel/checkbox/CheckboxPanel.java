package org.artifactory.webapp.wicket.common.component.table.columns.panel.checkbox;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public class CheckboxPanel extends Panel {
    public static final String CHECKBOX_ID = "checkbox";

    public CheckboxPanel(String id) {
        super(id);
    }

    public CheckboxPanel(String id, IModel model) {
        super(id, model);
    }
}
