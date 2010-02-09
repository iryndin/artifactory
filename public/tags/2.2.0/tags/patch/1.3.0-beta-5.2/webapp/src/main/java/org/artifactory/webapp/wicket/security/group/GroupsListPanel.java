/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.security.group;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.component.AjaxDeleteRow;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.component.table.SingleSelectionTable;
import org.artifactory.webapp.wicket.utils.ComparablePropertySorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public class GroupsListPanel extends TitledPanel {

    @SpringBean
    private UserGroupService userGroupService;

    List<GroupInfo> cachedGroups;

    public GroupsListPanel(String id) {
        super(id);

        List<IColumn> columns = createTableColumns();
        SingleSelectionTable<GroupInfo> table = createTable(columns);
        add(table);
    }

    void refreshGroups() {
        cachedGroups = userGroupService.getAllGroups();
    }

    private List<IColumn> createTableColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Group name"), "groupName", "groupName") {
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                super.populateItem(item, componentId, model);
                item.add(new CssClass("group nowrap"));
            }
        });

        columns.add(new PropertyColumn(new Model("Description"), "description") {
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                super.populateItem(item, componentId, model);
                GroupInfo groupInfo = (GroupInfo) model.getObject();
                String description = groupInfo.getDescription();
                item.add(new SimpleAttributeModifier("title",
                        description != null ? description : ""));
                item.add(new CssClass("one-line"));
            }
        });

        columns.add(new AbstractColumn(new Model()) {
            public void populateItem(Item cellItem, String componentId, IModel model) {
                cellItem.add(new CssClass("DeleteColumn"));
                cellItem.add(
                        new AjaxDeleteRow<GroupInfo>(componentId, model, GroupsListPanel.this) {
                            @Override
                            protected void doDelete() {
                                String groupName = getToBeDeletedObject().getGroupName();
                                userGroupService.deleteGroup(groupName);
                            }

                            @Override
                            protected void onDeleted(AjaxRequestTarget target, Component listener) {
                                //Refresh the groups list
                                refreshGroups();
                                target.addComponent(listener);
                            }

                            @Override
                            protected String getConfirmationQuestion() {
                                String name = getToBeDeletedObject().getGroupName();
                                return "Are you sure you wish to delete the group " + name + "?";
                            }
                        });
            }
        });
        return columns;
    }

    private SingleSelectionTable<GroupInfo> createTable(final List<IColumn> columns) {
        SingleSelectionTable<GroupInfo> table =
                new SingleSelectionTable<GroupInfo>("groups", columns,
                        new SortableGroupsDataProvider(), 10) {
                    @Override
                    protected void onRowSelected(GroupInfo selection, AjaxRequestTarget target) {
                        super.onRowSelected(selection, target);
                        getCreateUpdatePanel().replaceWith(target,
                                (getGroupsPage().newUpdatePanel(selection)));
                    }
                };
        return table;
    }

    private GroupsPage getGroupsPage() {
        return (GroupsPage) getPage();
    }

    private CreateUpdatePanel<GroupInfo> getCreateUpdatePanel() {
        return (GroupCreateUpdatePanel) getPage().get("createUpdate");
    }

    private List<GroupInfo> getGroups() {
        if (cachedGroups == null) {
            refreshGroups();
        }
        return cachedGroups;
    }

    private class SortableGroupsDataProvider extends SortableDataProvider {
        public SortableGroupsDataProvider() {
            // default sort by group name
            setSort("groupName", true);
        }

        public Iterator iterator(int first, int count) {
            List<GroupInfo> groupsSubList = getGroups().subList(first, first + count);
            ComparablePropertySorter<GroupInfo> propertySorter =
                    new ComparablePropertySorter<GroupInfo>(GroupInfo.class);
            propertySorter.sort(groupsSubList, getSort());
            return groupsSubList.iterator();
        }

        public int size() {
            return getGroups().size();
        }

        public IModel model(Object object) {
            return new Model((GroupInfo) object);
        }
    }

}