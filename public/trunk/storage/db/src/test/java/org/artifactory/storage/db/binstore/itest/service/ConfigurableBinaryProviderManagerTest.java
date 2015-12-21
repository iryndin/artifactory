package org.artifactory.storage.db.binstore.itest.service;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.storage.LinkedProperties;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.binstore.service.EmptyBinaryProvider;
import org.artifactory.storage.binstore.service.FileBinaryProvider;
import org.artifactory.storage.binstore.service.base.BinaryProviderBase;
import org.artifactory.storage.binstore.service.providers.FileBinaryProviderImpl;
import org.artifactory.storage.binstore.service.providers.FileCacheBinaryProviderImpl;
import org.artifactory.storage.binstore.service.providers.RetryBinaryProvider;
import org.artifactory.storage.config.BinaryProviderConfigBuilder;
import org.artifactory.storage.config.model.ChainMetaData;
import org.artifactory.storage.db.binstore.service.BinaryStoreImpl;
import org.artifactory.storage.db.binstore.service.BlobBinaryProviderImpl;
import org.artifactory.storage.db.binstore.service.ConfigurableBinaryProviderManager;
import org.artifactory.storage.db.binstore.service.UsageTrackingBinaryProvider;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.artifactory.storage.StorageProperties.BinaryProviderType;
import static org.artifactory.storage.StorageProperties.Key;
import static org.artifactory.storage.binstore.service.BinaryProviderFactory.buildProviders;

/**
 * @author Gidi Shabat
 */
@Test
public class ConfigurableBinaryProviderManagerTest extends DbBaseTest {

    @Autowired
    BinaryStoreImpl binaryStore;

    @Autowired
    StorageProperties storageProperties;

    public static ChainMetaData buildByConfig(String userConfigFile) throws IOException {
        InputStream userConfigStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(userConfigFile);
        String defaultConfigPath = "default-storage-config.xml";
        InputStream defaultConfigStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(defaultConfigPath);
        return BinaryProviderConfigBuilder.buildByUserConfig(defaultConfigStream, userConfigStream);
    }

    @Test
    public void binaryProviderWithOverrideProviderTest() throws IOException {
        ChainMetaData chainMetaData = buildByConfig("binarystore/config/binarystoreWithOverideProviders.xml");
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase blobBinaryProvider = binaryProvider.next();
        Assert.assertTrue(blobBinaryProvider instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blobBinaryProvider.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void binaryProviderWithTemplateTest() throws IOException {
        ChainMetaData chainMetaData = buildByConfig("binarystore/config/binarystore-filesystem-template.xml");
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase fileSystem = binaryProvider.next();
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("filestore", ((FileBinaryProvider) fileSystem).getBinariesDir().getName());
    }

    @Test
    public void binaryProviderWithExistingProviderTest() throws IOException {
        ChainMetaData chainMetaData = buildByConfig("binarystore/config/binarystoreWithExistingProviders.xml");
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase blobBinaryProvider = binaryProvider.next();
        Assert.assertTrue(blobBinaryProvider instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blobBinaryProvider.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void binaryProviderWithTemplateAndProviderTest() throws IOException {
        ChainMetaData chainMetaData = buildByConfig("binarystore/config/binarystoreWithTemplateAndProvider.xml");
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase cache = binaryProvider.next();
        Assert.assertTrue(cache instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase fileSystem = cache.next();
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("test654", ((FileBinaryProvider) fileSystem).getBinariesDir().getName());
    }

    @Test
    public void binaryProviderWithUserChainTest() throws IOException {
        ChainMetaData chainMetaData = buildByConfig("binarystore/config/binarystoreWithUserChain.xml");
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase cacheFs = binaryProvider.next();
        Assert.assertTrue(cacheFs instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase retry = cacheFs.next();
        Assert.assertTrue(retry instanceof RetryBinaryProvider);
        BinaryProviderBase fileSystem2 = retry.next();
        Assert.assertTrue(fileSystem2 instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem2.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("test89", ((FileBinaryProvider) cacheFs).getBinariesDir().getName());
        Assert.assertEquals("test99", ((FileBinaryProvider) fileSystem2).getBinariesDir().getName());
    }

    @Test
    public void oldGenerationWithFullDB() throws IOException {
        updateStorageProperty(Key.binaryProviderCacheMaxSize, "1000");
        updateStorageProperty(Key.binaryProviderType, BinaryProviderType.fullDb.name());
        ChainMetaData chainMetaData = ConfigurableBinaryProviderManager.buildByStorageProperties(storageProperties);
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase cacheFs = binaryProvider.next();
        Assert.assertTrue(cacheFs instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase blob = cacheFs.next();
        Assert.assertTrue(blob instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blob.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
        Assert.assertEquals("cache", ((FileBinaryProvider) cacheFs).getBinariesDir().getName());
    }

    @Test
    public void oldGenerationWithFullDBDirect() throws IOException {
        // If the cache size is 0 then no cache binary provider will be created
        updateStorageProperty(Key.binaryProviderCacheMaxSize, "0");
        updateStorageProperty(Key.binaryProviderType, BinaryProviderType.fullDb.name());
        ChainMetaData chainMetaData = ConfigurableBinaryProviderManager.buildByStorageProperties(storageProperties);
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase blob = binaryProvider.next();
        Assert.assertTrue(blob instanceof BlobBinaryProviderImpl);
        BinaryProviderBase empty = blob.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void oldGenerationWithFileSystemDBDirect() throws IOException {
        // If the cache size is 0 then no cache binary provider will be created
        updateStorageProperty(Key.binaryProviderCacheMaxSize, "0");
        updateStorageProperty(Key.binaryProviderType, BinaryProviderType.filesystem.name());
        ChainMetaData chainMetaData = ConfigurableBinaryProviderManager.buildByStorageProperties(storageProperties);
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase fileSystem = binaryProvider.next();
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    @Test
    public void oldGenerationWithCacheAndFile() throws IOException {
        // If the cache size is 0 then no cache binary provider will be created
        updateStorageProperty(Key.binaryProviderType, BinaryProviderType.cachedFS.name());
        ChainMetaData chainMetaData = ConfigurableBinaryProviderManager.buildByStorageProperties(storageProperties);
        BinaryProviderBase binaryProvider = buildProviders(chainMetaData, binaryStore, storageProperties);
        Assert.assertTrue(binaryProvider instanceof UsageTrackingBinaryProvider);
        BinaryProviderBase cacheFS = binaryProvider.next();
        Assert.assertTrue(cacheFS instanceof FileCacheBinaryProviderImpl);
        BinaryProviderBase fileSystem = cacheFS.next();
        Assert.assertTrue(fileSystem instanceof FileBinaryProviderImpl);
        BinaryProviderBase empty = fileSystem.next();
        Assert.assertTrue(empty instanceof EmptyBinaryProvider);
    }

    protected void updateStorageProperty(Key key, String value) {
        Object propsField = ReflectionTestUtils.getField(storageProperties, "props");
        ReflectionTestUtils.invokeMethod(propsField, "setProperty", key.key(), value);
    }
}
