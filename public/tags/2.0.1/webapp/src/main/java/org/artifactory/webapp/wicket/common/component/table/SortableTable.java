package org.artifactory.webapp.wicket.common.component.table;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.behavior.JavascriptEvent;
import org.artifactory.webapp.wicket.common.component.navigation.NavigationToolbarWithDropDown;
import org.artifactory.webapp.wicket.common.component.table.columns.AttachColumnListener;

import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class SortableTable extends DataTable {
    private ISortableDataProvider dataProvider;

    public SortableTable(String id, final List<IColumn> columns, ISortableDataProvider dataProvider, int rowsPerPage) {
        this(id, columns.toArray(new IColumn[columns.size()]), dataProvider, rowsPerPage);
    }

    public SortableTable(String id, final IColumn[] columns, ISortableDataProvider dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
        this.dataProvider = dataProvider;
        setOutputMarkupId(true);
        setVersioned(false);
        add(new CssClass("data-table"));

        addTopToolbar(new NavigationToolbarWithDropDown(this));

        // add header
        AjaxFallbackHeadersToolbar headersToolbar = new AjaxFallbackHeadersToolbar(this, dataProvider);
        headersToolbar.visitChildren(new AddCssVisitor());
        addTopToolbar(headersToolbar);

        // add bottom toolbars
        addBottomToolbar(new NoRecordsToolbar(this));

        notifyColumnsAttached();
    }

    public final ISortableDataProvider getDataProvider() {
        return dataProvider;
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    protected Item newRowItem(String id, int index, IModel model) {
        OddEvenItem rowItem = new OddEvenItem(id, index, model);
        rowItem.add(new JavascriptEvent("onmouseover", "DomUtils.addHoverStyle(this);"));
        rowItem.add(new JavascriptEvent("onmouseout", "DomUtils.removeHoverStyle(this);"));
        return rowItem;
    }

    @Override
    protected Item newCellItem(String id, int index, IModel model) {
        Item item = super.newCellItem(id, index, model);
        if (index == 0) {
            item.add(new CssClass("first-cell"));
        } else if (index == getColumns().length - 1) {
            item.add(new CssClass("last-cell"));
        }
        return item;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.put("cellpadding", "0");
        tag.put("cellspacing", "0");
    }

    private void notifyColumnsAttached() {
        for (IColumn column : getColumns()) {
            if (column instanceof AttachColumnListener) {
                ((AttachColumnListener) column).onColumnAttached(this);
            }
        }
    }

    private class AddCssVisitor implements IVisitor {
        private int index;

        public Object component(Component component) {
            if ("header".equals(component.getId())) {
                if (index == 0) {
                    component.add(new CssClass("first-cell"));
                } else if (index == getColumns().length - 1) {
                    component.add(new CssClass("last-cell"));
                }
                index++;
                return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
            return CONTINUE_TRAVERSAL;
        }
    }
}
