package org.artifactory.webapp.wicket.common.component.panel.list;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.CancelDefaultDecorator;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalShowLink;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.common.component.table.SortableTable;
import org.artifactory.webapp.wicket.common.component.table.columns.LinksColumn;
import org.artifactory.webapp.wicket.utils.ListPropertySorter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public abstract class ListPanel<T> extends TitledPanel {
    private static final int DEFAULT_ROWS_PER_PAGE = 8;
    private String defaultInitialSortProperty;
    private ModalShowLink newItemLink;

    protected ListPanel(String id) {
        super(id);
        init(new DefaultSortableDataProvider());
    }

    protected ListPanel(String id, SortableDataProvider dataProvider) {
        super(id);
        init(dataProvider);
    }

    private void init(SortableDataProvider dataProvider) {
        add(new CssClass("list-panel"));

        // add new item link
        newItemLink = new ModalShowLink("newItemLink", "New") {
            @Override
            protected BaseModalPanel getModelPanel() {
                return newCreateItemPanel();
            }
        };
        add(newItemLink);

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

    private void addLinksColumn(List<IColumn> columns) {
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

    protected void addLinks(List<AbstractLink> links, final T itemObject, String linkId) {
        // add edit link
        ModalShowLink editLink = new ModalShowLink(linkId, "Edit") {
            @Override
            protected BaseModalPanel getModelPanel() {
                return newUpdateItemPanel(itemObject);
            }
        };
        editLink.add(new CssClass("icon-link"));
        editLink.add(new CssClass("UpdateAction"));
        links.add(editLink);

        // add delete link
        TitledAjaxLink deleteLink = new TitledAjaxLink(linkId, "Remove") {
            public void onClick(AjaxRequestTarget target) {
                deleteItem(itemObject, target);
                target.addComponent(ListPanel.this);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ConfirmAjaxCallDecorator(itemObject);
            }
        };
        deleteLink.add(new CssClass("icon-link"));
        deleteLink.add(new CssClass("RemoveAction"));
        links.add(deleteLink);
    }

    protected void disableNewItemLink() {
        newItemLink.setEnabled(false);
    }

    @SuppressWarnings({"AbstractMethodOverridesConcreteMethod"})
    @Override
    public abstract String getTitle();

    protected abstract List<T> getList();

    protected abstract void addColumns(List<IColumn> columns);

    protected abstract BaseModalPanel newCreateItemPanel();

    protected abstract BaseModalPanel newUpdateItemPanel(T itemObject);

    protected abstract String getDeleteConfirmationText(T itemObject);

    protected abstract void deleteItem(T itemObject, AjaxRequestTarget target);

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

    private class ConfirmAjaxCallDecorator implements IAjaxCallDecorator {
        private final T rowObject;

        private ConfirmAjaxCallDecorator(T rowObject) {
            this.rowObject = rowObject;
        }

        public CharSequence decorateScript(CharSequence script) {
            return "if (confirm(' "
                    + getDeleteConfirmationText(rowObject).replaceAll("'", "''")
                    + "')) {"
                    + script + "} else { return false; }";
        }

        public CharSequence decorateOnSuccessScript(CharSequence script) {
            return script;
        }

        public CharSequence decorateOnFailureScript(CharSequence script) {
            return script;
        }
    }

    private class MySortableTable extends SortableTable {
        private MySortableTable(List<IColumn> columns, SortableDataProvider dataProvider) {
            super("table", columns, dataProvider, ListPanel.this.getRowsPerPage());
        }

        @SuppressWarnings({"RefusedBequest"})
        @Override
        protected Item newRowItem(String id, int index, final IModel model) {
            Item item = super.newRowItem(id, index, model);
            item.add(new AjaxEventBehavior("oncontextmenu") {
                @SuppressWarnings({"unchecked"})
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    ModalHandler modalHandler = ModalHandler.getInstanceFor(ListPanel.this);
                    T itemObject = (T) model.getObject();
                    modalHandler.setModalPanel(newUpdateItemPanel(itemObject));
                    modalHandler.show(target);
                }

                @Override
                protected IAjaxCallDecorator getAjaxCallDecorator() {
                    return new CancelDefaultDecorator();
                }
            });
            return item;
        }
    }
}
