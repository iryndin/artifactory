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

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.InlineBeanConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.jcr.ocm.OcmStorable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.acls.MutableAcl;
import org.springframework.security.acls.NotFoundException;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.UnloadedSidException;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;
import org.springframework.security.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Node(extend = OcmStorable.class)
public class Acl implements MutableAcl, OcmStorable {
    private static final Logger log = LoggerFactory.getLogger(Acl.class);

    @Collection(elementClassName = Ace.class,
            collectionConverter = NTCollectionConverterImpl.class)
    private List<Ace> aces = new ArrayList<Ace>();

    @Field
    private String updatedBy;

    @Bean(converter = InlineBeanConverterImpl.class)
    private PermissionTarget permissionTarget;


    public Acl() {
        //For use by ocm
    }

    public Acl(AclInfo descriptor) {
        this.permissionTarget = new PermissionTarget(descriptor.getPermissionTarget());
        this.updatedBy = descriptor.getUpdatedBy();
        Set<AceInfo> aces = descriptor.getAces();
        for (AceInfo aceDescriptor : aces) {
            addAce(new Ace(this, aceDescriptor));
        }
    }

    public Acl(PermissionTarget permissionTarget) {
        this.permissionTarget = permissionTarget;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        PrincipalSid sid;
        if (auth != null) {
            sid = new PrincipalSid(auth);
        } else {
            sid = new PrincipalSid(SecurityServiceInternal.USER_UNKNOWN);
        }
        this.updatedBy = sid.getPrincipal();
    }

    public AclInfo getDescriptor() {
        Set<AceInfo> acesDescriptors = new HashSet<AceInfo>(aces.size());
        for (Ace ace : aces) {
            acesDescriptors.add(ace.getDescriptor());
        }
        return new AclInfo(permissionTarget.getDescriptor(), acesDescriptors, updatedBy);
    }

    //Cannot be used as an ocm field since returns Serializable not String
    public String getId() {
        return permissionTarget.getName();
    }

    public PermissionTarget getPermissionTarget() {
        return permissionTarget;
    }

    public void setPermissionTarget(PermissionTarget permissionTarget) {
        this.permissionTarget = permissionTarget;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    public void insertAce(int atIndexLocation, Permission permission, Sid sid, boolean granting)
            throws NotFoundException {
        synchronized (aces) {
            Ace ace = new Ace(this, permission, (ArtifactorySid) sid);
            addAce(ace);
        }
    }

    public void updateOrCreateAce(Ace ace) {
        synchronized (aces) {
            for (int i = 0; i < aces.size(); i++) {
                Ace oldAce = aces.get(i);
                if (oldAce.equals(ace)) {
                    aces.set(i, ace);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(
                        "No existing ace for principal '" + ace.getPrincipal() +
                                "'. Creating one.");
            }
            addAce(ace);
        }
    }

    public boolean removePrincipalAces(PrincipalSid sid) {
        String principal = sid.getPrincipal();
        synchronized (aces) {
            for (int i = 0; i < aces.size(); i++) {
                Ace ace = aces.get(i);
                if (principal.equals(ace.getPrincipal())) {
                    deleteAce(i);
                    //There's a single ace for each principal on the acl
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    public void deleteAce(int aceIndex) throws NotFoundException {
        synchronized (aces) {
            this.aces.remove(aceIndex);
        }
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    public void updateAce(int aceIndex, Permission permission) throws NotFoundException {
        synchronized (aces) {
            Ace ace = aces.get(aceIndex);
            ace.setMask(permission.getMask());
        }
    }

    public List<Ace> getAces() {
        return aces;
    }

    public void setAces(List<Ace> aces) {
        if (aces != null) {
            this.aces = aces;
        } else {
            //Ocm passes null instead of an empty list
            this.aces = new ArrayList<Ace>();
        }
    }

    public org.springframework.security.acls.Acl getParentAcl() {
        return null;
    }

    public org.springframework.security.acls.Acl getParent() {
        //Satisfy ocm
        return null;
    }

    public void setParent(org.springframework.security.acls.Acl newParent) {
        throw new UnsupportedClassVersionError("RepoPath acls cannot have a parent.");
    }

    public PermissionTarget getObjectIdentity() {
        return getPermissionTarget();
    }

    public Sid getOwner() {
        return new PrincipalSid(updatedBy);
    }

    public void setOwner(Sid owner) {
        this.updatedBy = ((PrincipalSid) owner).getPrincipal();
    }

    public boolean isEntriesInheriting() {
        return false;
    }

    public void setEntriesInheriting(boolean entriesInheriting) {
        throw new UnsupportedClassVersionError("RepoPath acl entries are not inheriting.");
    }

    public boolean isGranted(Permission[] permissions, Sid[] sids, boolean administrativeMode)
            throws NotFoundException, UnloadedSidException {
        for (Ace ace : aces) {
            //Check that we match the sids
            boolean sidMatches = ace.matchSids(sids);
            if (sidMatches) {
                for (Permission permission : permissions) {
                    if ((ace.getMask() & permission.getMask()) > 0) {
                        //Any of the permissions is enough for granting
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Ace[] getEntries() {
        return aces.toArray(new Ace[aces.size()]);
    }

    public boolean isSidLoaded(Sid[] sids) {
        for (Ace ace : aces) {
            for (Sid sid : sids) {
                if (ace.getSid().equals(sid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getJcrPath() {
        return JcrAclManager.getAclsJcrPath() + "/" + permissionTarget.getJcrName();
    }

    public void setJcrPath(String path) {
        //noop
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Acl acl = (Acl) o;
        //Use only getters for properties of the other acl which might be an ocm cglib proxy
        return permissionTarget.equals(acl.getPermissionTarget());
    }

    @Override
    public int hashCode() {
        return permissionTarget.hashCode();
    }

    @Override
    public String toString() {
        return permissionTarget.toString();
    }

    private void addAce(Ace ace) {
        synchronized (aces) {
            aces.add(ace);
        }
    }
}