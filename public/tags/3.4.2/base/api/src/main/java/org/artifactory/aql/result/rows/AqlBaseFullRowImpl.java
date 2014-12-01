package org.artifactory.aql.result.rows;

import org.artifactory.aql.model.AqlField;

import java.util.Date;
import java.util.Map;

import static org.artifactory.aql.model.AqlDomainEnum.*;

/**
 * @author Gidi Shabat
 */
@QueryTypes({artifacts, properties, statistics, archive, build_dependencies,
        build_artifacts, build_module, build_props, builds})
public class AqlBaseFullRowImpl
        implements AqlRowResult, FullRow, AqlArtifact, AqlBaseArtifact, AqlArtifactsWithProperties, AqlArchiveArtifact,
        AqlArtifactsWithStatistics, AqlArtifactWithBuildArtifacts, AqlProperty {

    Map<AqlField, Object> map;

    public AqlBaseFullRowImpl(Map<AqlField, Object> map) {
        this.map = map;
    }

    @Override
    public Date getCreated() {
        return (Date) map.get(AqlField.artifactCreated);
    }

    @Override
    public Date getModified() {
        return (Date) map.get(AqlField.artifactModified);
    }

    @Override
    public Date getUpdated() {
        return (Date) map.get(AqlField.artifactUpdated);
    }

    @Override
    public String getCreatedBy() {
        return (String) map.get(AqlField.artifactCreatedBy);
    }

    @Override
    public String getModifiedBy() {
        return (String) map.get(AqlField.artifactModifiedBy);
    }

    @Override
    public Date getDownloaded() {
        return (Date) map.get(AqlField.artifactDownloaded);
    }

    @Override
    public int getDownloads() {
        return (int) map.get(AqlField.artifactDownloads);
    }

    @Override
    public String getDownloadedBy() {
        return (String) map.get(AqlField.artifactDownloadedBy);
    }

    @Override
    public int getType() {
        return (int) map.get(AqlField.artifactType);
    }

    @Override
    public String getRepo() {
        return (String) map.get(AqlField.artifactRepo);
    }

    @Override
    public String getPath() {
        return (String) map.get(AqlField.artifactPath);
    }

    @Override
    public String getName() {
        return (String) map.get(AqlField.artifactName);
    }

    @Override
    public long getSize() {
        return (long) map.get(AqlField.artifactSize);
    }

    @Override
    public int getDepth() {
        return (int) map.get(AqlField.artifactDepth);
    }

    @Override
    public int getNodeId() {
        return (int) map.get(AqlField.artifactNodeId);
    }

    @Override
    public String getOriginalMd5() {
        return (String) map.get(AqlField.artifactOriginalMd5);
    }

    @Override
    public String getActualMd5() {
        return (String) map.get(AqlField.artifactActualMd5);
    }

    @Override
    public String getOriginalSha1() {
        return (String) map.get(AqlField.artifactOriginalSha1);
    }

    @Override
    public String getActualSha1() {
        return (String) map.get(AqlField.artifactActualSha1);
    }

    @Override
    public String getKey() {
        return (String) map.get(AqlField.propertyKey);
    }

    @Override
    public String getValue() {
        return (String) map.get(AqlField.propertyValue);
    }

    @Override
    public String getEntryName() {
        return (String) map.get(AqlField.archiveEntryName);
    }

    @Override
    public String getEntryPath() {
        return (String) map.get(AqlField.archiveEntryPath);
    }

    @Override
    public String getBuildModuleName() {
        return (String) map.get(AqlField.buildModuleName);
    }

    @Override
    public String getBuildDependencyName() {
        return (String) map.get(AqlField.buildDependencyName);
    }

    @Override
    public String getBuildDependencyScope() {
        return (String) map.get(AqlField.buildDependencyScope);
    }

    @Override
    public String getBuildDependencyType() {
        return (String) map.get(AqlField.buildDependencyType);
    }

    @Override
    public String getBuildDependencySha1() {
        return (String) map.get(AqlField.buildDependencySha1);
    }

    @Override
    public String getBuildDependencyMd5() {
        return (String) map.get(AqlField.buildDependencyMd5);
    }

    @Override
    public String getBuildArtifactName() {
        return (String) map.get(AqlField.buildArtifactName);
    }

    @Override
    public String getBuildArtifactType() {
        return (String) map.get(AqlField.buildArtifactType);
    }

    @Override
    public String getBuildArtifactSha1() {
        return (String) map.get(AqlField.buildArtifactSha1);
    }

    @Override
    public String getBuildArtifactMd5() {
        return (String) map.get(AqlField.buildArtifactMd5);
    }

    @Override
    public String getBuildPropKey() {
        return (String) map.get(AqlField.buildPropertyKey);
    }

    @Override
    public String getBuildPropValue() {
        return (String) map.get(AqlField.buildPropertyValue);
    }

    @Override
    public String getBuildUrl() {
        return (String) map.get(AqlField.buildUrl);
    }

    @Override
    public String getBuildName() {
        return (String) map.get(AqlField.buildName);
    }

    @Override
    public String getBuildNumber() {
        return (String) map.get(AqlField.buildNumber);
    }

    @Override
    public Date getBuildCreated() {
        return (Date) map.get(AqlField.buildCreated);
    }

    @Override
    public String getBuildCreatedBy() {
        return (String) map.get(AqlField.buildCreatedBy);
    }

    @Override
    public Date getBuildModified() {
        return (Date) map.get(AqlField.buildModified);
    }

    @Override
    public String getBuildModifiedBy() {
        return (String) map.get(AqlField.buildModifiedBy);
    }

    @Override
    public String toString() {
        return "AqlBaseFullRowImpl{map=" + map + "}";
    }
}
