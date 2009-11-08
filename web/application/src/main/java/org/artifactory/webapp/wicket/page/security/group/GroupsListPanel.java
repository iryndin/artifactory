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

package org.artifactory.webapp.wicket.page.security.group;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.modal.links.ModalShowLink;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;
import org.artifactory.common.wicket.component.table.columns.BooleanColumn;
import org.artifactory.webapp.wicket.page.security.group.permission.GroupPermissionsPanel;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class GroupsListPanel extends ModalListPanel<GroupInfo> {

    @SpringBean
    private UserGroupService userGroupService;

    List<GroupInfo> cachedGroups;

    public GroupsListPanel(String id) {
        super(id);
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    protected List<GroupInfo> getList() {
        return userGroupService.getAllGroups();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Group Name"), "groupName", "groupName") {
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                super.populateItem(item, componentId, model);
                item.add(new CssClass("group nowrap"));
            }
        });

        columns.add(new PropertyColumn(new Model("Description"), "description", "description") {
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
        columns.add(new BooleanColumn(new Model("Auto Join"), "newUserDefault", "newUserDefault"));
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new GroupCreateUpdatePanel(CreateUpdateAction.CREATE, new GroupInfo(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(GroupInfo group) {
        return new GroupCreateUpdatePanel(CreateUpdateAction.UPDATE, group, this);
    }

    @Override
    protected String getDeleteConfirmationText(GroupInfo group) {
        return "Are you sure you wish to delete the group " + group.getGroupName() + "?";
    }

    @Override
    protected void deleteItem(GroupInfo group, AjaxRequestTarget target) {
        userGroupService.deleteGroup(group.getGroupName());
    }

    protected BaseModalPanel newViewUserPermissionsPanel(GroupInfo groupInfo) {
        return new GroupPermissionsPanel(groupInfo);
    }

    @Override
    protected void addLinks(List<AbstractLink> links, final GroupInfo groupInfo, String linkId) {
        super.addLinks(links, groupInfo, linkId);
        ModalShowLink viewPermissionsLink = new ModalShowLink(linkId, "Permissions") {
            @Override
            protected BaseModalPanel getModelPanel() {
                return newViewUserPermissionsPanel(groupInfo);
            }
        };
        viewPermissionsLink.add(new CssClass("icon-link"));
        viewPermissionsLink.add(new CssClass("ViewUserPermissionsAction"));
        links.add(viewPermissionsLink);
    }
}