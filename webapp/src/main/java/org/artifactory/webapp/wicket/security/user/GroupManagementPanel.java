package org.artifactory.webapp.wicket.security.user;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.component.panel.fieldset.FieldSetPanel;

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

    public GroupManagementPanel(String id, final UsersListPanel usersListPanel) {
        super(id);

        Form form = new Form("groupManagementForm");
        add(form);

        final List<GroupInfo> groupInfos = userGroupService.getAllGroups();
        // Drop-down choice of groups to add/remove users to/from
        final DropDownChoice groupDdc =
                new UsersListPanel.TargetGroupDropDownChoice("groupManagement",
                        new PropertyModel(this, "selectedGroup"), groupInfos);
        form.add(groupDdc);

        form.add(new SimpleButton("addToGroup", form, "Add to") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                List<String> selectedUsernames = usersListPanel.getSelectedUsernames();
                if (selectedGroup != null && !selectedUsernames.isEmpty()) {
                    userGroupService.addUsersToGroup(
                            selectedGroup, selectedUsernames);
                    info("Successfully added group '" + selectedGroup +
                            "' to selected users.");
                    // refresh the users table
                    usersListPanel.refreshUsersList(target);
                    usersListPanel.displayCreatePanel(target);
                    FeedbackUtils.refreshFeedback(target);
                }
            }
        });

        form.add(new SimpleButton("removeFromGroup", form, "Remove from") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                List<String> selectedUsernames = usersListPanel.getSelectedUsernames();
                if (selectedGroup != null && !selectedUsernames.isEmpty()) {
                    userGroupService.removeUsersFromGroup(
                            selectedGroup, selectedUsernames);
                    info("Successfully removed group '" + selectedGroup + "' from selected users.");
                    // refresh the users table
                    usersListPanel.refreshUsersList(target);
                    usersListPanel.displayCreatePanel(target);
                    FeedbackUtils.refreshFeedback(target);
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return "Add/remove user(s) from group";
    }

}