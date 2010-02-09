/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.security.acl;

import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yossi Shaul
 */
public class PermissionTargetAceInfoRowDataProvider extends BaseSortableAceInfoRowDataProvider {
    private AclInfo aclInfo;
    private UserGroupService userGroupService;

    public PermissionTargetAceInfoRowDataProvider(UserGroupService userGroupService, AclInfo aclInfo) {
        this.userGroupService = userGroupService;
        this.aclInfo = aclInfo;
        loadData();
    }

    @Override
    public void loadData() {
        //Restore the roles
        Set<AceInfo> aceInfos = aclInfo.getAces();
        Map<AceInfo, AceInfo> acesMap = new HashMap<AceInfo, AceInfo>(aceInfos.size());
        for (AceInfo aceInfo : aceInfos) {
            acesMap.put(aceInfo, aceInfo);
        }
        //List of recipients except for admins that are filtered out from acl management
        List<UserInfo> users = getUsers();
        List<GroupInfo> groups = getGroups();
        //Create a list of acls for *all* users and groups
        //Stored acls are only the non empty ones
        List<AceInfoRow> rows = new ArrayList<AceInfoRow>(users.size());
        for (UserInfo user : users) {
            addAceRow(rows, acesMap, user.getUsername(), false);
        }
        for (GroupInfo group : groups) {
            addAceRow(rows, acesMap, group.getGroupName(), true);
        }
        this.aces = rows;
    }

    protected List<GroupInfo> getGroups() {
        return userGroupService.getAllGroups();
    }

    protected List<UserInfo> getUsers() {
        return userGroupService.getAllUsers(false);
    }

    private void addAceRow(List<AceInfoRow> rows, Map<AceInfo, AceInfo> aces, String username, boolean group) {
        AceInfo aceInfo = new AceInfo(username, group, 0);
        AceInfo existingAceInfo = aces.get(aceInfo);
        if (existingAceInfo == null) {
            aclInfo.getAces().add(aceInfo);
        } else {
            aceInfo = existingAceInfo;
        }
        AceInfoRow row = new AceInfoRow(aceInfo);
        rows.add(row);
    }

    public void setAclInfo(AclInfo aclInfo) {
        this.aclInfo = aclInfo;
    }
}