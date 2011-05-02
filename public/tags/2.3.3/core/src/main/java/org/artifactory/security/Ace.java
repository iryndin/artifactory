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

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.api.security.AceInfo;
import org.artifactory.jcr.ocm.ParentCollectionBeanConverter;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

import java.io.Serializable;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 *
 * @author yoavl
 */
@Node
public class Ace implements AccessControlEntry {

    @Bean(converter = ParentCollectionBeanConverter.class)
    private Acl parentAcl;
    @Field
    private int mask;
    @Field(id = true)
    private String principal;
    @Field
    private boolean group;

    private transient Permission permission;

    public Ace() {
        //For use by ocm
    }

    public Ace(Acl acl, Permission permission, ArtifactorySid sid) {
        this.parentAcl = acl;
        this.principal = sid.getPrincipal();
        this.group = sid.isGroup();
        setMask(permission);
    }

    public Ace(Acl acl, AceInfo descriptor) {
        setParentAcl(acl);
        setMask(descriptor.getMask());
        setPrincipal(descriptor.getPrincipal());
        setGroup(descriptor.isGroup());
    }

    public AceInfo getDescriptor() {
        return new AceInfo(principal, group, mask);
    }

    //Cannot be used as an ocm field since ifc returns Acl not RepoPathAcl

    public Acl getAcl() {
        return parentAcl;
    }

    public Acl getParentAcl() {
        return parentAcl;
    }

    public void setParentAcl(Acl parentAcl) {
        this.parentAcl = parentAcl;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(Permission permission) {
        this.mask = permission.getMask();
        this.permission = permission;
    }

    /**
     * For use by ocm
     *
     * @param mask
     */
    public void setMask(int mask) {
        this.mask = mask;
        this.permission = new DefaultPermissionFactory().buildFromMask(mask);
    }

    public Permission getPermission() {
        if (permission == null) {
            //Overcome the fact that xstream sets fields directly
            setMask(mask);
        }
        return permission;
    }

    public Serializable getId() {
        //Not used
        return null;
    }

    public ArtifactorySid getSid() {
        return new ArtifactorySid(principal, group);
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public boolean isGranting() {
        return true;
    }

    public boolean matchSids(Sid... sids) {
        ArtifactorySid artifactorySid = getSid();
        for (Sid sid : sids) {
            if (artifactorySid.equals(sid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Ace ace = (Ace) o;

        return group == ace.group && parentAcl.equals(ace.parentAcl) && principal.equals(ace.principal);
    }

    @Override
    public int hashCode() {
        int result = parentAcl.hashCode();
        result = 31 * result + principal.hashCode();
        result = 31 * result + (group ? 1 : 0);
        return result;
    }
}