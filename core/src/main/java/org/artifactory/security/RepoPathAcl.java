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

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.NTCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.jackrabbit.util.Text;
import org.apache.log4j.Logger;
import org.artifactory.jcr.ocm.OcmStorable;
import org.springframework.security.Authentication;
import org.springframework.security.acls.Acl;
import org.springframework.security.acls.MutableAcl;
import org.springframework.security.acls.NotFoundException;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.UnloadedSidException;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;
import org.springframework.security.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Node(extend = OcmStorable.class)
public class RepoPathAcl implements MutableAcl, OcmStorable {

    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RepoPathAcl.class);

    @Collection(elementClassName = RepoPathAce.class,
            collectionConverter = NTCollectionConverterImpl.class)
    private List<RepoPathAce> aces = new ArrayList<RepoPathAce>();

    @Field
    private String identifier;
    @Field
    private String updatedBy;

    private transient SecuredRepoPath repoPath;

    public RepoPathAcl() {
        //For use by ocm
    }

    public RepoPathAcl(SecuredRepoPath repoPath) {
        this.identifier = Text.escape(repoPath.getIdentifier());
        this.repoPath = repoPath;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        PrincipalSid sid;
        if (auth != null) {
            sid = new PrincipalSid(auth);
        } else {
            sid = new PrincipalSid(ArtifactorySecurityManager.USER_UNKNOWN);
        }
        this.updatedBy = sid.getPrincipal();
    }

    //Cannot be used as an ocm field since returns Seriliazale not String
    public String getId() {
        return identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        repoPath = new SecuredRepoPath(Text.unescape(identifier));
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
            RepoPathAce repoPathAce = new RepoPathAce(this, permission, (PrincipalSid) sid);
            addAce(0, repoPathAce);
        }
    }

    public void updateOrCreateAce(RepoPathAce ace) {
        synchronized (aces) {
            for (int i = 0; i < aces.size(); i++) {
                RepoPathAce repoPathAce = aces.get(i);
                if (repoPathAce.equals(ace)) {
                    aces.set(i, ace);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "No existing ace for principal '" + ace.getPrincipal() +
                                "'. Creating one.");
            }
            addAce(0, ace);
        }
    }

    public void removePrincipalAces(PrincipalSid sid) {
        String principal = sid.getPrincipal();
        synchronized (aces) {
            for (int i = 0; i < aces.size(); i++) {
                RepoPathAce ace = aces.get(i);
                if (principal.equals(ace.getPrincipal())) {
                    deleteAce(i);
                }
            }
        }
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
            RepoPathAce ace = aces.get(aceIndex);
            ace.setMask(permission.getMask());
        }
    }

    public List<RepoPathAce> getAces() {
        return aces;
    }

    public void setAces(List<RepoPathAce> aces) {
        if (aces != null) {
            this.aces = aces;
        } else {
            //Ocm passes null instead of an empty list
            this.aces = new ArrayList<RepoPathAce>();
        }
    }

    public Acl getParentAcl() {
        return null;
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    public Acl getParent() {
        //Satisfy ocm
        return null;
    }

    public void setParent(Acl newParent) {
        throw new UnsupportedClassVersionError("RepoPath acls cannot have a parent.");
    }

    public SecuredRepoPath getObjectIdentity() {
        if (repoPath == null) {
            //Overcome the fact that xstream sets fields directly
            setIdentifier(identifier);
        }
        return repoPath;
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
        for (RepoPathAce ace : aces) {
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

    public RepoPathAce[] getEntries() {
        return aces.toArray(new RepoPathAce[aces.size()]);
    }

    public boolean isSidLoaded(Sid[] sids) {
        return false;
    }

    public String getJcrPath() {
        return JcrAclService.getAclsJcrPath() + "/" + getId();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setJcrPath(String path) {
        //noop
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepoPathAcl acl = (RepoPathAcl) o;
        //Very important to use getters for properties of the other acl which might be an ocm
        //cglib-enhanced proxy
        return !(identifier != null ? !identifier.equals(acl.getIdentifier()) :
                acl.getIdentifier() != null);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public int hashCode() {
        int result = (identifier != null ? identifier.hashCode() : 0);
        return result;
    }

    private void addAce(int atIndexLocation, RepoPathAce ace) {
        synchronized (aces) {
            aces.add(atIndexLocation, ace);
        }
    }
}