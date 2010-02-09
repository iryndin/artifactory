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

package org.artifactory.version;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

import java.util.Map;

/**
 * A job to retrieve Artifactory versioning info from the jfrog service. Used for async communication.
 *
 * @author Yossi Shaul
 */
public class VersioningRetrieverJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(VersioningRetrieverJob.class);

    public static final String ATTR_HEADERS = "headers";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        Cache<Object, Object> cache = cacheService.getCache(ArtifactoryCache.versioning);
        if (cache.get(VersionInfoServiceImpl.CACHE_KEY) != null) {
            // finish if already in cache
            log.trace("Cached version already in cache: {}", cache.get(VersionInfoServiceImpl.CACHE_KEY));
            return;
        }

        JobDataMap jobData = callbackContext.getJobDetail().getJobDataMap();
        @SuppressWarnings({"unchecked"})
        Map<String, String> headersMap = (Map) jobData.get(VersioningRetrieverJob.ATTR_HEADERS);
        VersionInfoService versioningService = InternalContextHelper.get().beanForType(VersionInfoService.class);
        ArtifactoryVersioning versioning;
        try {
            log.debug("Retrieving Artifactory versioning from remote server");
            versioning = versioningService.getRemoteVersioning(headersMap);
        } catch (Exception e) {
            log.debug("Failed to retrieve Artifactory versioning from remote server {}", e.getMessage());
            versioning = createServiceUnavailableVersioning();
        }
        cache.put(VersionInfoServiceImpl.CACHE_KEY, versioning);
    }

    private ArtifactoryVersioning createServiceUnavailableVersioning() {
        return new ArtifactoryVersioning(VersionHolder.VERSION_UNAVAILABLE, VersionHolder.VERSION_UNAVAILABLE);
    }
}