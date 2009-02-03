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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.artifactory.api.security.AclInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.utils.AlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.NotFoundException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Repository
public class JcrAclManager implements AclManager {
    private final static Logger log = LoggerFactory.getLogger(JcrAclManager.class);

    static final String ACLS_KEY = "acls";
    private static final String ARTIFACTORY_CACHE_NAME = "artifactory-cache";

    @Autowired
    private JcrService jcr;

    /**
     * Cache holding one entry (key is ACLS_KEY) with all the ACLs. Access to this cache should be
     * synchronized.
     */
    private Ehcache aclsCache;

    public static String getAclsJcrPath() {
        return JcrPath.get().getOcmClassJcrPath(ACLS_KEY);
    }

    @PostConstruct
    public void register() {
        //Init the cache
        //TODO: Use Spring to read the file and convert disk store path on the fly
        // And then provide an input stream to EhCache manager
        URL url = this.getClass().getResource("/META-INF/ehcache/ehcache.xml");
        //Trick ehcache to store its temp file under artifactory's "data" folder
        String origTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", ArtifactoryHome.getDataDir().getAbsolutePath());
        try {
            CacheManager cacheManager = CacheManager.create(url);
            cacheManager.addCache(ARTIFACTORY_CACHE_NAME);
            aclsCache = cacheManager.getCache(ARTIFACTORY_CACHE_NAME);
        } finally {
            if (origTmpDir != null) {
                System.setProperty("java.io.tmpdir", origTmpDir);
            }
        }
        InternalContextHelper.get().addReloadableBean(AclManager.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class};
    }

    public void init() {
        //Create the storage folders
        Node confNode = jcr.getOrCreateUnstructuredNode(JcrPath.get().getOcmJcrRootPath());
        jcr.getOrCreateUnstructuredNode(confNode, ACLS_KEY);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Nothing for the moment
    }

    public void destroy() {
        CacheManager.getInstance().removalAll();
    }

    /**
     * Custom acl creation for permission target with no inheritence and no object ownerships
     *
     * @param objectIdentity the user or group this ACL apply to
     * @return the new ACL created
     * @throws org.artifactory.utils.AlreadyExistsException
     *
     */
    public Acl createAcl(PermissionTarget objectIdentity) throws AlreadyExistsException {
        Acl acl = toAcl(objectIdentity);
        return createAcl(acl);
    }

    public Acl createAcl(Acl acl) {
        ObjectContentManager ocm = jcr.getOcm();
        String aclPath = acl.getJcrPath();
        Acl existingAcl = (Acl) ocm.getObject(aclPath);
        //Check this object identity hasn't already been persisted
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
        deleteAcl(acl.getObjectIdentity());
        Acl newAcl = createAcl(acl);
        return newAcl;
    }

    public Acl findAclById(PermissionTarget permissionTarget) throws NotFoundException {
        List<Acl> acls = getAllAcls();
        for (Acl acl : acls) {
            if (acl.getPermissionTarget().equals(permissionTarget)) {
                return acl;
            }
        }
        return null;
    }

    public void createAnythingPermision(SimpleUser anonUser) {
        //Create the anon acl if the anonymous user was created
        PermissionTarget anyAnyTarget = PermissionTarget.ANY_PERMISSION_TARGET;
        Acl anyAnyAcl = findAclById(anyAnyTarget);
        boolean toCreate;
        if (anyAnyAcl == null) {
            anyAnyAcl = toAcl(anyAnyTarget);
            toCreate = true;
        } else {
            toCreate = false;
        }
        //Force the read since the anonymous user is there for the first time
        //After this update version if anonymous permissions changed we will not
        //reach this method
        List<Ace> aces = anyAnyAcl.getAces();
        ArtifactorySid anonSid = anonUser.toArtifactorySid();
        Ace anonAnyAnyAce =
                new Ace(anyAnyAcl, BasePermission.READ, anonSid);
        if (!aces.contains(anonAnyAnyAce)) {
            anyAnyAcl.insertAce(0, BasePermission.READ, anonSid, true);
            if (toCreate) {
                createAcl(anyAnyAcl);
            } else {
                updateAcl(anyAnyAcl);
            }
        }
    }

    public List<PermissionTarget> getAllPermissionTargets() {
        List<Acl> acls = getAllAcls();
        List<PermissionTarget> repoPaths = new ArrayList<PermissionTarget>(acls.size());
        for (Acl acl : acls) {
            repoPaths.add(acl.getPermissionTarget());
        }
        return repoPaths;
    }

    public List<Acl> getAllAcls() {
        return getAllAclsFromCache();
    }

    public List<Acl> getAllAcls(ArtifactorySid[] sids) {
        List<Acl> acls = getAllAcls();
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

    @SuppressWarnings({"unchecked"})
    public Collection<Acl> loadAllAcls() {
        ObjectContentManager ocm = jcr.getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(Acl.class);
        filter.setScope(getAclsJcrPath() + "/");
        Query query = queryManager.createQuery(filter);
        Collection<Acl> acls = ocm.getObjects(query);
        return acls;
    }

    public void removeAllUserAces(String username) {
        List<Acl> acls = getAllAcls();
        PrincipalSid sid = new PrincipalSid(username);
        //Work on a copy of the list to avoid CME
        List<Acl> aclsCopy = new ArrayList<Acl>(acls);
        for (Acl acl : aclsCopy) {
            if (acl.removePrincipalAces(sid)) {
                updateAcl(acl);
            }
        }
    }

    public void deleteAllAcls() {
        //Must work on a copy of the (cached) acl collection in order not to get into concurrent mod
        //exceptions on it
        List<Acl> acls = getAllAcls();
        List<Acl> aclsToDelete = new ArrayList<Acl>();
        aclsToDelete.addAll(acls);
        for (Acl acl : aclsToDelete) {
            deleteAcl(acl.getObjectIdentity());
        }
    }

    public List<AclInfo> getAllAclDescriptors() {
        List<Acl> acls = getAllAcls();
        ArrayList<AclInfo> descriptors = new ArrayList<AclInfo>(acls.size());
        for (Acl acl : acls) {
            descriptors.add(acl.getDescriptor());
        }
        return descriptors;
    }

    private static Acl toAcl(PermissionTarget permissionTarget) {
        Assert.notNull(permissionTarget, "Non null repoPath required");
        return new Acl(permissionTarget);
    }

    @SuppressWarnings({"unchecked"})
    private synchronized List<Acl> getAllAclsFromCache() {
        Element element = aclsCache.get(ACLS_KEY);
        List<Acl> acls = element != null ? (List<Acl>) element.getValue() : null;
        if (acls == null) {
            log.debug("Loading ACLs from JCR database ...");
            Collection<Acl> loadedAcls = loadAllAcls();
            log.debug("Successfully loaded " + loadedAcls.size() + " ACLs");
            acls = new ArrayList<Acl>(loadedAcls);
            Element aclsElement = new Element(ACLS_KEY, acls);
            aclsCache.put(aclsElement);
        }
        return acls;
    }

    private synchronized void updateCache(Acl acl) {
        List<Acl> acls = getAllAcls();
        if (acls.contains(acl)) {
            acls.remove(acl);
        }
        acls.add(acl);
        Element element = new Element(ACLS_KEY, acls);
        aclsCache.put(element);
    }

    private synchronized void removeFormCache(Acl acl) {
        List<Acl> acls = getAllAcls();
        acls.remove(acl);
        Element element = new Element(ACLS_KEY, acls);
        aclsCache.put(element);
    }
}