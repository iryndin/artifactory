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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;

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
