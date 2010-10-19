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

package org.artifactory.security;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.AlreadyExistsException;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.artifactory.api.security.PermissionTargetInfo.*;

/**
 * @author Yoav Landman
 */
@Repository
@Reloadable(beanClass = InternalAclManager.class, initAfter = {JcrService.class, InternalCacheService.class})
public class JcrAclManager implements InternalAclManager {
    private static final Logger log = LoggerFactory.getLogger(JcrAclManager.class);

    static final String ACLS_KEY = "acls";

    @Autowired
    private JcrService jcr;

    /**
     * All the ACL per permission target name
     */
    private Cache<String, Acl> acls;

    public static String getAclsJcrPath() {
        return JcrPath.get().getConfigJcrPath(ACLS_KEY);
    }

    public void init() {
        //Init the cache
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        acls = cacheService.getCache(ArtifactoryCache.acl);
        //Create the storage folders
        Node confNode = jcr.getOrCreateUnstructuredNode(JcrPath.get().getConfigJcrRootPath());
        jcr.getOrCreateUnstructuredNode(confNode, ACLS_KEY);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        acls.clear();
    }

    public void destroy() {
        // Nothing for the moment
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    /**
     * Custom acl creation for permission target with no inheritence and no object ownerships
     *
     * @param aclInfo the user or group this ACL apply to
     * @return the new ACL created
     * @throws AlreadyExistsException
     */
    public Acl createAcl(AclInfo aclInfo) throws AlreadyExistsException {
        Acl acl = new Acl(aclInfo);
        return createAcl(acl);
    }

    public Acl createAcl(Acl acl) {
        ObjectContentManager ocm = jcr.getOcm();
        String aclPath = acl.getJcrPath();
        Acl existingAcl = (Acl) ocm.getObject(aclPath);
        //Check this object identity hasn't already been persisted
        if (existingAcl != null) {
            throw new AlreadyExistsException("Acls '" + aclPath + "' already exists");
        }
        ocm.insert(acl);
        updateCache(acl);
        return acl;
    }

    public void deleteAcl(PermissionTarget objectIdentity) {
        Acl acl = toAcl(objectIdentity);
        ObjectContentManager ocm = jcr.getOcm();
        ocm.remove(acl);
        //Clear the cache
        removeFormCache(acl);
    }

    public Acl updateAcl(Acl acl) throws NotFoundException {
        //Delete and recreate the ACL
        ObjectContentManager ocm = jcr.getOcm();
        ocm.remove(acl.getJcrPath());
        ocm.insert(acl);
        updateCache(acl);
        return acl;
    }

    public Acl findAclById(PermissionTarget permissionTarget) throws NotFoundException {
        return findAclById(permissionTarget.getName());
    }

    private Acl findAclById(String permissionTargetKey) throws NotFoundException {
        Acl result = acls.get(permissionTargetKey);
        if (result == null) {
            log.debug("Did not find ACL for PermissionTarget " + permissionTargetKey);
        }
        return result;
    }

    public void createDefaultSecurityEntities(SimpleUser anonUser, Group readersGroup) {
        if (!UserInfo.ANONYMOUS.equals(anonUser.getUsername())) {
            throw new IllegalArgumentException(
                    "Default anything permissions should be created for the anonymous user only");
        }

        ArtifactorySid anonSid = anonUser.toArtifactorySid();
        ArtifactorySid readersSid = new ArtifactorySid(readersGroup.getGroupName(), true);

        // create or update raed permissions on "anything"
        Acl anyAnyAcl = findAclById(ANY_PERMISSION_TARGET_NAME);
        Ace anonAnyAnyAce = new Ace(anyAnyAcl, BasePermission.READ, anonSid);
        Ace readersAnyAnyAce = new Ace(anyAnyAcl, BasePermission.READ, readersSid);
        if (anyAnyAcl == null) {
            PermissionTarget anyAnyTarget =
                    new PermissionTarget(ANY_PERMISSION_TARGET_NAME, ANY_REPO, ANY_PATH, null);
            anyAnyAcl = toAcl(anyAnyTarget);
            anyAnyAcl.add(anonAnyAnyAce);
            anyAnyAcl.add(readersAnyAnyAce);
            createAcl(anyAnyAcl);
        } else {
            anyAnyAcl.add(anonAnyAnyAce);
            anyAnyAcl.add(readersAnyAnyAce);
            updateAcl(anyAnyAcl);
        }

        // create or update read and deploy permissions on all remote repos
        Acl anyRemoteAcl = findAclById(ANY_REMOTE_PERMISSION_TARGET_NAME);
        Ace anonAnyRemoteAce = new Ace(anyRemoteAcl, BasePermission.READ, anonSid);
        anonAnyRemoteAce.setMask(BasePermission.READ.getMask() | BasePermission.WRITE.getMask());
        if (anyRemoteAcl == null) {
            PermissionTarget anyRemoteTarget =
                    new PermissionTarget(ANY_REMOTE_PERMISSION_TARGET_NAME, ANY_REMOTE_REPO, ANY_PATH, null);
            anyRemoteAcl = toAcl(anyRemoteTarget);
            anyRemoteAcl.add(anonAnyRemoteAce);
            createAcl(anyRemoteAcl);
        } else {
            anyRemoteAcl.add(anonAnyRemoteAce);
            updateAcl(anyRemoteAcl);
        }
    }

    public List<PermissionTarget> getAllPermissionTargets() {
        Collection<Acl> acls = getAllAclsFromCache();
        List<PermissionTarget> repoPaths = new ArrayList<PermissionTarget>(acls.size());
        for (Acl acl : acls) {
            repoPaths.add(acl.getPermissionTarget());
        }
        return repoPaths;
    }

    public List<Acl> getAllAcls() {
        return new ArrayList<Acl>(getAllAclsFromCache());
    }

    public List<Acl> getAllAcls(ArtifactorySid[] sids) {
        Collection<Acl> acls = getAllAclsFromCache();
        List<Acl> aclsForSids = new ArrayList<Acl>();
        for (Acl acl : acls) {
            // check if one of the Acl's Aces matches one of the Sids
            for (Ace ace : acl.getAces()) {
                if (ace.matchSids(sids)) {
                    aclsForSids.add(acl);
                    break;
                }
            }
        }
        return aclsForSids;
    }

    /**
     * Reload all configured ACLs within a new transction
     */
    public void reloadAcls() {
        InternalAclManager internalAclManager = InternalContextHelper.get().beanForType(InternalAclManager.class);
        internalAclManager.reloadAndReturnAcls();
    }

    /**
     * Reload and return all configured ACLs
     *
     * @return List of reloaded ACLs
     */
    @SuppressWarnings({"unchecked"})
    public Collection<Acl> reloadAndReturnAcls() {
        ObjectContentManager ocm = jcr.getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(Acl.class);
        filter.setScope(getAclsJcrPath() + "/");
        Query query = queryManager.createQuery(filter);
        Collection<Acl> aclFromDbs = ocm.getObjects(query);
        acls.clear();
        for (Acl acl : aclFromDbs) {
            updateCache(acl);
        }
        return aclFromDbs;
    }

    public void removeAllUserAces(String username) {
        ArtifactorySid sid = new ArtifactorySid(username);
        //Work on a copy of the list to avoid CME
        Collection<Acl> acls = getAllAcls();
        for (Acl acl : acls) {
            Acl newAcl = acl.removePrincipalAces(sid);
            if (newAcl != null) {
                updateAcl(newAcl);
            }
        }
    }

    public void deleteAllAcls() {
        acls.clear();
        jcr.delete(getAclsJcrPath());
        jcr.getOrCreateUnstructuredNode(getAclsJcrPath());
    }

    private static Acl toAcl(PermissionTarget permissionTarget) {
        Assert.notNull(permissionTarget, "Non null permission target required");
        return new Acl(permissionTarget);
    }

    @SuppressWarnings({"unchecked"})
    private Collection<Acl> getAllAclsFromCache() {
        if (acls.isEmpty()) {
            // TODO: If no ACLs in DB reloading all the time !
            return reloadAndReturnAcls();
        }
        return acls.values();
    }

    private void updateCache(Acl acl) {
        acls.put(acl.getPermissionTarget().getName(), acl);
    }

    private void removeFormCache(Acl acl) {
        acls.remove(acl.getPermissionTarget().getName());
    }
}