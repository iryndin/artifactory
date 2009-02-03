package org.artifactory.webapp.wicket.common.component.border.fieldset;

import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitleLabel;

/**
 * @author Yoav Aharoni
 */
public class FieldSetBorder extends Border implements Titled {
    public FieldSetBorder(String id) {
        this(id, null);
    }

    public FieldSetBorder(String id, IModel model) {
        super(id, model);
        add(new CssClass("fieldset"));
        add(new TitleLabel(this));
    }

    public String getTitle() {
        return getString(getId(), null, "");
    }
}
