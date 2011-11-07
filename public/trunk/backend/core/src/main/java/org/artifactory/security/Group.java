/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.security;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.ocm.OcmStorable;
import org.artifactory.security.jcr.JcrUserGroupManager;

import java.io.Serializable;

@Node(extend = OcmStorable.class)
public class Group implements OcmStorable, Serializable {

    private final MutableGroupInfo info;

    public Group() {
        info = InfoFactoryHolder.get().createGroup();
    }

    public Group(String groupName) {
        info = InfoFactoryHolder.get().createGroup(groupName);
    }

    public Group(GroupInfo groupInfo) {
        info = InfoFactoryHolder.get().copyGroup(groupInfo);
    }

    @Field
    public String getGroupName() {
        return info.getGroupName();
    }

    public void setGroupName(String groupName) {
        info.setGroupName(groupName);
    }

    @Field
    public String getDescription() {
        return info.getDescription();
    }

    public void setDescription(String description) {
        info.setDescription(description);
    }

    @Field
    public String getRealm() {
        return info.getRealm();
    }

    public void setRealm(String realm) {
        info.setRealm(realm);
    }

    @Field
    public String getRealmAttributes() {
        return info.getRealmAttributes();
    }

    public void setRealmAttributes(String realmAttributes) {
        info.setRealmAttributes(realmAttributes);
    }

    @Field(jcrDefaultValue = "false")
    public boolean isNewUserDefault() {
        return info.isNewUserDefault();
    }

    public void setNewUserDefault(boolean newUserDefault) {
        info.setNewUserDefault(newUserDefault);
    }

    public GroupInfo getInfo() {
        return info;
    }

    public boolean isExternal() {
        return info.isExternal();
    }

    public String getJcrPath() {
        return JcrUserGroupManager.getGroupsJcrPath() + "/" + getGroupName();
    }

    public void setJcrPath(String path) {
        //noop
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Group)) {
            return false;
        }
        Group group = (Group) o;
        return getGroupName().equals(group.getGroupName());

    }

    public int hashCode() {
        return info.hashCode();
    }

    public String toString() {
        return "Group{" +
                "name='" + getGroupName() + '\'' +
                ", description=" + getDescription() +
                ", new user default=" + isNewUserDefault() +
                '}';
    }

}