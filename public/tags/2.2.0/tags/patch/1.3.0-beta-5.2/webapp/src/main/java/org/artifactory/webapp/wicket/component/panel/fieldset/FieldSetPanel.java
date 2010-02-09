package org.artifactory.webapp.wicket.component.panel.fieldset;

import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;

/**
 * @author Yoav Aharoni
 */
public class FieldSetPanel extends TitledPanel {
    public FieldSetPanel(String id) {
        this(id, null);
    }

    public FieldSetPanel(String id, IModel model) {
        super(id, model);
        add(new CssClass("fieldset"));
    }
}
