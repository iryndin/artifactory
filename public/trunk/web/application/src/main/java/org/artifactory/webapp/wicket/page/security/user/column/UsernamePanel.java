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

package org.artifactory.webapp.wicket.page.security.user.column;

import com.google.common.collect.Lists;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.LdapGroupWebAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.wicket.component.deletable.listview.DeletableLabelGroup;
import org.artifactory.webapp.wicket.page.security.user.UserModel;

import java.util.Set;

/**
 * @author Yoav Aharoni
 */
public class UsernamePanel extends Panel {

    @SpringBean
    private UserGroupService userGroupService;

    public UsernamePanel(String id, IModel model) {
        super(id);
        add(new SimpleAttributeModifier("class", "UserColumn"));
        final UserModel userModel = (UserModel) model.getObject();
        final String username = userModel.getUsername();
        add(new Label("username", username));

        Set<UserInfo.UserGroupInfo> userGroups = userModel.getGroups();
        LdapGroupWebAddon groupAddon =
                ContextHelper.get().beanForType(AddonsManager.class).addonByType(LdapGroupWebAddon.class);
        groupAddon.addExternalGroups(username, userGroups);

        DeletableLabelGroup<UserInfo.UserGroupInfo> groups =
                new DeletableLabelGroup<UserInfo.UserGroupInfo>("groups", userGroups) {
                    @Override
                    public void onDelete(UserInfo.UserGroupInfo value, AjaxRequestTarget target) {
                        super.onDelete(value, target);
                        //Save the group changes on each delete
                        userGroupService.removeUsersFromGroup(value.getGroupName(), Lists.newArrayList(username));
                        userModel.removeGroup(value);
                    }
                };
        // set the final merged set of groups to the user model.
        userModel.addGroups(userGroups);
        groups.setItemsPerPage(3);
        groups.setLabelClickable(false);
        groups.setLabelDeletable(false);
        add(groups);
    }
}
