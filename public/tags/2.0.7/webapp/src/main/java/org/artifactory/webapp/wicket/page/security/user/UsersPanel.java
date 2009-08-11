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
package org.artifactory.webapp.wicket.page.security.user;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;

import java.util.List;

/**
 * @author Yoav Aharoni
 * @author Yossi Shaul
 */
public class UsersPanel extends TitledPanel {

    @SpringBean
    private UserGroupService userGroupService;

    private UsersTable usersTable;

    public UsersPanel(String id) {
        super(id);

        UsersFilterPanel usersFilterPanel = new UsersFilterPanel("usersFilterPanel", this);
        add(usersFilterPanel);

        add(new GroupManagementPanel("groupManagementPanel", this));

        usersTable = new UsersTable("users",
                new UsersTableDataProvider(usersFilterPanel, userGroupService));
        add(usersTable);
    }

    public List<String> getSelectedUsernames() {
        return usersTable.getSelectedUsernames();
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

    void refreshUsersList(AjaxRequestTarget target) {
        usersTable.refreshUsersList(target);
    }
}
