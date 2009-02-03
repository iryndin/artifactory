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
package org.artifactory.webapp.wicket.security.user;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.component.AjaxDeleteRow;
import org.artifactory.webapp.wicket.component.BooleanColumn;
import org.artifactory.webapp.wicket.component.CheckboxColumn;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.component.table.SingleSelectionTable;
import org.artifactory.webapp.wicket.security.user.column.UserColumn;
import org.artifactory.webapp.wicket.utils.ComparablePropertySorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 * @author Yossi Shaul
 */
public class UsersListPanel extends TitledPanel {

    @SpringBean
    private UserGroupService userGroupService;

    private SingleSelectionTable<UserModel> table;

    private List<UserModel> users;

    private SortableUsersDataProvider usersDataProvider;
    private UsersFilterPanel usersFilterPanel;

    public UsersListPanel(String string) {
        super(string);

        usersFilterPanel = new UsersFilterPanel("usersFilterPanel", this);
        add(usersFilterPanel);

        add(new GroupManagementPanel("groupManagementPanel", this));

        addUsersTable();
    }

    List<String> getSelectedUsernames() {
        List<String> selectedUsernames = new ArrayList<String>();
        for (UserModel userModel : users) {
            if (userModel.isSelected()) {
                selectedUsernames.add(userModel.getUsername());
            }
        }
        return selectedUsernames;
    }

    private void addUsersTable() {
        List<IColumn> columns = new ArrayList<IColumn>();

        //Add a dummy div so that it can be ajax'ed replaced
        WebMarkupContainer usersTableContainer = new WebMarkupContainer("usersContainer");
        usersTableContainer.setOutputMarkupId(true);
        add(usersTableContainer);

        columns.add(new CheckboxColumn<UserModel>("", "selected", usersTableContainer) {
            @Override
            protected void doUpdate(UserModel userModel, boolean checked,
                    AjaxRequestTarget target) {
                //clearFeedback(target);
            }
        });

        columns.add(new UserColumn(new Model("User name")));
        columns.add(new BooleanColumn(new Model("Admin"), "adminString", "admin"));
        columns.add(new AbstractColumn(new Model()) {
            public void populateItem(Item cellItem, String componentId, IModel model) {
                cellItem.add(new CssClass("DeleteColumn"));
                cellItem.add(new AjaxDeleteRow<UserModel>(componentId, model, UsersListPanel.this) {

                    @Override
                    protected void doDelete() {
                        String username = getToBeDeletedObject().getUsername();
                        userGroupService.deleteUser(username);
                    }

                    @Override
                    protected void onDeleted(AjaxRequestTarget target, Component listener) {
                        //Refresh the users list
                        refreshUsersList(target);
                        target.addComponent(listener);
                        //Hide the update panel
                        displayCreatePanel(target);
                    }

                    @Override
                    protected String getConfirmationQuestion() {
                        String name = getToBeDeletedObject().getUsername();
                        return "Are you sure you wish to delete the user " + name + "?";
                    }
                });
            }
        });

        usersDataProvider = new SortableUsersDataProvider();
        table = new SingleSelectionTable<UserModel>("users", columns, usersDataProvider, 5) {
            @Override
            protected void onRowSelected(UserModel selection, AjaxRequestTarget target) {
                super.onRowSelected(selection, target);
                for (UserModel user : users) {
                    user.setSelected(false);
                }
                selection.setSelected(true);
                getCreateUpdatePanel().replaceWith(
                        target, (getUsersPage().newUpdatePanel(selection)));
            }
        };

        usersTableContainer.add(table);
    }

    private class SortableUsersDataProvider extends SortableDataProvider {
        private SortParam previousSort;

        public SortableUsersDataProvider() {
            //Set default sort
            setSort("username", true);
            previousSort = getSort();
            recalcUsersList();
        }

        public Iterator iterator(int first, int count) {
            if (!previousSort.equals(getSort())) {
                sortUsers();
            }
            List<UserModel> usersSubList = users.subList(first, first + count);
            return usersSubList.iterator();
        }

        public int size() {
            return users.size();
        }

        public IModel model(Object object) {
            return new Model((UserModel) object);
        }

        public void recalcUsersList() {
            users = getFilteredUsers();
            sortUsers();
        }

        private void sortUsers() {
            previousSort = getSort();
            if (users != null) {
                ComparablePropertySorter<UserModel> propertySorter =
                        new ComparablePropertySorter<UserModel>(UserModel.class);
                propertySorter.sort(users, getSort());
            }
        }
    }

    private CreateUpdatePanel<UserModel> getCreateUpdatePanel() {
        return (UserCreateUpdatePanel) getUsersPage().get("createUpdate");
    }

    void displayCreatePanel(AjaxRequestTarget target) {
        getCreateUpdatePanel().replaceWith(target, (getUsersPage().newCreatePanel()));
    }

    private UsersPage getUsersPage() {
        return (UsersPage) getPage();
    }

    private List<UserModel> getFilteredUsers() {
        List<UserInfo> allUsers = userGroupService.getAllUsers(true);
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserInfo userInfo : allUsers) {
            //Don't list anonymous and excluded users
            if (!userInfo.isAnonymous() && includedByFilter(userInfo)) {
                users.add(new UserModel(userInfo));
            }
        }
        return users;
    }

    /**
     * @param userInfo The user to check if to include in the table
     * @return True if the user should be included
     */
    private boolean includedByFilter(UserInfo userInfo) {
        return passesUsernameFilter(userInfo) && passesGroupNameFilter(userInfo);
    }

    private boolean passesUsernameFilter(UserInfo userInfo) {
        String usernameFilter = usersFilterPanel.getUsernameFilter();
        return (usernameFilter == null || userInfo.getUsername().contains(usernameFilter));
    }

    private boolean passesGroupNameFilter(UserInfo userInfo) {
        String groupNameFilter = usersFilterPanel.getGroupFilter();
        return (groupNameFilter == null || userInfo.getGroups().contains(groupNameFilter));
    }

    static class TargetGroupDropDownChoice extends DropDownChoice {
        public TargetGroupDropDownChoice(String id, IModel model, List<GroupInfo> groups) {
            super(id, model, groups);
            setPersistent(true);
            setOutputMarkupId(true);
        }
    }

    static class FilterGroupDropDownChoice extends DropDownChoice {
        public FilterGroupDropDownChoice(String id, IModel model, List<GroupInfo> groups) {
            super(id, model, groups);
            setPersistent(true);
            setOutputMarkupId(true);
            setNullValid(true);
        }
    }

    public void refreshUsersList(AjaxRequestTarget target) {
        usersDataProvider.recalcUsersList();
        target.addComponent(table);
    }
}
