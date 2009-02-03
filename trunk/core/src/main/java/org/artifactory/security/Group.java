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
package org.artifactory.security;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.jcr.ocm.OcmStorable;

import java.io.Serializable;

@Node(extend = OcmStorable.class)
public class Group implements OcmStorable, Serializable {

    private final GroupInfo info;

    public Group() {
        info = new GroupInfo();
    }

    public Group(String groupName) {
        info = new GroupInfo(groupName);
    }

    public Group(GroupInfo groupInfo) {
        info = new GroupInfo(groupInfo);
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

    public GroupInfo getInfo() {
        return info;
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
                '}';
    }

}