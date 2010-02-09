package org.artifactory.util;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.version.VersionInfoServiceImpl;
import org.easymock.EasyMock;
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

    InternalArtifactoryContext context = EasyMock.createMock(InternalArtifactoryContext.class);
    CentralConfigService centralConfigService = EasyMock.createMock(CentralConfigService.class);
    CentralConfigDescriptor centralConfigDescriptor = EasyMock.createMock(CentralConfigDescriptor.class);

    @BeforeClass
    public void setUp() {
        EasyMock.expect(centralConfigDescriptor.getDefaultProxy()).andReturn(null);
        EasyMock.expect(centralConfigService.getDescriptor()).andReturn(centralConfigDescriptor);
        EasyMock.expect(context.getCentralConfig()).andReturn(centralConfigService);
        EasyMock.replay(centralConfigDescriptor, centralConfigService, context);
        ArtifactoryContextThreadBinder.bind(context);
    }

    /**
     * Creates mock representations of the VersionInfo service and tests it by attempting to retrive info from the cache
     * and the remote source
     */
    @Test
    @SuppressWarnings({"unchecked"})
    public void testService() {
        CacheService cacheService = EasyMock.createMock(CacheService.class);
        Map map = EasyMock.createMock(Map.class);
        EasyMock.expect(map.get("versioning")).andReturn(null);
        EasyMock.expect(map.put(EasyMock.anyObject(), EasyMock.anyObject())).andReturn(null);
        EasyMock.expect(cacheService.getCache(ArtifactoryCache.versioning)).andReturn(map).anyTimes();
        EasyMock.replay(map, cacheService);
        VersionInfoServiceImpl infoService = new VersionInfoServiceImpl();
        ReflectionTestUtils.setField(infoService, "cacheService", cacheService);
        String version1 = infoService.getLatestVersion(Collections.<String, String>emptyMap(), false);
        Assert.assertFalse("NA".equals(version1));
        EasyMock.verify();
    }
}