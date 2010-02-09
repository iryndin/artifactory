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
import org.acegisecurity.Authentication;
import org.acegisecurity.acl.AclEntry;
import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.BasicAclProvider;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.acl.basic.cache.EhCacheBasedAclEntryCache;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.springframework.util.Assert;

import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class CustomAclProvider extends BasicAclProvider {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(CustomAclProvider.class);
    private static final String ARTIFACTORY_CACHE_NAME = "artifactory-cache";

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        //Init the cache
        URL url = this.getClass().getResource("/ehcache/ehcache.xml");
        //Trick ehcache to store its temp file under artifactory's "data" folder
        String origTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", ArtifactoryHome.path() + "/data");
        try {
            CacheManager cacheManager = CacheManager.create(url);
            cacheManager.addCache(ARTIFACTORY_CACHE_NAME);
            Cache cache = cacheManager.getCache(ARTIFACTORY_CACHE_NAME);
            EhCacheBasedAclEntryCache entryCache = new EhCacheBasedAclEntryCache();
            entryCache.setCache(cache);
            setBasicAclEntryCache(entryCache);
        } finally {
            if (origTmpDir != null) {
                System.setProperty("java.io.tmpdir", origTmpDir);
            }
        }
    }

    /**
     * This will receive only a RepoPath as the domainInstance
     *
     * @param domainInstance
     * @return
     */
    @Override
    protected AclObjectIdentity obtainIdentity(Object domainInstance) {
        Assert.isInstanceOf(RepoPath.class, domainInstance,
                "Cannot obtain object identity from a non RepoPath. Received: " +
                        domainInstance);
        return (RepoPath) domainInstance;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    @Override
    public SimpleAclEntry[] getAcls(Object domainInstance) {
        AclEntry[] aclEntries = super.getAcls(domainInstance);
        SimpleAclEntry[] simpleAclEntries = toSimpleAclEntries(aclEntries);
        return simpleAclEntries;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    @Override
    public AclEntry[] getAcls(Object domainInstance, Authentication authentication) {
        AclEntry[] aclEntries = super.getAcls(domainInstance, authentication);
        SimpleAclEntry[] simpleAclEntries = toSimpleAclEntries(aclEntries);
        return simpleAclEntries;
    }

    @SuppressWarnings({"SuspiciousSystemArraycopy"})
    private SimpleAclEntry[] toSimpleAclEntries(AclEntry[] aclEntries) {
        if (aclEntries == null) {
            return new SimpleAclEntry[]{};
        }
        SimpleAclEntry[] simpleAclEntries = new SimpleAclEntry[aclEntries.length];
        System.arraycopy(aclEntries, 0, simpleAclEntries, 0, aclEntries.length);
        return simpleAclEntries;
    }

    public void clearAclEntryCache() {
        CacheManager cacheManager = CacheManager.create();
        Cache cache = cacheManager.getCache(ARTIFACTORY_CACHE_NAME);
        cache.removeAll();
    }
}
