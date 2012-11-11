/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.apache.jackrabbit.ocm.manager.beanconverter.impl.DefaultBeanConverterImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.DefaultCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.api.security.SecurityService;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.ocm.OcmStorable;
import org.artifactory.security.jcr.JcrAclManager;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.UnloadedSidException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yoav Landman
 */
@Node(extend = OcmStorable.class)
public class Acl implements MutableAcl, OcmStorable {
    @Collection(collectionClassName = ArrayList.class, elementClassName = Ace.class,
            collectionConverter = DefaultCollectionConverterImpl.class)
    private List<Ace> aces = new ArrayList<Ace>();

    @Field
    private String updatedBy;

    @Bean(converter = DefaultBeanConverterImpl.class)
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
            sid = new PrincipalSid(SecurityService.USER_SYSTEM);
        }
        this.updatedBy = sid.getPrincipal();
    }

    public AclInfo getDescriptor() {
        Set<AceInfo> acesDescriptors = new HashSet<AceInfo>(aces.size());
        for (Ace ace : aces) {
            acesDescriptors.add(ace.getDescriptor());
        }
        return InfoFactoryHolder.get().createAcl(permissionTarget.getDescriptor(), acesDescriptors, updatedBy);
    }

    //Cannot be used as an ocm field since returns Serializable not String

    @Override
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

    @Override
    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    public void insertAce(int atIndexLocation, Permission permission, Sid sid, boolean granting)
            throws NotFoundException {
        throw new UnsupportedOperationException("insertAce from Spring security interface");
    }

    /**
     * Remove prinicpal entries from aces if found.
     *
     * @param sid the principal id that need to be removed from permissions list
     * @return null If this ACL does not have any ACE with this principal, the new ACL without entry for the principal
     */
    public Acl removePrincipalAces(ArtifactorySid sid) {
        Ace toFind = new Ace(this, BasePermission.READ, sid);
        //There's a single ace for each principal on the acl
        if (aces.contains(toFind)) {
            List<Ace> newAces = new ArrayList<Ace>(aces);
            newAces.remove(toFind);
            Acl newAcl = new Acl(getDescriptor());
            newAcl.setAces(newAces);
            return newAcl;
        }
        return null;
    }

    @Override
    public void deleteAce(int aceIndex) throws NotFoundException {
        throw new UnsupportedOperationException("Delete ACE from index");
    }

    @Override
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

    public void add(Ace ace) {
        aces.add(ace);
    }

    @Override
    public org.springframework.security.acls.model.Acl getParentAcl() {
        return null;
    }

    public org.springframework.security.acls.model.Acl getParent() {
        //Satisfy ocm
        return null;
    }

    @Override
    public void setParent(org.springframework.security.acls.model.Acl newParent) {
        throw new UnsupportedClassVersionError("RepoPath acls cannot have a parent.");
    }

    @Override
    public PermissionTarget getObjectIdentity() {
        return getPermissionTarget();
    }

    @Override
    public Sid getOwner() {
        return new PrincipalSid(updatedBy);
    }

    @Override
    public void setOwner(Sid owner) {
        this.updatedBy = ((PrincipalSid) owner).getPrincipal();
    }

    @Override
    public boolean isEntriesInheriting() {
        return false;
    }

    @Override
    public void setEntriesInheriting(boolean entriesInheriting) {
        throw new UnsupportedClassVersionError("RepoPath acl entries are not inheriting.");
    }

    @Override
    public boolean isGranted(List<Permission> permission, List<Sid> sids, boolean administrativeMode)
            throws NotFoundException, UnloadedSidException {
        return isGranted(permission.toArray(new Permission[permission.size()]),
                sids.toArray(new Sid[sids.size()]), administrativeMode);
    }

    //TODO: [by YS] remove array based methods

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

    @Override
    public List<AccessControlEntry> getEntries() {
        throw new UnsupportedOperationException("This method is not supported");
    }

    @Override
    public boolean isSidLoaded(List<Sid> sids) {
        return isSidLoaded(sids.toArray(new Sid[sids.size()]));
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

    @Override
    public String getJcrPath() {
        return JcrAclManager.getAclsJcrPath() + "/" + permissionTarget.getJcrName();
    }

    @Override
    public void setJcrPath(String path) {
        //noop
    }

    private void addAce(Ace ace) {
        aces.add(ace);
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
}