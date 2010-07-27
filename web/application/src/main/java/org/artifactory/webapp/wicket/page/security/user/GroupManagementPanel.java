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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.filteringselect.FilteringSelectBehavior;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.fieldset.FieldSetPanel;
import org.artifactory.common.wicket.util.AjaxUtils;

import java.util.List;

/**
 * This panel controls the users filtering by username and/or group.
 *
 * @author Yossi Shaul
 */
public class GroupManagementPanel extends FieldSetPanel {

    @SpringBean
    private UserGroupService userGroupService;

    @WicketProperty
    private String selectedGroup;

    public GroupManagementPanel(String id, final UsersPanel usersListPanel) {
        super(id);

        Form form = new Form("groupManagementForm");
        add(form);

        List<GroupInfo> groupInfos = userGroupService.getAllGroups();
        // Drop-down choice of groups to add/remove users to/from
        DropDownChoice groupDdc = new UsersPanel.TargetGroupDropDownChoice("groupManagement",
                new PropertyModel(this, "selectedGroup"), groupInfos) {
            @Override
            protected CharSequence getDefaultChoice(Object selected) {
                return "";
            }
        };
        groupDdc.add(new FilteringSelectBehavior());
        form.add(groupDdc);

        form.add(new TitledAjaxSubmitLink("addToGroup", "Add to", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                List<String> selectedUsernames = usersListPanel.getSelectedUsernames();
                if (selectedGroup != null && !selectedUsernames.isEmpty()) {
                    userGroupService.addUsersToGroup(
                            selectedGroup, selectedUsernames);
                    info("Successfully added selected users  to group '" + selectedGroup + "'.");
                    // refresh the users table
                    usersListPanel.refreshUsersList(target);
                    AjaxUtils.refreshFeedback(target);
                }
            }
        });

        form.add(new TitledAjaxSubmitLink("removeFromGroup", "Remove from", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                List<String> selectedUsernames = usersListPanel.getSelectedUsernames();
                if (selectedGroup != null && !selectedUsernames.isEmpty()) {
                    userGroupService.removeUsersFromGroup(
                            selectedGroup, selectedUsernames);
                    info("Successfully removed selected users from group'" + selectedGroup + "'.");
                    // refresh the users table
                    usersListPanel.refreshUsersList(target);
                    AjaxUtils.refreshFeedback(target);
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return "Add/Remove Selected User(s) from Group";
    }

}