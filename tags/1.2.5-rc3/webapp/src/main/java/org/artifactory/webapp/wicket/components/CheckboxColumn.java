package org.artifactory.webapp.wicket.components;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

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
                        Component tableContainer = CheckboxColumn.this.container.getPage()
                                .get("targetsManagementPanel:panel:recipients:recipientsContainer");
                        target.addComponent(tableContainer);
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
