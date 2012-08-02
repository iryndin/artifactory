/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.repo.jcr;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.jcr.JcrService;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.jcr.cache.expirable.CacheExpiry;
import org.artifactory.repo.jcr.local.LocalNonCacheOverridable;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
public class StoringRepoMixinTest extends ArtifactoryHomeBoundTest {

    LocalCacheRepo storingRepo = EasyMock.createMock(LocalCacheRepo.class);
    ArtifactoryContext context = EasyMock.createMock(ArtifactoryContext.class);
    StoringRepoMixin storingRepoMixin = new StoringRepoMixin(storingRepo, null);

    @BeforeClass
    public void setUp() throws Exception {
        ArtifactoryContextThreadBinder.bind(context);
    }

    @Test
    public void testChecksumProtection() throws Exception {
        for (ChecksumType checksumType : ChecksumType.values()) {
            assertFalse(storingRepoMixin.shouldProtectPathDeletion(checksumType.ext(), true),
                    "Checksum should never be protected.");
            assertFalse(storingRepoMixin.shouldProtectPathDeletion(checksumType.ext(), false),
                    "Checksum should never be protected.");
        }
    }

    @Test
    public void testCacheAndExpirableProtection() throws Exception {
        assertTrue(storingRepoMixin.shouldProtectPathDeletion("somepath", false),
                "Non-checksum item should never be protected when not overriding.");
        EasyMock.expect(storingRepo.isCache()).andReturn(true);
        CacheExpiry expiry = EasyMock.createMock(CacheExpiry.class);
        EasyMock.expect(expiry.isExpirable(EasyMock.eq(storingRepo), EasyMock.eq("somepath"))).andReturn(true);
        EasyMock.expect(context.beanForType(CacheExpiry.class)).andReturn(expiry);
        EasyMock.replay(expiry, context, storingRepo);
        assertFalse(storingRepoMixin.shouldProtectPathDeletion("somepath", true),
                "Expired path shouldn't be protected.");
        EasyMock.verify(expiry, context, storingRepo);
        EasyMock.reset(context, storingRepo);
    }

    @Test
    public void testLocalNotCacheAndOverridableProtection() throws Exception {
        EasyMock.expect(storingRepo.isCache()).andReturn(false);
        LocalNonCacheOverridable overridable = EasyMock.createMock(LocalNonCacheOverridable.class);
        EasyMock.expect(overridable.isOverridable(EasyMock.eq(storingRepo), EasyMock.eq("somepath"))).andReturn(true);
        EasyMock.expect(context.beanForType(LocalNonCacheOverridable.class)).andReturn(overridable);
        EasyMock.replay(overridable, context, storingRepo);
        assertFalse(storingRepoMixin.shouldProtectPathDeletion("somepath", true),
                "Overridable path shouldn't be protected.");
        EasyMock.verify(overridable, context, storingRepo);
        EasyMock.reset(context, storingRepo);
    }

    @Test
    public void testLocalNotCacheAndMetadataProtection() throws Exception {
        EasyMock.expect(storingRepo.isCache()).andReturn(false).times(2);
        LocalNonCacheOverridable overridable = EasyMock.createMock(LocalNonCacheOverridable.class);
        EasyMock.expect(overridable.isOverridable(EasyMock.eq(storingRepo), EasyMock.eq("maven-metadata.xml")))
                .andReturn(false);
        EasyMock.expect(overridable.isOverridable(EasyMock.eq(storingRepo), EasyMock.eq("some:metadata.xml")))
                .andReturn(false);
        EasyMock.expect(context.beanForType(LocalNonCacheOverridable.class)).andReturn(overridable).times(2);
        EasyMock.replay(overridable, context, storingRepo);
        assertFalse(storingRepoMixin.shouldProtectPathDeletion("maven-metadata.xml", true),
                "Metadata path shouldn't be protected.");
        assertFalse(storingRepoMixin.shouldProtectPathDeletion("some:metadata.xml", true),
                "Metadata path shouldn't be protected.");
        EasyMock.verify(overridable, context, storingRepo);
        EasyMock.reset(context, storingRepo);
    }

    @Test
    public void testLocalNotCacheAndNotFileProtection() throws Exception {
        EasyMock.expect(storingRepo.getKey()).andReturn("somekey");
        EasyMock.expect(storingRepo.isCache()).andReturn(false);
        LocalNonCacheOverridable overridable = EasyMock.createMock(LocalNonCacheOverridable.class);
        EasyMock.expect(overridable.isOverridable(EasyMock.eq(storingRepo), EasyMock.eq("somefile"))).andReturn(false);
        EasyMock.expect(context.beanForType(LocalNonCacheOverridable.class)).andReturn(overridable);
        JcrService jcrService = EasyMock.createMock(JcrService.class);
        EasyMock.expect(jcrService.isFile(EasyMock.eq(InternalRepoPathFactory.create("somekey", "somefile"))))
                .andReturn(false);
        Field jcrServiceField = storingRepoMixin.getClass().getDeclaredField("jcrService");
        jcrServiceField.setAccessible(true);
        jcrServiceField.set(storingRepoMixin, jcrService);
        EasyMock.replay(overridable, jcrService, context, storingRepo);
        assertFalse(storingRepoMixin.shouldProtectPathDeletion("somefile", true),
                "Items which aren't files shouldn't be protected.");
        EasyMock.verify(overridable, jcrService, context, storingRepo);
        EasyMock.reset(context, storingRepo);
    }
}
