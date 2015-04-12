package org.artifactory.converters.helpers;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.converters.ConverterManager;
import org.artifactory.converters.ConvertersManagerImpl;
import org.artifactory.converters.VersionProvider;
import org.artifactory.converters.VersionProviderImpl;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.spring.SpringConfigPaths;
import org.artifactory.state.model.ArtifactoryStateManager;
import org.artifactory.storage.db.properties.service.ArtifactoryCommonDbPropertiesService;
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService;
import org.artifactory.version.ArtifactoryVersion;

import java.util.Map;

/**
 * @author Gidi Shabat
 */
public class MockArtifactoryContext implements ArtifactoryContext {

    private ConvertersManagerImpl convertersManager;
    private VersionProviderImpl versionProvider;
    private final MockDbPropertiesService mockDbPropertiesService;
    private final MockArtifactoryStateManager mockArtifactoryStateManager;
    private final MockArtifactoryServersCommonService mockArtifactoryServersCommonService;

    public MockArtifactoryContext(ArtifactoryVersion version, long release, ConvertersManagerImpl convertersManager,
            VersionProviderImpl versionProvider) {
        this.convertersManager = convertersManager;
        this.versionProvider = versionProvider;
        mockDbPropertiesService = new MockDbPropertiesService(version, release);
        mockArtifactoryStateManager = new MockArtifactoryStateManager();
        mockArtifactoryServersCommonService = new MockArtifactoryServersCommonService(version);
    }

    @Override
    public <T> T beanForType(String name, Class<T> type) {
        return null;
    }

    @Override
    public CentralConfigService getCentralConfig() {
        return null;
    }

    @Override
    public <T> T beanForType(Class<T> type) {
        if (type.equals(ArtifactoryCommonDbPropertiesService.class)) {
            return (T) mockDbPropertiesService;
        }
        if (type.equals(ArtifactoryStateManager.class)) {
            return (T) mockArtifactoryStateManager;
        }
        if (type.equals(ArtifactoryServersCommonService.class)) {
            return (T) mockArtifactoryServersCommonService;
        }

        return null;
    }

    @Override
    public <T> Map<String, T> beansForType(Class<T> type) {
        return null;
    }

    @Override
    public Object getBean(String name) {
        return null;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return null;
    }

    @Override
    public AuthorizationService getAuthorizationService() {
        return null;
    }

    @Override
    public long getUptime() {
        return 0;
    }

    @Override
    public ArtifactoryHome getArtifactoryHome() {
        return null;
    }

    @Override
    public String getContextId() {
        return null;
    }

    @Override
    public SpringConfigPaths getConfigPaths() {
        return null;
    }

    @Override
    public String getServerId() {
        return "test";
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public void setOffline() {
    }

    @Override
    public ConverterManager getConverterManager() {
        return convertersManager;
    }

    @Override
    public VersionProvider getVersionProvider() {
        return versionProvider;
    }

    @Override
    public void destroy() {
        // NOOP
    }

    @Override
    public void exportTo(ExportSettings settings) {
    }

    @Override
    public void importFrom(ImportSettings settings) {
    }

    public MockDbPropertiesService getMockDbPropertiesService() {
        return mockDbPropertiesService;
    }

    public MockArtifactoryStateManager getMockArtifactoryStateManager() {
        return mockArtifactoryStateManager;
    }
}