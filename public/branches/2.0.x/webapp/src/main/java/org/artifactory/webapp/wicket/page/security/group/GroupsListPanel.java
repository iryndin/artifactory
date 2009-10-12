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
package org.artifactory.webapp.wicket.page.security.group;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.component.BooleanColumn;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.list.ListPanel;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class GroupsListPanel extends ListPanel<GroupInfo> {

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
        columns.add(new PropertyColumn(new Model("Group name"), "groupName", "groupName") {
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

}