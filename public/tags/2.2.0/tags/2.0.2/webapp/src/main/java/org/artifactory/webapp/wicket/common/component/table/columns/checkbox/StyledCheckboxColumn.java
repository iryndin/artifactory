package org.artifactory.webapp.wicket.common.component.table.columns.checkbox;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;

/**
 * @author Yoav Aharoni
 */
public class StyledCheckboxColumn<T> extends CheckboxColumn<T> {
    public StyledCheckboxColumn(String title, String expression, String sortProperty) {
        super(title, expression, sortProperty);
    }

    @Override
    protected FormComponent newCheckBox(String id, IModel model, final T rowObject) {
        return new StyledCheckbox(id, model).setTitle("");
    }
}