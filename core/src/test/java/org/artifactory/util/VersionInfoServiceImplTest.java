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

package org.artifactory.util;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.version.ArtifactoryVersioning;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.descriptor.config.CentralConfigDescriptorImpl;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.version.VersionInfoServiceImpl;
import org.artifactory.version.VersioningRetrieverJob;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.artifactory.api.version.VersionInfoService.SERVICE_UNAVAILABLE;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests the cached and remote data retrieval behaviour of the VersionInfoService
 *
 * @author Noam Tenne
 */
public class VersionInfoServiceImplTest {
    private final static Logger log = LoggerFactory.getLogger(VersionInfoServiceImplTest.class);

    /**
     * Creates mock representations of the VersionInfo service and tests it by attempting to retrive info from the cache
     * and the remote source
     */
    @Test
    public void testVersioningRetrieverJob() {
        // setup mocks
        InternalArtifactoryContext context = createMock(InternalArtifactoryContext.class);
        CentralConfigService centralConfigService = createMock(CentralConfigService.class);
        ArtifactoryContextThreadBinder.bind(context);
        CacheService cacheService = createMock(CacheService.class);
        Cache cache = createMock(Cache.class);
        expect(cache.get("versioning")).andReturn(null).anyTimes();
        expect(cacheService.getCache(ArtifactoryCache.versioning)).andReturn(cache).anyTimes();
        TaskService taskService = createMock(TaskService.class);
        expect(taskService.hasTaskOfType(VersioningRetrieverJob.class)).andReturn(false);
        expect(taskService.startTask((TaskBase) anyObject())).andReturn("token");

        replay(context, cacheService, cache, taskService, centralConfigService);

        // test
        VersionInfoServiceImpl infoService = new VersionInfoServiceImpl();
        ReflectionTestUtils.setField(infoService, "cacheService", cacheService);
        ReflectionTestUtils.setField(infoService, "taskService", taskService);
        String version1 = infoService.getLatestVersion(Collections.<String, String>emptyMap(), false);
        assertTrue(SERVICE_UNAVAILABLE.equals(version1));

        // verify and tear down
        verify(context, cacheService, cache, taskService, centralConfigService);
        ArtifactoryContextThreadBinder.unbind();
    }

    @Test
    public void retrieveVersioningFromJFrogService() {
        InternalArtifactoryContext context = createMock(InternalArtifactoryContext.class);
        ArtifactoryContextThreadBinder.bind(context);
        CentralConfigService centralConfigService = createMock(CentralConfigService.class);
        expect(context.getCentralConfig()).andReturn(centralConfigService).anyTimes();
        expect(centralConfigService.getDescriptor()).andReturn(new CentralConfigDescriptorImpl());

        replay(context, centralConfigService);

        try {
            System.setProperty(ConstantValues.artifactoryVersion.getPropertyName(), "test");
            ArtifactorySystemProperties artifactorySystemProperties = new ArtifactorySystemProperties();
            artifactorySystemProperties.loadArtifactorySystemProperties(null, null);
            ArtifactorySystemProperties.bind(artifactorySystemProperties);
            VersionInfoServiceImpl infoService = new VersionInfoServiceImpl();
            ArtifactoryVersioning versioning;
            try {
                versioning = infoService.getRemoteVersioning(Collections.<String, String>emptyMap());
            } catch (ItemNotFoundRuntimeException e) {
                log.warn("Failed to find latest version (perhaps offline). *Not* failing the test.");
                return;
            }
            assertFalse(SERVICE_UNAVAILABLE.equals(versioning.getRelease().getVersion()));

            // verify and tear down
            verify(context, centralConfigService);
        } finally {
            System.clearProperty(ConstantValues.artifactoryVersion.getPropertyName());
            ArtifactorySystemProperties.unbind();
            ArtifactoryContextThreadBinder.unbind();
        }
    }
}