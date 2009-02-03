package org.artifactory.webapp.wicket.page.security.acl;

import org.artifactory.api.security.*;

import java.util.*;

/**
 * @author Yossi Shaul
 */
public class PermissionTargetAceInfoRowDataProvider extends BaseSortableAceInfoRowDataProvider {
    private AclInfo aclInfo;
    private UserGroupService userGroupService;

    public PermissionTargetAceInfoRowDataProvider(UserGroupService userGroupService,
                                                  AclInfo aclInfo) {
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

    private void addAceRow(List<AceInfoRow> rows,
                           Map<AceInfo, AceInfo> aces, String principal, boolean group) {
        AceInfo aceInfo = new AceInfo(principal, group, 0);
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