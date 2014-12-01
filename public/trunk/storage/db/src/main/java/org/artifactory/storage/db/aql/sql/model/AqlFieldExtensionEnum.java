package org.artifactory.storage.db.aql.sql.model;

import org.artifactory.aql.model.AqlField;
import org.artifactory.aql.model.AqlTableFieldsEnum;

import static org.artifactory.aql.model.AqlTableFieldsEnum.*;
import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.*;

/**
 * @author Gidi Shabat
 */
public enum AqlFieldExtensionEnum {
    // node
    artifactRepo(AqlField.artifactRepo, nodes, repo, false),
    artifactPath(AqlField.artifactPath, nodes, node_path, false),
    artifactName(AqlField.artifactName, nodes, node_name, false),
    artifactCreated(AqlField.artifactCreated, nodes, created, false),
    artifactModified(AqlField.artifactModified, nodes, modified, true),
    artifactUpdated(AqlField.artifactUpdated, nodes, updated, true),
    artifactCreatedBy(AqlField.artifactCreatedBy, nodes, created_by, true),
    artifactModifiedBy(AqlField.artifactModifiedBy, nodes, modified_by, true),
    artifactType(AqlField.artifactType, nodes, node_type, false),
    artifactDepth(AqlField.artifactDepth, nodes, depth, false),
    artifactNodeId(AqlField.artifactNodeId, nodes, node_id, false),
    artifactOriginalMd5(AqlField.artifactOriginalMd5, nodes, md5_original, true),
    artifactActualMd5(AqlField.artifactActualMd5, nodes, md5_actual, true),
    artifactOriginalSha1(AqlField.artifactOriginalSha1, nodes, sha1_original, true),
    artifactActualSha1(AqlField.artifactActualSha1, nodes, sha1_actual, true),
    artifactSize(AqlField.artifactSize, nodes, bin_length, true),
    // stats
    artifactDownloaded(AqlField.artifactDownloaded, stats, last_downloaded, true),
    artifactDownloads(AqlField.artifactDownloads, stats, download_count, true),
    artifactDownloadedBy(AqlField.artifactDownloadedBy, stats, last_downloaded_by, true),
    // properties
    propertyKey(AqlField.propertyKey, node_props, prop_key, false),
    propertyValue(AqlField.propertyValue, node_props, prop_value, true),
    // archive entries
    archiveEntryName(AqlField.archiveEntryName, archive_names, entry_name, false),
    archiveEntryPath(AqlField.archiveEntryName, archive_paths, entry_path, false),
    // builds
    buildModuleName(AqlField.buildModuleName, build_modules, module_name_id, false),
    buildDependencyName(AqlField.buildDependencyName, build_dependencies, dependency_name_id, false),
    buildDependencyScope(AqlField.buildDependencyScope, build_dependencies, dependency_scopes, false),
    buildDependencyType(AqlField.buildDependencyType, build_dependencies, dependency_type, false),
    buildDependencySha1(AqlField.buildDependencySha1, build_dependencies, sha1, false),
    buildDependencyMd5(AqlField.buildDependencyMd5, build_dependencies, md5, false),
    buildArtifactName(AqlField.buildArtifactName, build_artifacts, artifact_name, false),
    buildArtifactType(AqlField.buildArtifactType, build_artifacts, artifact_type, false),
    buildArtifactSha1(AqlField.buildArtifactSha1, build_artifacts, sha1, false),
    buildArtifactMd5(AqlField.buildArtifactMd5, build_artifacts, md5, false),
    buildPropertyKey(AqlField.buildPropertyKey, build_props, prop_key, false),
    buildPropertyValue(AqlField.buildPropertyValue, build_props, prop_value, false),
    buildUrl(AqlField.buildUrl, builds, ci_url, false),
    buildName(AqlField.buildName, builds, build_name, false),
    buildNumber(AqlField.buildNumber, builds, build_number, false),
    buildCreated(AqlField.buildCreated, builds, created, false),
    buildCreatedBy(AqlField.buildCreatedBy, builds, created_by, true),
    buildModified(AqlField.buildModified, builds, modified, true),
    buildModifiedBy(AqlField.buildModifiedBy, builds, modified_by, true);

    private AqlField aqlField;
    public SqlTableEnum table;
    public AqlTableFieldsEnum tableField;
    private boolean nullable;

    AqlFieldExtensionEnum(AqlField aqlField, SqlTableEnum table, AqlTableFieldsEnum tableField,
            boolean nullable) {
        this.aqlField = aqlField;
        this.table = table;
        this.tableField = tableField;
        this.nullable = nullable;
    }

    public boolean isNullable() {
        return nullable;
    }

    public static AqlFieldExtensionEnum getExtensionFor(AqlField field) {
        for (AqlFieldExtensionEnum fieldExtensionEnum : values()) {
            if (fieldExtensionEnum.aqlField == field) {
                return fieldExtensionEnum;
            }
        }
        return null;
    }
}
