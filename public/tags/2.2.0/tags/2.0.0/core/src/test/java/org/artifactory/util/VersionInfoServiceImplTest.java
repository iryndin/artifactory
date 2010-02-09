package org.artifactory.util;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.version.ArtifactoryVersioning;
import static org.artifactory.api.version.VersionInfoService.SERVICE_UNAVAILABLE;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.version.VersionInfoServiceImpl;
import org.artifactory.version.VersioningRetrieverJob;
import static org.easymock.EasyMock.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

/**
 * Tests the cached and remote data retrieval behaviour of the VersionInfoService
 *
 * @author Noam Tenne
 */
public class VersionInfoServiceImplTest {

    InternalArtifactoryContext context = createMock(InternalArtifactoryContext.class);
    CentralConfigService centralConfigService = createMock(CentralConfigService.class);
    CentralConfigDescriptor centralConfigDescriptor = createMock(CentralConfigDescriptor.class);

    @BeforeClass
    public void setUp() {
        expect(centralConfigDescriptor.getDefaultProxy()).andReturn(null);
        expect(centralConfigService.getDescriptor()).andReturn(centralConfigDescriptor);
        expect(context.getCentralConfig()).andReturn(centralConfigService);
        replay(centralConfigDescriptor, centralConfigService, context);
        ArtifactoryContextThreadBinder.bind(context);
    }

    /**
     * Creates mock representations of the VersionInfo service and tests it by attempting to retrive info from the cache
     * and the remote source
     */
    @Test
    @SuppressWarnings({"unchecked"})
    public void testService() {
        CacheService cacheService = createMock(CacheService.class);
        Map map = createMock(Map.class);
        expect(map.get("versioning")).andReturn(null).anyTimes();
        expect(cacheService.getCache(ArtifactoryCache.versioning)).andReturn(map).anyTimes();
        TaskService taskService = createMock(TaskService.class);
        expect(taskService.hasTaskOfType(VersioningRetrieverJob.class)).andReturn(false);
        expect(taskService.startTask((TaskBase) anyObject())).andReturn("token");
        replay(map, cacheService, taskService);
        VersionInfoServiceImpl infoService = new VersionInfoServiceImpl();
        ReflectionTestUtils.setField(infoService, "cacheService", cacheService);
        ReflectionTestUtils.setField(infoService, "taskService", taskService);
        String version1 = infoService.getLatestVersion(Collections.<String, String>emptyMap(), false);
        Assert.assertTrue(SERVICE_UNAVAILABLE.equals(version1));
        verify();
    }

    @Test
    public void retrieveVersioningFromJFrogService() {
        VersionInfoServiceImpl infoService = new VersionInfoServiceImpl();
        ArtifactoryVersioning versioning = infoService.getRemoteVersioning(Collections.<String, String>emptyMap());
        Assert.assertFalse(SERVICE_UNAVAILABLE.equals(versioning.getRelease().getVersion()));
        verify();
    }
}