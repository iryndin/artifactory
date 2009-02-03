package org.artifactory.webapp.actionable.view;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.security.acl.AceInfoRow;
import org.artifactory.webapp.wicket.security.acl.BaseSortableAceInfoRowDataProvider;

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

    private void addIfHasPermissions(List<AceInfoRow> rows, AceInfo aceInfo) {
        if (aceInfo.getMask() > 0) {
            // only add users/groups who have some permission
            rows.add(new AceInfoRow(aceInfo));
        }
    }

    private AceInfo createAceInfoForUser(UserInfo userInfo) {
        AceInfo aceInfo = new AceInfo(userInfo.getUsername(), false, 0);
        aceInfo.setRead(authService.canRead(userInfo, repoPath));
        aceInfo.setDeploy(authService.canDeploy(userInfo, repoPath));
        aceInfo.setDelete(authService.canDelete(userInfo, repoPath));
        aceInfo.setAdmin(authService.canAdmin(userInfo, repoPath));
        return aceInfo;
    }

    private AceInfo createAceInfoForGroup(GroupInfo groupInfo) {
        AceInfo aceInfo = new AceInfo(groupInfo.getGroupName(), true, 0);
        aceInfo.setRead(authService.canRead(groupInfo, repoPath));
        aceInfo.setDeploy(authService.canDeploy(groupInfo, repoPath));
        aceInfo.setDelete(authService.canDelete(groupInfo, repoPath));
        aceInfo.setAdmin(authService.canAdmin(groupInfo, repoPath));
        return aceInfo;
    }
}
