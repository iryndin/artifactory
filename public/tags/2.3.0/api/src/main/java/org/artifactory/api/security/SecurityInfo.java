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
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.artifactory.common.Info;

import java.util.List;

/**
 * User: freds Date: Jun 12, 2008 Time: 1:57:29 PM
 */
@XStreamAlias("security")
public class SecurityInfo implements Info {
    @XStreamAsAttribute
    private String version;
    private List<UserInfo> users;
    private List<GroupInfo> groups;
    private List<AclInfo> acls;

    public SecurityInfo() {
    }

    public SecurityInfo(List<UserInfo> users, List<GroupInfo> groups, List<AclInfo> acls) {
        this.users = users;
        this.groups = groups;
        this.acls = acls;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<UserInfo> getUsers() {
        return users;
    }

    public void setUsers(List<UserInfo> users) {
        this.users = users;
    }

    public List<GroupInfo> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupInfo> groups) {
        this.groups = groups;
    }

    public List<AclInfo> getAcls() {
        return acls;
    }

    public void setAcls(List<AclInfo> acls) {
        this.acls = acls;
    }
}