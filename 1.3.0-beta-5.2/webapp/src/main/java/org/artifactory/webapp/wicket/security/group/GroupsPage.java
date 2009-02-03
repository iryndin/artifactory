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

import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.webapp.wicket.AuthenticatedPage;
import org.artifactory.webapp.wicket.component.CreateUpdatePanel;

/**
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class GroupsPage extends AuthenticatedPage {
    private GroupsListPanel groupsListPanel;

    public GroupsPage() {
        groupsListPanel = new GroupsListPanel("groupsList");
        add(groupsListPanel);
        add(newCreatePanel());
    }

    public GroupCreateUpdatePanel newCreatePanel() {
        return new GroupCreateUpdatePanel(
                CreateUpdatePanel.CreateUpdateAction.CREATE, new GroupInfo(), groupsListPanel);
    }

    public GroupCreateUpdatePanel newUpdatePanel(GroupInfo group) {
        return new GroupCreateUpdatePanel(
                CreateUpdatePanel.CreateUpdateAction.UPDATE, group, groupsListPanel);
    }

    @Override
    protected String getPageName() {
        return "Groups Management";
    }
}