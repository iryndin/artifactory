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

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.ParentBeanConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.log4j.Logger;
import org.artifactory.api.security.AceInfo;
import org.springframework.security.acls.AccessControlEntry;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.sid.Sid;

import java.io.Serializable;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
@Node
public class Ace implements AccessControlEntry {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Ace.class);

    static {
        //Force eager loading of this class to initialize default permissions registration.
        //Required to overcome a bug is ss-2.0.3
        //noinspection UnusedDeclaration
        Permission dummy = BasePermission.READ;
    }

    @Bean(converter = ParentBeanConverterImpl.class)
    private Acl parentAcl;
    @Field
    private int mask;
    @Field
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
        this.permission = BasePermission.buildFromMask(mask);
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

    /**
     * Since we use accumulative permissions we only need to compare by owning acl and principal
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Ace ace = (Ace) o;
        return !(parentAcl != null ? !parentAcl.equals(ace.parentAcl) : ace.parentAcl != null) &&
                !(principal != null ? !principal.equals(ace.principal) : ace.principal != null);

    }

    @Override
    public int hashCode() {
        int result;
        result = (parentAcl != null ? parentAcl.hashCode() : 0);
        result = 31 * result + mask;
        result = 31 * result + (principal != null ? principal.hashCode() : 0);
        result = 31 * result + (permission != null ? permission.hashCode() : 0);
        return result;
    }
}