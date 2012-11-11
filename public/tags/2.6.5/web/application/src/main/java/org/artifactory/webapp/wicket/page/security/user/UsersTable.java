/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.security.user;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserAwareAuthenticationProvider;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.label.tooltip.TooltipLabel;
import org.artifactory.common.wicket.component.modal.links.ModalShowLink;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.common.wicket.component.table.columns.TooltipLabelColumn;
import org.artifactory.common.wicket.component.table.columns.checkbox.SelectAllCheckboxColumn;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.AccessLogger;
import org.artifactory.webapp.wicket.page.security.user.column.UserColumn;
import org.artifactory.webapp.wicket.page.security.user.permission.UserPermissionsPanel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The users table is special in that it is sensitive to a filter and also contains a checkbox column.
 *
 * @author Yossi Shaul
 */
public class UsersTable extends ModalListPanel<UserModel> {
    private static final Logger log = LoggerFactory.getLogger(UsersTable.class);

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private UserAwareAuthenticationProvider provider;

    private UsersTableDataProvider dataProvider;

    public UsersTable(String id, UsersTableDataProvider dataProvider) {
        super(id, dataProvider);
        this.dataProvider = dataProvider;
    }

    @Override
    public String getTitle() {
        return "Users List";
    }

    @Override
    protected List<UserModel> getList() {
        throw new UnsupportedOperationException("Users table uses it's own data provider");
    }

    @Override
    protected void addColumns(List<? super IColumn<UserModel>> columns) {
        columns.add(new SelectAllCheckboxColumn<UserModel>("", "selected", null) {

            @Override
            protected boolean isEnabled(UserModel userModel) {
                //Enable the user's individual checkbox only if it's not anonymous
                return !userModel.isAnonymous();
            }

            @Override
            protected boolean canChangeItemSelectionState(UserModel userModel) {
                //Make the user's selection state affected by the select all checkbox only if it's not anonymous
                return !userModel.isAnonymous();
            }
        });
        columns.add(new UserColumn("User Name"));
        columns.add(createExternalStatusColumn());
        columns.add(new BooleanColumn<UserModel>("Admin", "admin", "admin"));
        columns.add(new PropertyColumn<UserModel>(Model.of("Last Login"), "lastLoginTimeMillis",
                "lastLoginString"));
    }

    @Override
    protected boolean canAddRowItemDoubleClickBehavior(IModel<UserModel> model) {
        //Respond to double clicks on the row only if it's not anonymous
        return !model.getObject().isAnonymous();
    }

    private TooltipLabelColumn<UserModel> createExternalStatusColumn() {
        return new TooltipLabelColumn<UserModel>(Model.of("Realm"), "realm", "status.description", 0) {
            @Override
            public void populateItem(Item<ICellPopulator<UserModel>> item, String componentId,
                    final IModel<UserModel> model) {
                item.add(new TooltipLabel(componentId, createLabelModel(model), 0) {
                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        UserModel userModel = model.getObject();

                        if (userModel.isAnonymous()) {
                            setText("");
                        } else if (StringUtils.isBlank(userModel.getRealm())) {
                            setText("Will be updated on next login");
                            add(new CssClass("gray-listed-label"));
                        } else {
                            setText(StringUtils.capitalize(userModel.getRealm()));
                        }
                    }
                });
                UserModel user = model.getObject();
                log.debug("User '{}' is from realm '{}'", user.getUsername(), user.getRealm());
                if ("internal".equals(user.getRealm())) {
                    user.setStatus(UserModel.Status.NOT_EXTERNAL_USER);
                } else if ("system".equals(user.getRealm())) {
                    user.setStatus(UserModel.Status.ACTIVE_USER);
                } else {
                    if (provider.userExists(user.getUsername(), user.getRealm())) {
                        user.setStatus(UserModel.Status.ACTIVE_USER);
                    } else {
                        user.setStatus(UserModel.Status.INACTIVE_USER);
                        item.add(new CssClass("black-listed-label"));
                    }
                }
            }
        };
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        Set<String> defaultGroupsNames = userGroupService.getNewUserDefaultGroupsNames();
        return new UserCreateUpdatePanel(CreateUpdateAction.CREATE, new UserModel(defaultGroupsNames), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(UserModel user) {
        return new UserCreateUpdatePanel(CreateUpdateAction.UPDATE, user, this);
    }

    protected BaseModalPanel newViewUserPermissionsPanel(UserModel user) {
        return new UserPermissionsPanel(user);
    }

    @Override
    protected String getDeleteConfirmationText(UserModel user) {
        return "Are you sure you wish to delete the user '" + user.getUsername() + "'?";
    }

    @Override
    protected void deleteItem(UserModel user, AjaxRequestTarget target) {
        String currentUsername = authorizationService.currentUsername();
        String selectedUsername = user.getUsername();
        if (currentUsername.equals(selectedUsername)) {
            error("Action cancelled. You are logged-in as the user you have selected for removal.");
            return;
        }
        userGroupService.deleteUser(selectedUsername);
        AccessLogger.deleted("User " + selectedUsername + " was deleted successfully");
        refreshUsersList(target);
    }

    @Override
    protected void addLinks(List<AbstractLink> links, final UserModel userModel, String linkId) {
        //Do not add the delete and edit links to the anonymous user
        if (!userModel.isAnonymous()) {
            super.addLinks(links, userModel, linkId);
        }
        // add view user permissions link
        ModalShowLink viewPermissionsLink = new ModalShowLink(linkId, "Permissions") {
            @Override
            protected BaseModalPanel getModelPanel() {
                return newViewUserPermissionsPanel(userModel);
            }
        };
        viewPermissionsLink.add(new CssClass("icon-link"));
        viewPermissionsLink.add(new CssClass("ViewUserPermissionsAction"));
        links.add(viewPermissionsLink);
    }

    public void refreshUsersList(AjaxRequestTarget target) {
        dataProvider.recalcUsersList();
        target.add(this);
    }

    List<String> getSelectedUsernames() {
        List<String> selectedUsernames = new ArrayList<String>();
        for (UserModel userModel : dataProvider.getUsers()) {
            if (userModel.isSelected()) {
                selectedUsernames.add(userModel.getUsername());
            }
        }
        return selectedUsernames;
    }
}
