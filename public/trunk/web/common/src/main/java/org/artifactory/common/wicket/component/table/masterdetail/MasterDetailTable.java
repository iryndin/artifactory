package org.artifactory.common.wicket.component.table.masterdetail;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.toolbar.emptyrow.EmptyRowToolbar;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;

import java.io.Serializable;
import java.util.*;

/**
 * @author Yoav Aharoni
 */
public abstract class MasterDetailTable<M extends Serializable, D extends Serializable> extends SortableTable {
    private Set<M> openedItems = new HashSet<M>();

    public MasterDetailTable(String id, List<IColumn> columns, List<M> masterList, int rowsPerPage) {
        this(id, columns.toArray(new IColumn[columns.size()]), masterList, rowsPerPage);
    }

    public MasterDetailTable(String id, IColumn[] columns, List<M> masterList, int rowsPerPage) {
        super(id, addSpaceColumns(columns), new MasterDataProvider(masterList), rowsPerPage);
    }

    {
        add(new CssClass("groupable-table grouped"));
        setItemReuseStrategy(new MasterDetailItemsStrategy(this));
        addBottomToolbar(new EmptyRowToolbar(this));
    }

    protected boolean isMasterExpanded(M masterObject) {
        return openedItems.contains(masterObject);
    }

    @SuppressWarnings({"unchecked"})
    protected Item newMasterRow(IModel rowModel, int index) {
        // add row item
        Item rowItem = new Item("master" + index, index, null);
        rowItem.setRenderBodyOnly(true);

        // add cell item
        Item cellItem = new Item("cells", 0, null);
        cellItem.setRenderBodyOnly(true);
        rowItem.add(cellItem);

        // add master panel
        final M masterObject = ((MasterDetailEntry<M, D>) rowModel.getObject()).getMaster();
        cellItem.add(new MasterDetailRowPanel<M, D>("cell", masterObject, this));
        return rowItem;
    }

    @SuppressWarnings({"unchecked"})
    protected void onMasterToggle(MasterDetailRowPanel row, AjaxRequestTarget target) {
        final M m = (M) row.getModelObject();
        if (openedItems.contains(m)) {
            openedItems.remove(m);
        } else {
            openedItems.add(m);
        }
        target.addComponent(MasterDetailTable.this);
    }

    protected abstract String getMasterLabel(M masterObject);

    protected abstract List<D> getDetails(M masterObject);


    private static class SpaceColumn extends AbstractColumn {
        private String cssClass;

        public SpaceColumn(String cssClass) {
            super(new Model(""));
            this.cssClass = cssClass;
        }

        public void populateItem(Item cellItem, String componentId, IModel rowModel) {
            cellItem.add(new Label(componentId, "."));
        }

        @Override
        public String getCssClass() {
            return cssClass;
        }
    }

    private static class MasterDataProvider<M extends Serializable, D extends Serializable> extends SortableDataProvider {
        private List<?> list;

        private MasterDataProvider(List<?> list) {
            this.list = list;
        }

        @SuppressWarnings({"unchecked"})
        public Iterator iterator(int first, int count) {
            final SortParam sortParam = getSort();
            if (sortParam != null && sortParam.getProperty().startsWith("master.")) {
                ListPropertySorter.sort(list, sortParam);
                final String property = sortParam.getProperty().substring(7);
                final SortDefinition sortDefinition = new MutableSortDefinition(property, true, sortParam.isAscending());
                Collections.sort(list, new PropertyComparator(sortDefinition));
            }
            List<?> result = list.subList(first, first + count);
            return result.iterator();
        }

        public int size() {
            return list.size();
        }

        @SuppressWarnings({"unchecked"})
        public IModel model(Object object) {
            return new Model(new MasterDetailEntry<M, D>((M) object, null));
        }
    }

    private static IColumn[] addSpaceColumns(IColumn[] columns) {
        columns = (IColumn[]) ArrayUtils.add(columns, 0, new SpaceColumn("first-cell"));
        columns = (IColumn[]) ArrayUtils.add(columns, new SpaceColumn("last-cell"));
        return columns;
    }
}
