package org.artifactory.aql.model;

import static org.artifactory.aql.model.AqlField.*;

/**
 * @author Gidi Shabat
 */
public enum AqlDomainEnum {
    artifacts(artifactRepo, artifactPath, artifactName, artifactType),
    full_artifacts(artifactNodeId, artifactType, artifactRepo, artifactPath, artifactName, artifactDepth,
            artifactCreated,
            artifactCreatedBy,
            artifactModified, artifactModifiedBy,
            artifactUpdated, artifactSize, artifactActualSha1, artifactOriginalSha1, artifactActualMd5,
            artifactOriginalMd5),
    properties(propertyKey, propertyValue),
    statistics(artifactRepo, artifactPath, artifactName, artifactDownloaded, artifactDownloadedBy),

    archive(archiveEntryPath, archiveEntryName),

    builds(buildNumber, buildName),
    build_artifacts(artifactRepo, artifactPath, artifactName, artifactType, buildArtifactName, buildArtifactType),
    build_dependencies(artifactRepo, artifactPath, artifactName, artifactType, buildDependencyName,
            buildDependencyScope,
            buildDependencyType),
    build_module(buildModuleName),
    build_props(buildPropertyKey, buildPropertyValue);

    public AqlField[] fields;

    AqlDomainEnum(AqlField... fields) {
        this.fields = fields;
    }

}
