package org.artifactory.webapp.wicket.common.component.table.columns.checkbox;

import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.table.columns.panel.checkbox.CheckboxPanel;
import static org.artifactory.webapp.wicket.common.component.table.columns.panel.checkbox.CheckboxPanel.CHECKBOX_ID;

/**
 * @author Yoav Aharoni
 */
public class CheckboxColumn<T> extends AbstractColumn {
    private String expression;

    public CheckboxColumn(String title, String expression, String sortProperty) {
        super(new Model(title), sortProperty);
        this.expression = expression;
    }

    public void populateItem(final Item cellItem, String componentId, final IModel rowModel) {
        T rowObject = getRowModelObject(rowModel);

        CheckboxPanel panel = new CheckboxPanel(componentId, rowModel);
        cellItem.add(new CssClass("CheckboxColumn"));
        cellItem.add(panel);

        IModel model = newPropertyModel(rowObject);
        FormComponent checkBox = newCheckBox(CHECKBOX_ID, model, rowObject);
        panel.add(checkBox);

        boolean enabled = isEnabled(rowObject);
        checkBox.setEnabled(enabled);
    }

    protected FormComponent newCheckBox(String id, IModel model, T rowObject) {
        return new CheckBox(id, model);
    }

    protected IModel newPropertyModel(T rowObject) {
        return new PropertyModel(rowObject, expression);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected boolean isEnabled(T rowObject) {
        return true;
    }

    @SuppressWarnings({"unchecked"})
    protected final T getRowModelObject(IModel model) {
        return (T) model.getObject();
    }

    public final String getExpression() {
        return expression;
    }
}