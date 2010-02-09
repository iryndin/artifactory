package org.artifactory.webapp.wicket.common.component.table.columns.checkbox;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

/**
 * @author Yoav Aharoni
 */
public class AjaxCheckboxColumn<T> extends StyledCheckboxColumn<T> {
    public AjaxCheckboxColumn(String title, String expression, String sortProperty) {
        super(title, expression, sortProperty);
    }

    @Override
    protected FormComponent newCheckBox(String id, IModel model, final T rowObject) {
        final FormComponent checkbox = super.newCheckBox(id, model, rowObject);

        checkbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            protected void onUpdate(AjaxRequestTarget target) {
                Boolean checked = (Boolean) checkbox.getModelObject();
                AjaxCheckboxColumn.this.onUpdate(rowObject, checked, target);
            }
        });
        return checkbox;
    }

    /**
     * Called when the checkbox is updated (checked/unchecked).
     *
     * @param rowObject The affected row model.
     * @param value     True if the checkbox is checked.
     * @param target    The ajax target (the table container is added by default).
     */
    protected void onUpdate(T rowObject, boolean value, AjaxRequestTarget target) {
    }
}
