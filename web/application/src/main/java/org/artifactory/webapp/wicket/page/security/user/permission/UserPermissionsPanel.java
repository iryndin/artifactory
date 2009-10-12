/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.security.user.permission;

import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.webapp.wicket.page.security.acl.AclsPage;
import org.artifactory.webapp.wicket.page.security.user.UserModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public class UserPermissionsPanel extends BaseModalPanel {

    @SpringBean
    private UserGroupService userGroupService;

    @SpringBean
    private AclService aclService;

    private UserInfo userInfo;

    public UserPermissionsPanel(UserModel user) {
        setWidth(400);
        setTitle(String.format("%s's Permission Targets", user.getUsername()));
        userInfo = userGroupService.findUser(user.getUsername());

        TitledBorder border = new TitledBorder("border");
        add(border);
        border.add(addTable());
    }

    private SortableTable addTable() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn(new Model("Permission Target"), "permissionTarget.name") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                cellItem.add(new LinkPanel(componentId, rowModel));
            }
        });

        columns.add(new BooleanColumn(new Model("Admin"), "admin", "admin"));
        columns.add(new BooleanColumn(new Model("Delete"), "delete", "delete"));
        columns.add(new BooleanColumn(new Model("Deploy"), "deploy", "deploy"));
        columns.add(new BooleanColumn(new Model("Read"), "read", "read"));

        PermissionsTabTableDataProvider dataProvider = new PermissionsTabTableDataProvider(userInfo);
        return new SortableTable("userPermissionsTable", columns, dataProvider, 10);
    }

    class PermissionsTabTableDataProvider extends SortableDataProvider {
        private final UserInfo userInfo;
        private List<UserPermissionsRow> userPermissions;

        public PermissionsTabTableDataProvider(UserInfo userInfo) {
            setSort("permissionTarget.name", true);
            this.userInfo = userInfo;
            loadData();
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(userPermissions, getSort());
            List<UserPermissionsRow> list = userPermissions.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return userPermissions.size();
        }

        public IModel model(Object object) {
            return new Model((UserPermissionsRow) object);
        }

        private void loadData() {
            userPermissions = new ArrayList<UserPermissionsRow>();
            List<AclInfo> acls = aclService.getAllAcls();
            for (AclInfo acl : acls) {
                UserPermissionsRow permissionRow = createPermissionRow(acl);
                addIfHasPermissions(permissionRow, userPermissions);
            }
        }

        private UserPermissionsRow createPermissionRow(AclInfo acl) {
            PermissionTargetInfo target = acl.getPermissionTarget();
            UserPermissionsRow permissionsRow = new UserPermissionsRow(target);
            permissionsRow.setRead(aclService.canRead(userInfo, target));
            permissionsRow.setDeploy(aclService.canDeploy(userInfo, target));
            permissionsRow.setDelete(aclService.canDelete(userInfo, target));
            permissionsRow.setAdmin(aclService.canAdmin(userInfo, target));
            return permissionsRow;
        }

        private void addIfHasPermissions(UserPermissionsRow permissionRow, List<UserPermissionsRow> userPermissions) {
            if (permissionRow.hasPermissions()) {
                // only add users/groups who have some permission
                userPermissions.add(permissionRow);
            }
        }
    }

    private class UserPermissionsRow implements Serializable {
        private PermissionTargetInfo permissionTarget;
        private boolean read;
        private boolean deploy;
        private boolean delete;
        private boolean admin;

        private UserPermissionsRow(PermissionTargetInfo permissionTarget) {
            this.permissionTarget = permissionTarget;
        }

        public PermissionTargetInfo getPermissionTarget() {
            return permissionTarget;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }

        public boolean isDeploy() {
            return deploy;
        }

        public void setDeploy(boolean deploy) {
            this.deploy = deploy;
        }

        public boolean isDelete() {
            return delete;
        }

        public void setDelete(boolean delete) {
            this.delete = delete;
        }

        public boolean isAdmin() {
            return admin;
        }

        public void setAdmin(boolean admin) {
            this.admin = admin;
        }

        public boolean hasPermissions() {
            return isRead() || isDeploy() || isDelete() || isAdmin();
        }
    }

    private class LinkPanel extends Panel {
        private LinkPanel(String id, IModel model) {
            super(id, model);
            UserPermissionsRow permRow = (UserPermissionsRow) model.getObject();
            final PermissionTargetInfo permissionTarget = permRow.getPermissionTarget();
            Link link = new Link("link") {
                public void onClick() {
                    setResponsePage(new AclsPage(permissionTarget));
                }
            };
            add(link);
            link.add(new Label("label", permissionTarget.getName()));
        }
    }
}