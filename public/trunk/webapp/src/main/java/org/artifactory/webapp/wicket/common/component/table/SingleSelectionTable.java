package org.artifactory.webapp.wicket.common.component.table;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public abstract class SingleSelectionTable<T> extends SortableTable {
    private T selection;

    protected SingleSelectionTable(String id, List<IColumn> columns,
                                   ISortableDataProvider dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    protected SingleSelectionTable(String id, IColumn[] columns, ISortableDataProvider dataProvider,
                                   int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    {
        add(new CssClass("selectable"));
        setOutputMarkupId(true);
    }

    protected void onRowSelected(T selection, final AjaxRequestTarget target) {
        target.addComponent(this);
    }

    @Override
    protected Item newRowItem(String id, int index, IModel model) {
        Item rowItem = super.newRowItem(id, index, model);
        if (model.getObject().equals(selection)) {
            rowItem.add(new CssClass("selected"));
        }
        return rowItem;
    }

    @Override
    protected Item newCellItem(String id, int index, final IModel model) {
        Item cellItem = super.newCellItem(id, index, model);
        if (model.getObject() instanceof PropertyColumn) {
            cellItem.add(new SelectRowBehavior());
        }
        return cellItem;
    }

    public T getSelection() {
        return selection;
    }

    private class SelectRowBehavior extends AjaxEventBehavior {
        private SelectRowBehavior() {
            super("onclick");
        }

        @Override
        @SuppressWarnings({"unchecked"})
        protected void onEvent(final AjaxRequestTarget target) {
            T rowObject = (T) getComponent().getParent().getParent().getModelObject();
            selection = rowObject;

            onRowSelected(rowObject, target);
        }
    }
}
