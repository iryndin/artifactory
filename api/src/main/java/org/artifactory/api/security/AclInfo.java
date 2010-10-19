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
import org.artifactory.common.Info;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Yoav Landman
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
        this(new PermissionTargetInfo(copy.permissionTarget), new HashSet<AceInfo>(), copy.updatedBy);
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