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

package org.artifactory.webapp.wicket.page.security.user;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.LdapService;
import org.artifactory.api.security.LdapUser;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.label.TooltipLabel;
import org.artifactory.common.wicket.component.modal.links.ModalShowLink;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.common.wicket.component.table.columns.TooltipLabelColumn;
import org.artifactory.common.wicket.component.table.columns.checkbox.SelectAllCheckboxColumn;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.log.LoggerFactory;
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
    private LdapService ldapService;

    @SpringBean
    private CentralConfigService centralConfigService;

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
    protected void addColumns(List<IColumn> columns) {
        columns.add(new SelectAllCheckboxColumn<UserModel>("", "selected", null));
        columns.add(new UserColumn(new Model("User Name")));
        columns.add(createExternalStatusColumn());
        columns.add(new BooleanColumn(new Model("Admin"), "admin", "admin"));
        columns.add(new PropertyColumn(new Model("Last Login"), "lastLoginTimeMillis", "lastLoginString"));
        //columns.add(new PropertyColumn(new Model("Last Access"), "lastAccessTimeMillis", "lastAccessString"));
    }

    private TooltipLabelColumn createExternalStatusColumn() {
        final LdapSetting enabledLdapSettings =
                centralConfigService.getDescriptor().getSecurity().getEnabledLdapSettings();
        return new TooltipLabelColumn(new Model("External"), "status", "status", 0) {
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                item.add(new TooltipLabel(componentId, createLabelModel(model), 0) {
                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        setText("");
                    }
                });
                UserModel user = (UserModel) model.getObject();
                if (user.getStatus().equals(UserModel.Status.NOT_LDAP_USER)) {
                    log.debug("User {} is not an LDAP user", user.getUsername());
                    return;
                }
                if (user.isDisableInternalPassword() && enabledLdapSettings != null) {
                    LdapUser ldapUser = ldapService.getDnFromUserName(enabledLdapSettings, user.getUsername());
                    if (ldapUser == null) {
                        item.add(new CssClass("WarnColumn"));
                        item.add(new CssClass("warn"));
                        user.setStatus(UserModel.Status.INACTIVE_USER);
                    } else {
                        item.add(new CssClass("BooleanColumn"));
                        item.add(new CssClass("true"));
                        user.setStatus(UserModel.Status.ACTIVE_USER);
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
        return "Are you sure you wish to delete the user " + user.getUsername() + "?";
    }

    @Override
    protected void deleteItem(UserModel user, AjaxRequestTarget target) {
        String currentUsername = authorizationService.currentUsername();
        String selectedUsername = user.getUsername();
        if (currentUsername.equals(selectedUsername)) {
            error("Error: Action cancelled. You are logged-in as the user you have selected for removal.");
            return;
        }
        userGroupService.deleteUser(selectedUsername);
        refreshUsersList(target);
    }

    @Override
    protected void addLinks(List<AbstractLink> links, final UserModel userModel, String linkId) {
        super.addLinks(links, userModel, linkId);
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
        target.addComponent(this);
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
