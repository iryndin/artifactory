package org.artifactory.api.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;

/**
 * Holds information about user groups.
 *
 * @author Yossi Shaul
 */
@XStreamAlias("group")
public class GroupInfo implements Info {
    private String groupName;
    private String description;

    /** indicates if this group should automatically be added to newly created users */
    private boolean newUserDefault;

    public GroupInfo() {
    }

    public GroupInfo(String groupName) {
        this.groupName = groupName;
    }

    public GroupInfo(String groupName, String description, boolean newUserDefault) {
        this.groupName = groupName;
        this.description = description;
        this.newUserDefault = newUserDefault;
    }

    /**
     * A copy constructor.
     *
     * @param groupInfo Original group info.
     */
    public GroupInfo(GroupInfo groupInfo) {
        this(groupInfo.getGroupName(), groupInfo.getDescription(), groupInfo.isNewUserDefault());
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return True if this group should automatically be added to newly created users.
     */
    public boolean isNewUserDefault() {
        return newUserDefault;
    }

    public void setNewUserDefault(boolean newUserDefault) {
        this.newUserDefault = newUserDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GroupInfo info = (GroupInfo) o;

        return !(groupName != null ? !groupName.equals(info.groupName) : info.groupName != null);

    }

    @Override
    public int hashCode() {
        return (groupName != null ? groupName.hashCode() : 0);
    }

    @Override
    public String toString() {
        return (groupName != null ? groupName : "Group name not set");
    }

}
