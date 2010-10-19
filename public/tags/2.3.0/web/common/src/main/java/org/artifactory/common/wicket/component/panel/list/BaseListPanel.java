/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.common.wicket.component.panel.list;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.ajax.CancelDefaultDecorator;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.PlaceHolder;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.LinksColumn;
import org.artifactory.common.wicket.util.ListPropertySorter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Contains most of the implementations of the former ListPanel (Now ModalListPanel). Enables seperation between list
 * panel and the action link behavior.
 *
 * @author Noam Tenne
 */
public abstract class BaseListPanel<T> extends TitledPanel {
    protected static final int DEFAULT_ROWS_PER_PAGE = 15;

    protected String defaultInitialSortProperty;
    protected TitledAjaxLink newItemLink;
    protected SortableDataProvider dataProvider;

    protected BaseListPanel(String id) {
        super(id);
        dataProvider = new DefaultSortableDataProvider();
    }

    protected BaseListPanel(String id, SortableDataProvider dataProvider) {
        super(id);
        this.dataProvider = dataProvider;
    }

    public final SortableDataProvider getDataProvider() {
        return dataProvider;
    }

    protected void init() {
        add(new CssClass("list-panel"));

        // add new item link
        newItemLink = getNewItemLink();
        add(newItemLink);

        Component exportItemLink = newExportLink("export");
        add(exportItemLink);

        // add table
        List<IColumn> columns = new ArrayList<IColumn>();
        addLinksColumn(columns);
        addColumns(columns);
        // by default use the sort property of the second column
        if (columns.size() > 1) {
            defaultInitialSortProperty = columns.get(1).getSortProperty();
        }

        add(new MySortableTable(columns, dataProvider));
    }

    /**
     * @return The property by which the table will initialy be sorted by.
     */
    protected String getInitialSortProperty() {
        return defaultInitialSortProperty;
    }

    protected void addLinksColumn(List<IColumn> columns) {
        columns.add(new LinksColumn<T>() {
            @Override
            protected Collection<? extends AbstractLink> getLinks(T rowObject, String linkId) {
                List<AbstractLink> links = new ArrayList<AbstractLink>();
                addLinks(links, rowObject, linkId);
                return links;
            }
        });
    }

    protected int getRowsPerPage() {
        return DEFAULT_ROWS_PER_PAGE;
    }

    protected void disableNewItemLink() {
        newItemLink.setEnabled(false);
    }

    @SuppressWarnings({"AbstractMethodOverridesConcreteMethod"})
    @Override
    public abstract String getTitle();

    protected abstract List<T> getList();

    protected abstract void addColumns(List<IColumn> columns);

    protected abstract String getDeleteConfirmationText(T itemObject);

    protected abstract void deleteItem(T itemObject, AjaxRequestTarget target);

    protected abstract TitledAjaxLink getNewItemLink();

    protected Component newExportLink(String id) {
        return new PlaceHolder(id);
    }

    protected abstract TitledAjaxLink getEditItemLink(final T itemObject, String linkId);

    protected abstract void onRowItemEvent(String id, int index, final IModel model, AjaxRequestTarget target);

    private class DefaultSortableDataProvider extends SortableDataProvider {
        private DefaultSortableDataProvider() {
            if (defaultInitialSortProperty != null) {
                setSort(defaultInitialSortProperty, true);
            }
        }

        public Iterator iterator(int first, int count) {
            List<T> items = getList();
            ListPropertySorter.sort(items, getSort());
            List<T> itemsSubList = items.subList(first, first + count);
            return itemsSubList.iterator();
        }

        public int size() {
            return getList().size();
        }

        public IModel model(Object object) {
            return new Model((Serializable) object);
        }
    }

    public SortableTable getTable() {
        return (SortableTable) get("table");
    }

    private class MySortableTable extends SortableTable {
        private MySortableTable(List<IColumn> columns, SortableDataProvider dataProvider) {
            super("table", columns, dataProvider, BaseListPanel.this.getRowsPerPage());
        }

        @SuppressWarnings({"RefusedBequest"})
        @Override
        protected Item newRowItem(final String id, final int index, final IModel model) {
            Item item = super.newRowItem(id, index, model);
            item.add(new AjaxEventBehavior("ondblclick") {
                @SuppressWarnings({"unchecked"})
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    onRowItemEvent(id, index, model, target);
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new CancelDefaultDecorator();
                }
            });
            return item;
        }
    }

    protected void addLinks(List<AbstractLink> links, final T itemObject, String linkId) {
        // add edit link
        TitledAjaxLink editLink = getEditItemLink(itemObject, linkId);
        editLink.add(new CssClass("icon-link"));
        editLink.add(new CssClass("UpdateAction"));
        links.add(editLink);

        // add delete link
        TitledAjaxLink deleteLink = new TitledAjaxLink(linkId, "Delete") {
            public void onClick(AjaxRequestTarget target) {
                deleteItem(itemObject, target);
                target.addComponent(BaseListPanel.this);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ConfirmationAjaxCallDecorator(getDeleteConfirmationText(itemObject));
            }
        };
        deleteLink.add(new CssClass("icon-link"));
        deleteLink.add(new CssClass("DeleteAction"));
        links.add(deleteLink);
    }
}
