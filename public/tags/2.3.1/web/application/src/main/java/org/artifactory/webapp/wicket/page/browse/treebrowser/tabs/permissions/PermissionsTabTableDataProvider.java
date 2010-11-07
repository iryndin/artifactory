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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.permissions;

import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.wicket.page.security.acl.AceInfoRow;
import org.artifactory.webapp.wicket.page.security.acl.BaseSortableAceInfoRowDataProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Data provider for the permissions table in the permissions tab.
 *
 * @author Yossi Shaul
 */
class PermissionsTabTableDataProvider extends BaseSortableAceInfoRowDataProvider {
    private UserGroupService userGroupService;
    private AuthorizationService authService;
    private RepoPath repoPath;

    public PermissionsTabTableDataProvider(UserGroupService userGroupService,
            AuthorizationService authService, RepoPath repoPath) {
        this.userGroupService = userGroupService;
        this.authService = authService;
        this.repoPath = repoPath;
        loadData();
    }

    @Override
    public void loadData() {

        List<UserInfo> users = userGroupService.getAllUsers(true);
        List<GroupInfo> groups = userGroupService.getAllGroups();

        //Create a list of acls for *all* users and groups
        List<AceInfoRow> rows = new ArrayList<AceInfoRow>(users.size());
        for (UserInfo user : users) {
            addAceRow(rows, user);
        }
        for (GroupInfo group : groups) {
            addAceRow(rows, group);
        }
        aces = rows;
    }

    private void addAceRow(List<AceInfoRow> rows, UserInfo userInfo) {
        AceInfo aceInfo = createAceInfoForUser(userInfo);
        addIfHasPermissions(rows, aceInfo);
    }

    private void addAceRow(List<AceInfoRow> rows, GroupInfo groupInfo) {
        AceInfo aceInfo = createAceInfoForGroup(groupInfo);
        addIfHasPermissions(rows, aceInfo);
    }

    private AceInfo createAceInfoForUser(UserInfo userInfo) {
        AceInfo aceInfo = new AceInfo(userInfo.getUsername(), false, 0);
        aceInfo.setRead(authService.canRead(userInfo, repoPath));
        aceInfo.setAnnotate(authService.canAnnotate(userInfo, repoPath));
        aceInfo.setDeploy(authService.canDeploy(userInfo, repoPath));
        aceInfo.setDelete(authService.canDelete(userInfo, repoPath));
        aceInfo.setAdmin(authService.canAdmin(userInfo, repoPath));
        return aceInfo;
    }

    private AceInfo createAceInfoForGroup(GroupInfo groupInfo) {
        AceInfo aceInfo = new AceInfo(groupInfo.getGroupName(), true, 0);
        aceInfo.setRead(authService.canRead(groupInfo, repoPath));
        aceInfo.setAnnotate(authService.canAnnotate(groupInfo, repoPath));
        aceInfo.setDeploy(authService.canDeploy(groupInfo, repoPath));
        aceInfo.setDelete(authService.canDelete(groupInfo, repoPath));
        aceInfo.setAdmin(authService.canAdmin(groupInfo, repoPath));
        return aceInfo;
    }

    private static void addIfHasPermissions(List<AceInfoRow> rows, AceInfo aceInfo) {
        if (aceInfo.getMask() > 0) {
            // only add users/groups who have some permission
            rows.add(new AceInfoRow(aceInfo));
        }
    }
}
