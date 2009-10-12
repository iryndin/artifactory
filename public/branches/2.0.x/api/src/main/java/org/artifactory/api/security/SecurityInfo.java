/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;

import java.util.List;

/**
 * User: freds Date: Jun 12, 2008 Time: 1:57:29 PM
 */
@XStreamAlias("security")
public class SecurityInfo implements Info {
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