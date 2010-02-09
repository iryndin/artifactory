package org.artifactory.webapp.wicket.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.behavior.CssClass;

/**
 * Requires the following fragment to exist in the markup container: <wicket:fragment
 * wicket:id="checkboxFrag"><input type="checkbox" wicket:id="checkbox"/></wicket:fragment>
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class CheckboxColumn<R> extends AbstractColumn {
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
                        R row = CheckboxColumn.this.getRow(model);
                        Boolean value = (Boolean) this.getModelObject();
                        doUpdate(row, value, target);
                    }
                };
        R row = getRow(model);
        boolean enabled = isEnabled(row);
        checkBox.setEnabled(enabled);
        Fragment fragment = new Fragment(componentId, "checkboxFrag", container, model);
        fragment.add(checkBox);
        cellItem.add(fragment);
        cellItem.add(new CssClass("CheckboxColumn"));
    }

    protected boolean isEnabled(R row) {
        return true;
    }

    /**
     * Called when the checkboc is updated (checked/unchecked).
     *
     * @param row     The affected row model.
     * @param checked True if the checkbox is checked.
     * @param target  The ajax target (the table container is added by default).
     */
    protected abstract void doUpdate(R row, boolean checked, AjaxRequestTarget target);

    @SuppressWarnings({"unchecked"})
    private R getRow(IModel model) {
        return (R) model.getObject();
    }
}
