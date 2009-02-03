package org.artifactory.webapp.wicket.common.component.table.columns.checkbox;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.PropertyResolver;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.common.component.table.SortableTable;
import org.artifactory.webapp.wicket.common.component.table.columns.panel.checkbox.CheckboxPanel;
import static org.artifactory.webapp.wicket.common.component.table.columns.panel.checkbox.CheckboxPanel.CHECKBOX_ID;

import java.util.Iterator;

/**
 * @author Yoav Aharoni
 */
public class SelectAllCheckboxColumn<T> extends AjaxCheckboxColumn<T> {
    private IModel selectAllModel = new Model(false);
    private StyledCheckbox selectAllCheckbox;

    public SelectAllCheckboxColumn(String title, String expression, String sortProperty) {
        super(title, expression, sortProperty);
    }

    @Override
    public Component getHeader(String componentId) {
        CheckboxPanel panel = new CheckboxPanel(componentId);
        selectAllCheckbox = new StyledCheckbox(CHECKBOX_ID, getSelectAllModel());
        selectAllCheckbox.setTitle(getDisplayModel().getObject().toString());
        selectAllCheckbox.add(new SimpleAttributeModifier("title", "Select All"));
        selectAllCheckbox.setOutputMarkupId(true);
        selectAllCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            protected void onUpdate(AjaxRequestTarget target) {
                SortableTable table = (SortableTable) selectAllCheckbox.findParent(DataTable.class);
                selectAll(table, target);
            }
        });
        panel.add(selectAllCheckbox);
        return panel;
    }

    @SuppressWarnings({"unchecked"})
    private void selectAll(SortableTable table, AjaxRequestTarget target) {
        ISortableDataProvider dataProvider = table.getDataProvider();
        Iterator<T> iterator = (Iterator<T>) dataProvider.iterator(0, dataProvider.size());
        while (iterator.hasNext()) {
            T rowObject = iterator.next();
            PropertyResolver.setValue(getExpression(), rowObject, isSelectAll(), null);
        }
        onSelectAll(iterator, target);
        target.addComponent(table);
    }

    protected void onSelectAll(Iterator<T> iterator, AjaxRequestTarget target) {
    }

    @Override
    protected void onUpdate(T rowObject, boolean value, AjaxRequestTarget target) {
        super.onUpdate(rowObject, value, target);
        setSelectAll(false);
        target.addComponent(selectAllCheckbox);
    }

    protected void notifyUpdateToChildren(Iterator<T> iterator, AjaxRequestTarget target) {
        while (iterator.hasNext()) {
            T rowObject = iterator.next();
            onUpdate(rowObject, isSelectAll(), target);
        }
    }

    public IModel getSelectAllModel() {
        return selectAllModel;
    }

    public void setSelectAllModel(IModel selectAllModel) {
        this.selectAllModel = selectAllModel;
    }

    public boolean isSelectAll() {
        return (Boolean) getSelectAllModel().getObject();
    }

    public void setSelectAll(boolean selectAll) {
        getSelectAllModel().setObject(selectAll);
    }
}
