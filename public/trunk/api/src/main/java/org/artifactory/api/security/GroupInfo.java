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

package org.artifactory.api.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
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

    /**
     * indicates if this group should automatically be added to newly created users
     */
    private boolean newUserDefault;

    /**
     * indicates if this group is external (e.g LDAP)
     */
    @XStreamOmitField
    private boolean external;

    private String realm;

    private String realmAttributes;

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

    public GroupInfo(String groupName, String description, boolean newUserDefault, String realm,
            String realmAttributes) {
        this.groupName = groupName;
        this.description = description;
        this.newUserDefault = newUserDefault;
        this.realm = realm;
        this.realmAttributes = realmAttributes;
    }

    /**
     * A copy constructor.
     *
     * @param groupInfo Original group info.
     */
    public GroupInfo(GroupInfo groupInfo) {
        this(groupInfo.getGroupName(), groupInfo.getDescription(), groupInfo.isNewUserDefault(), groupInfo.getRealm(),
                groupInfo.getRealmAttributes());
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

    public boolean isExternal() {
        return realm != null && !SecurityService.DEFAULT_REALM.equals(realm);
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealmAttributes() {
        return realmAttributes;
    }

    public void setRealmAttributes(String realmAttributes) {
        this.realmAttributes = realmAttributes;
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
