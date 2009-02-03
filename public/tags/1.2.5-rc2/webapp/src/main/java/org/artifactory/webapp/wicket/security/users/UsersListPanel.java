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
package org.artifactory.webapp.wicket.security.users;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.security.ExtendedUserDetailsService;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class UsersListPanel extends TitlePanel {

    public UsersListPanel(String string, final UserPanel createPanel, final UserPanel updatePanel) {
        super(string);
        //Results table
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Username"), "username", "username"));
        columns.add(new PropertyColumn(new Model("Admin"), "admin", "admin"));
        columns.add(new AbstractColumn(new Model()) {
            public void populateItem(Item cellItem, String componentId, IModel model) {
                cellItem.add(new Delete(componentId, model, createPanel));
            }
        });

        AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("users", columns,
                new SortableUsersDataProvider(), 10) {

            //Handle row selection
            @Override
            protected Item newCellItem(final String id, int index, final IModel model) {
                Item item = super.newCellItem(id, index, model);
                Object modelObject = model.getObject();
                if (modelObject instanceof PropertyColumn) {
                    //Don't add behavior to the delete column
                    item.add(new AjaxEventBehavior("onMouseUp") {
                        protected void onEvent(final AjaxRequestTarget target) {
                            //Show the update panel
                            User user =
                                    (User) getComponent().getParent().getParent().getModelObject();
                            updatePanel.updateModelObject(user, target);
                        }
                    });
                }
                return item;
            }
        };
        add(table);
    }

    private class SortableUsersDataProvider extends SortableDataProvider {
        public SortableUsersDataProvider() {
            //Set default sort
            setSort("username", true);
        }

        public Iterator iterator(int first, int count) {
            List<User> list = getUsers().subList(first, first + count);
            SortParam sp = getSort();
            boolean asc = sp.isAscending();
            String prop = sp.getProperty();
            if (("username").equals(prop) && asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u1.getUsername().compareTo(u2.getUsername());
                    }
                });
            } else if (("username").equals(prop) && !asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u2.getUsername().compareTo(u1.getUsername());
                    }
                });
            } else if (("admin").equals(prop) && asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u1.isAdmin() && (u2.isAdmin()) ? 1 : 0;
                    }
                });
            } else if (("admin").equals(prop) && !asc) {
                Collections.sort(list, new Comparator<User>() {
                    public int compare(User u1, User u2) {
                        return u1.isAdmin() && (u2.isAdmin()) ? 0 : 1;
                    }
                });
            }
            return list.iterator();
        }

        public int size() {
            return getUsers().size();
        }

        public IModel model(Object object) {
            return new Model((User) object);
        }
    }

    private class Delete extends WebMarkupContainer {

        private User user;

        public Delete(String id, IModel model, final UserPanel panel) {
            super(id);
            user = (User) model.getObject();
            add(new AjaxEventBehavior("onClick") {
                protected void onEvent(final AjaxRequestTarget target) {
                    ArtifactoryContext context = ContextHelper.get();
                    SecurityHelper security = context.getSecurity();
                    ExtendedUserDetailsService userDetailsService =
                            security.getUserDetailsService();
                    String username = user.getUsername();
                    userDetailsService.deleteUser(username);
                    panel.show(target);
                }

                @SuppressWarnings({"UnnecessaryLocalVariable"})
                @Override
                protected CharSequence getCallbackScript() {
                    CharSequence orig = super.getCallbackScript();
                    String callbackScript =
                            "if (confirm('Are you sure you wish to delete the user " +
                                    user.getUsername() + "?')) {" +
                                    orig + "} else { return false; }";
                    return callbackScript;
                }
            });
        }

        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            tag.setName("img");
            tag.put("src", "../images/delete.png");
            tag.put("alt", "Delete");
            tag.put("style", "cursor:pointer;");
        }

        @Override
        protected void onComponentTagBody(
                final MarkupStream markupStream, final ComponentTag openTag) {
            replaceComponentTagBody(markupStream, openTag, "");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private List<User> getUsers() {
        ArtifactoryContext context = ContextHelper.get();
        SecurityHelper security = context.getSecurity();
        List<SimpleUser> simpleUsers = security.getUserDetailsService().getAllUsers();
        List<User> users = new ArrayList<User>();
        for (SimpleUser simpleUser : simpleUsers) {
            users.add(new User(simpleUser));
        }
        return users;
    }
}
