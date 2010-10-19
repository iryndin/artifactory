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

package org.artifactory.common.wicket.component.table.groupable;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.table.groupable.provider.GroupableDataProvider;

import java.util.Iterator;

/**
 * @author Yoav Aharoni
 */
public class GroupedItemsStrategy implements IItemReuseStrategy {
    private GroupableTable table;

    GroupedItemsStrategy(GroupableTable table) {
        this.table = table;
    }

    public Iterator getItems(final IItemFactory factory, final Iterator newModels, Iterator existingItems) {
        return new Iterator() {
            private int index = 0;

            private Object lastGroupValue;
            private Item lastGroupItem;
            private IModel lastGroupModel;

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return lastGroupModel != null || newModels.hasNext();
            }

            public Object next() {
                // returned group item in last iteration, return saved model item
                if (lastGroupModel != null) {
                    Item item = newRowItem(lastGroupModel);
                    lastGroupModel = null;
                    return item;
                }

                Object next = newModels.next();
                if (next != null && !(next instanceof IModel)) {
                    throw new WicketRuntimeException("Expecting an instance of " +
                            IModel.class.getName() + ", got " + next.getClass().getName());
                }
                final IModel model = (IModel) next;

                // check if grouped
                final GroupableDataProvider provider = table.getGroupableDataProvider();
                SortParam groupParam = provider.getGroupParam();
                if (groupParam != null && model != null) {
                    String property = groupParam.getProperty();
                    IChoiceRenderer reneder = provider.getGroupReneder(property);
                    Object modelObject = model.getObject();
                    Object value = reneder.getIdValue(modelObject, index);
                    if (!value.equals(lastGroupValue)) {
                        lastGroupValue = value;
                        lastGroupModel = model;

                        lastGroupItem = table.newGroupRowItem("group" + index, index, model);
                        Item cellItem = table.newGroupCellItem("cells", 0, model);
                        lastGroupItem.add(cellItem);
                        table.populateGroupItem(cellItem, "cell", property, model);
                        return lastGroupItem;
                    }
                }

                return newRowItem(model);
            }

            private Item newRowItem(IModel model) {
                Item item = factory.newItem(index, model);
                if (lastGroupItem != null && !table.isGroupExpanded(lastGroupItem) &&
                        table.getGroupableDataProvider().getGroupParam() != null) {
                    item.add(new CssClass("row-collapsed"));
                }
                index++;
                return item;
            }
        };
    }
}
