package org.artifactory.storage.db.mbean;

import org.artifactory.storage.db.spring.ArtifactoryDataSource;

/**
 * MBean wrapper for {@link org.artifactory.storage.db.spring.ArtifactoryDataSource}
 *
 * @author mamo
 */
public class ManagedDataSource implements ManagedDataSourceMBean {

    private final ArtifactoryDataSource artifactoryDataSource;

    public ManagedDataSource(ArtifactoryDataSource dataSource) {
        artifactoryDataSource = dataSource;
    }

    @Override
    public int getActiveConnectionsCount() {
        return artifactoryDataSource.getActiveConnectionsCount();
    }

    @Override
    public int getIdleConnectionsCount() {
        return artifactoryDataSource.getIdleConnectionsCount();
    }

    @Override
    public int getMaxActive() {
        return artifactoryDataSource.getMaxActive();
    }

    @Override
    public int getMaxIdle() {
        return artifactoryDataSource.getMaxIdle();
    }

    @Override
    public long getMaxWait() {
        return artifactoryDataSource.getMaxWait();
    }

    @Override
    public int getMinIdle() {
        return artifactoryDataSource.getMinIdle();
    }

    @Override
    public String getUrl() {
        return artifactoryDataSource.getUrl();
    }
}
