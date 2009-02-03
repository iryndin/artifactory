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

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@XStreamAlias("acl")
public class AclInfo implements Info {

    private PermissionTargetInfo permissionTarget;
    // TODO: verify it's a clean HashSet implemention on all sets
    private Set<AceInfo> aces;
    private String updatedBy;

    public AclInfo() {
        this.permissionTarget = new PermissionTargetInfo();
        this.aces = new HashSet<AceInfo>();
    }

    public AclInfo(AclInfo copy) {
        this(new PermissionTargetInfo(copy.permissionTarget), new HashSet<AceInfo>(),
                copy.updatedBy);
        for (AceInfo aceInfo : copy.getAces()) {
            aces.add(new AceInfo(aceInfo));
        }
    }

    public AclInfo(PermissionTargetInfo permissionTarget) {
        this.permissionTarget = permissionTarget;
        this.aces = new HashSet<AceInfo>();
    }

    public AclInfo(PermissionTargetInfo permissionTarget, Set<AceInfo> aces,
            String updatedBy) {
        this.permissionTarget = permissionTarget;
        this.aces = aces;
        this.updatedBy = updatedBy;
    }

    public PermissionTargetInfo getPermissionTarget() {
        return permissionTarget;
    }

    public void setPermissionTarget(PermissionTargetInfo permissionTarget) {
        this.permissionTarget = permissionTarget;
    }

    public Set<AceInfo> getAces() {
        return aces;
    }

    public void setAces(Set<AceInfo> aces) {
        this.aces = aces;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AclInfo info = (AclInfo) o;

        return !(permissionTarget != null ? !permissionTarget.equals(info.permissionTarget) :
                info.permissionTarget != null);
    }

    @Override
    public int hashCode() {
        return (permissionTarget != null ? permissionTarget.hashCode() : 0);
    }
}