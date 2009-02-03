package org.artifactory.webapp.wicket.page.security.user;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.webapp.wicket.WicketProperty;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.panel.fieldset.FieldSetPanel;

import java.util.List;

/**
 * This panel controls the users filtering by username and/or group.
 *
 * @author Yossi Shaul
 */
public class UsersFilterPanel extends FieldSetPanel {
    @SpringBean
    private UserGroupService userGroupService;

    @WicketProperty
    private String usernameFilter;

    @WicketProperty
    private String groupFilter;

    public UsersFilterPanel(String id, final UsersPanel usersListPanel) {
        super(id);

        Form form = new Form("usersFilterForm");
        add(form);

        form.add(new TextField("usernameFilter", new PropertyModel(this, "usernameFilter")));

        form.add(new SimpleButton("filter", form, "Filter") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // refresh the users list and table
                usersListPanel.refreshUsersList(target);
            }
        });

        final List<GroupInfo> groupInfos = userGroupService.getAllGroups();

        // Drop-down choice of groups to filter by
        DropDownChoice groupDdc = new UsersPanel.FilterGroupDropDownChoice("groupFilter",
                new PropertyModel(this, "groupFilter"), groupInfos);
        form.add(groupDdc);
    }

    //@Override
    public String getTitle() {
        return "Filter users";
    }

    public String getUsernameFilter() {
        return usernameFilter;
    }

    public String getGroupFilter() {
        return groupFilter;
    }

    public void setUsernameFilter(String usernameFilter) {
        this.usernameFilter = usernameFilter;
    }

    public void setGroupFilter(String groupFilter) {
        this.groupFilter = groupFilter;
    }
}
