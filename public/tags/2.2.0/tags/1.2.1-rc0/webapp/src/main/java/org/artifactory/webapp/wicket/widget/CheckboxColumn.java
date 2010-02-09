package org.artifactory.webapp.wicket.widget;

import org.apache.log4j.Logger;
import wicket.ajax.AjaxRequestTarget;
import wicket.ajax.markup.html.form.AjaxCheckBox;
import wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.panel.Fragment;
import wicket.markup.repeater.Item;
import wicket.model.IModel;
import wicket.model.Model;
import wicket.model.PropertyModel;

/**
 * Requires the following fragment to exist in the markup container: <wicket:fragment
 * wicket:id="checkboxFrag"><input type="checkbox" wicket:id="checkbox"/></wicket:fragment>
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class CheckboxColumn<T> extends AbstractColumn {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(CheckboxColumn.class);

    private String expression;
    private WebMarkupContainer container;

    public CheckboxColumn(String title, String expression, WebMarkupContainer container) {
        this(title, expression, null, container);
    }

    public CheckboxColumn(
            String title, String expression, String sortProperty, WebMarkupContainer container) {
        super(new Model(title), sortProperty);
        this.expression = expression;
        this.container = container;
    }

    public void populateItem(final Item cellItem, String componentId, final IModel model) {
        AjaxCheckBox checkBox =
                new AjaxCheckBox("checkbox", new PropertyModel(model, expression)) {
                    protected void onUpdate(AjaxRequestTarget target) {
                        T row = CheckboxColumn.this.getModelObject(model);
                        Boolean value = (Boolean) this.getModelObject();
                        doUpdate(row, value);
                    }
                };
        T row = getModelObject(model);
        boolean enabled = isEnabled(row);
        checkBox.setEnabled(enabled);
        Fragment fragment = new Fragment(componentId, "checkboxFrag", container, model);
        fragment.add(checkBox);
        cellItem.add(fragment);
    }

    @SuppressWarnings({"unchecked"})
    private T getModelObject(IModel model) {
        return (T) model.getObject();
    }

    protected boolean isEnabled(T row) {
        return true;
    }

    protected abstract void doUpdate(T rowModelObject, boolean value);
}
