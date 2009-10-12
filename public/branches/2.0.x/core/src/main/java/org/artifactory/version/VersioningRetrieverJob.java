package org.artifactory.version;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.api.version.VersionHolder;
import org.artifactory.api.version.VersionInfoService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A job to retrieve Artifactory versioning info from the jfrog service. Used for async communication.
 *
 * @author Yossi Shaul
 */
public class VersioningRetrieverJob extends QuartzCommand {
    private final static Logger log = LoggerFactory.getLogger(VersioningRetrieverJob.class);

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        Map<Object, Object> cache = cacheService.getCache(ArtifactoryCache.versioning);
        if (cache.get(VersionInfoServiceImpl.CACHE_KEY) != null) {
            // finish if already in cache
            log.trace("Cached version already in cache: {}", cache.get(VersionInfoServiceImpl.CACHE_KEY));
            return;
        }

        JobDataMap jobData = callbackContext.getJobDetail().getJobDataMap();
        @SuppressWarnings({"unchecked"})
        Map<String, String> headersMap = (Map) jobData.get("headers");
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