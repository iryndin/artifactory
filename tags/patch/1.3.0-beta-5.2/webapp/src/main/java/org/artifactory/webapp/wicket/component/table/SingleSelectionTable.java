package org.artifactory.webapp.wicket.component.table;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.behavior.JavascriptEvent;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public abstract class SingleSelectionTable<T> extends AjaxFallbackDefaultDataTable {
    private T selection;

    public SingleSelectionTable(String id, List<IColumn> columns,
            ISortableDataProvider dataProvider,
            int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    public SingleSelectionTable(String id, IColumn[] columns, ISortableDataProvider dataProvider,
            int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    {
        add(new CssClass("selectable"));
        setOutputMarkupId(true);
        add(TextTemplateHeaderContributor.forJavaScript(SingleSelectionTable.class,
                "SingleSelectionTable.js", new Model()));
    }

    protected void onRowSelected(T selection, final AjaxRequestTarget target) {
        target.addComponent(this);
    }

    @Override
    protected Item newRowItem(String id, int index, IModel model) {
        Item rowItem = super.newRowItem(id, index, model);
        rowItem.add(new JavascriptEvent("onmouseover", "SingleSelectionTable.onmouseover(this);"));
        rowItem.add(new JavascriptEvent("onmouseout", "SingleSelectionTable.onmouseout(this);"));

        if (model.getObject().equals(selection)) {
            rowItem.add(new CssClass("selected"));
        }
        return rowItem;
    }

    @Override
    protected Item newCellItem(String id, int index, final IModel model) {
        Item cellItem = super.newCellItem(id, index, model);
        if (model.getObject() instanceof PropertyColumn) {
            cellItem.add(new AjaxEventBehavior("onclick") {
                @Override
                @SuppressWarnings({"unchecked"})
                protected void onEvent(final AjaxRequestTarget target) {
                    T rowObject = (T) getComponent().getParent().getParent().getModelObject();
                    selection = rowObject;

                    target.appendJavascript("SingleSelectionTable.onclick(this);");
                    onRowSelected(rowObject, target);
                }
            });
        }
        return cellItem;
    }

    public T getSelection() {
        return selection;
    }
}
