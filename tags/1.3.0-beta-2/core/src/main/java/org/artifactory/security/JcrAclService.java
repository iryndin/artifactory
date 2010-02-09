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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.artifactory.ArtifactoryHome;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.acls.AlreadyExistsException;
import org.springframework.security.acls.ChildrenExistException;
import org.springframework.security.acls.MutableAcl;
import org.springframework.security.acls.NotFoundException;
import org.springframework.security.acls.jdbc.AclCache;
import org.springframework.security.acls.jdbc.EhCacheBasedAclCache;
import org.springframework.security.acls.objectidentity.ObjectIdentity;
import org.springframework.security.acls.sid.Sid;
import org.springframework.util.Assert;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrAclService
        implements ExtendedAclService, InitializingBean, ApplicationContextAware {

    private static final String ACLS_KEY = "acls";
    private static final String ARTIFACTORY_CACHE_NAME = "artifactory-cache";

    private AclCache aclCache;
    private ArtifactoryApplicationContext applicationContext;

    public static String getAclsJcrPath() {
        return JcrPath.get().getOcmClassJcrPath(ACLS_KEY);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ArtifactoryApplicationContext) applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        //Create the storage folders
        JcrWrapper jcr = applicationContext.getJcr();
        jcr.getOrCreateUnstructuredNode(getAclsJcrPath());
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
            Cache cache = cacheManager.getCache(ARTIFACTORY_CACHE_NAME);
            aclCache = new EhCacheBasedAclCache(cache);
        } finally {
            if (origTmpDir != null) {
                System.setProperty("java.io.tmpdir", origTmpDir);
            }
        }
    }

    /**
     * Custom acl creation for repo path with no inheritence and no object ownerships
     *
     * @param objectIdentity the user or group this ACL apply to
     * @return the new ACL created
     * @throws AlreadyExistsException
     */
    public RepoPathAcl createAcl(ObjectIdentity objectIdentity) throws AlreadyExistsException {
        RepoPathAcl acl = toAcl(objectIdentity);
        return createAcl(acl);
    }

    public RepoPathAcl createAcl(RepoPathAcl acl) {
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        String aclPath = acl.getJcrPath();
        RepoPathAcl existingAcl = (RepoPathAcl) ocm.getObject(aclPath);
        //Check this object identity hasn't already been persisted
        if (existingAcl != null) {
            throw new AlreadyExistsException("Acls '" + aclPath + "' already exists");
        }
        ocm.insert(acl);
        ocm.save();
        aclCache.putInCache(acl);
        return acl;
    }

    public void deleteAcl(ObjectIdentity objectIdentity, boolean deleteChildren)
            throws ChildrenExistException {
        RepoPathAcl acl = toAcl(objectIdentity);
        //Delete the ACL and its ACEs
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        ocm.remove(acl);
        ocm.save();
        //Clear the cache
        aclCache.evictFromCache(acl);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoPathAcl updateAcl(RepoPathAcl acl) throws NotFoundException {
        //Delete and recreate the ACL
        deleteAcl(acl.getObjectIdentity(), true);
        RepoPathAcl newAcl = createAcl(acl);
        return newAcl;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoPathAcl updateAcl(MutableAcl acl) throws NotFoundException {
        return updateAcl(((RepoPathAcl) acl));
    }

    public ObjectIdentity[] findChildren(ObjectIdentity parentIdentity) {
        return new ObjectIdentity[0];
    }

    public RepoPathAcl readAclById(ObjectIdentity object) throws NotFoundException {
        return readAclById(object, null);
    }

    public RepoPathAcl readAclById(ObjectIdentity object, Sid[] sids) throws NotFoundException {
        Map<SecuredRepoPath, RepoPathAcl> result = readAclsById(new ObjectIdentity[]{object});
        return result.size() > 0 ? result.values().iterator().next() : null;
    }

    public Map<SecuredRepoPath, RepoPathAcl> readAclsById(ObjectIdentity[] objects)
            throws NotFoundException {
        return readAclsById(objects, null);
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    public Map<SecuredRepoPath, RepoPathAcl> readAclsById(ObjectIdentity[] objects, Sid[] sids)
            throws NotFoundException {
        Map<SecuredRepoPath, RepoPathAcl> result = new HashMap<SecuredRepoPath, RepoPathAcl>();
        //Iterate on each acl and check whether is needs to be loaded or can be retrieved from the
        //cache
        //TODO: [by yl] Check that jcr has no equivalent caching taking place by default
        Set<SecuredRepoPath> currentBatchToLoad = new HashSet<SecuredRepoPath>();
        for (int i = 0; i < objects.length; i++) {
            boolean aclFound = false;
            //Check we don't already have this ACL in the results
            if (result.containsKey(objects[i])) {
                aclFound = true;
            }
            //Check cache for the present ACL entry
            if (!aclFound) {
                RepoPathAcl acl = (RepoPathAcl) aclCache.getFromCache(objects[i]);
                if (acl != null) {
                    result.put(acl.getObjectIdentity(), acl);
                    aclFound = true;
                }
            }
            //Mark the ACL for load from storage
            if (!aclFound) {
                currentBatchToLoad.add((SecuredRepoPath) objects[i]);
            }
            //Is it time to load from storage?
            if (i + 1 == objects.length && currentBatchToLoad.size() > 0) {
                //Get all the acls for the passed objects sid
                Collection elements = getAcls(currentBatchToLoad);
                Map<SecuredRepoPath, RepoPathAcl> loadedBatch =
                        new HashMap<SecuredRepoPath, RepoPathAcl>(elements.size());
                for (Object element : elements) {
                    RepoPathAcl acl = (RepoPathAcl) element;
                    loadedBatch.put(acl.getObjectIdentity(), acl);
                }
                //Add loaded batch
                result.putAll(loadedBatch);
                //Add the loaded batch to the cache
                for (RepoPathAcl acl : loadedBatch.values()) {
                    aclCache.putInCache(acl);
                }
                currentBatchToLoad.clear();
            }
        }
        return result;
    }

    public List<SecuredRepoPath> getAllRepoPaths() {
        Collection elements = getAcls(null);
        List<SecuredRepoPath> repoPaths = new ArrayList<SecuredRepoPath>(elements.size());
        for (Object element : elements) {
            RepoPathAcl acl = (RepoPathAcl) element;
            repoPaths.add(acl.getObjectIdentity());
        }
        return repoPaths;
    }

    public List<RepoPathAcl> getAllAcls() {
        Collection elements = getAcls(null);
        List<RepoPathAcl> acls = new ArrayList<RepoPathAcl>(elements.size());
        for (Object element : elements) {
            RepoPathAcl acl = (RepoPathAcl) element;
            acls.add(acl);
        }
        return acls;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private static RepoPathAcl toAcl(ObjectIdentity objectIdentity) {
        SecuredRepoPath repoPath = (SecuredRepoPath) objectIdentity;
        Assert.notNull(repoPath, "Non null repoPath required");
        RepoPathAcl acl = new RepoPathAcl(repoPath);
        return acl;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private Collection getAcls(Set<SecuredRepoPath> repoPaths) {
        JcrWrapper jcr = applicationContext.getJcr();
        ObjectContentManager ocm = jcr.getOcm();
        QueryManager queryManager = ocm.getQueryManager();
        Filter filter = queryManager.createFilter(RepoPathAcl.class);
        filter.setScope(getAclsJcrPath() + "/");
        //Filter by repopaths
        if (repoPaths != null) {
            Filter repoPathAndFilter = queryManager.createFilter(RepoPathAcl.class);
            for (SecuredRepoPath repoPath : repoPaths) {
                Filter repoPathOrFilter = queryManager.createFilter(RepoPathAcl.class);
                //Required for identifier escaping
                RepoPathAcl queryAcl = new RepoPathAcl(repoPath);
                filter.addEqualTo("identifier", queryAcl.getIdentifier());
                repoPathAndFilter.addOrFilter(repoPathOrFilter);
            }
            filter.addAndFilter(repoPathAndFilter);
        }
        Query query = queryManager.createQuery(filter);
        Collection elements = ocm.getObjects(query);
        return elements;
    }
}