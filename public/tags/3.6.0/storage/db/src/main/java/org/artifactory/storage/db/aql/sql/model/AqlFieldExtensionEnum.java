package org.artifactory.storage.db.aql.sql.model;

import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.model.AqlTableFieldsEnum;

import static org.artifactory.aql.model.AqlTableFieldsEnum.*;
import static org.artifactory.storage.db.aql.sql.model.SqlTableEnum.*;

/**
 * @author Gidi Shabat
 */
public enum AqlFieldExtensionEnum {
    // node
    artifactRepo(AqlFieldEnum.itemRepo, nodes, repo, false),
    artifactPath(AqlFieldEnum.itemPath, nodes, node_path, false),
    artifactName(AqlFieldEnum.itemName, nodes, node_name, false),
    artifactCreated(AqlFieldEnum.itemCreated, nodes, created, false),
    artifactModified(AqlFieldEnum.itemModified, nodes, modified, true),
    artifactUpdated(AqlFieldEnum.itemUpdated, nodes, updated, true),
    artifactCreatedBy(AqlFieldEnum.itemCreatedBy, nodes, created_by, true),
    artifactModifiedBy(AqlFieldEnum.itemModifiedBy, nodes, modified_by, true),
    artifactType(AqlFieldEnum.itemType, nodes, node_type, false),
    artifactDepth(AqlFieldEnum.itemDepth, nodes, depth, false),
    artifactNodeId(AqlFieldEnum.itemNodeId, nodes, node_id, false),
    artifactOriginalMd5(AqlFieldEnum.itemOriginalMd5, nodes, md5_original, true),
    artifactActualMd5(AqlFieldEnum.itemActualMd5, nodes, md5_actual, true),
    artifactOriginalSha1(AqlFieldEnum.itemOriginalSha1, nodes, sha1_original, true),
    artifactActualSha1(AqlFieldEnum.itemActualSha1, nodes, sha1_actual, true),
    artifactSize(AqlFieldEnum.itemSize, nodes, bin_length, true),
    // stats
    artifactDownloaded(AqlFieldEnum.statDownloaded, stats, last_downloaded, true),
    artifactDownloads(AqlFieldEnum.statDownloads, stats, download_count, true),
    artifactDownloadedBy(AqlFieldEnum.statDownloadedBy, stats, last_downloaded_by, true),
    // properties
    propertyKey(AqlFieldEnum.propertyKey, node_props, prop_key, false),
    propertyValue(AqlFieldEnum.propertyValue, node_props, prop_value, true),
    // archive entries
    archiveEntryName(AqlFieldEnum.archiveEntryName, archive_names, entry_name, false),
    archiveEntryPath(AqlFieldEnum.archiveEntryPath, archive_paths, entry_path, false),
    // builds
    buildModuleName(AqlFieldEnum.buildModuleName, build_modules, module_name_id, false),
    buildDependencyName(AqlFieldEnum.buildDependencyName, build_dependencies, dependency_name_id, false),
    buildDependencyScope(AqlFieldEnum.buildDependencyScope, build_dependencies, dependency_scopes, false),
    buildDependencyType(AqlFieldEnum.buildDependencyType, build_dependencies, dependency_type, false),
    buildDependencySha1(AqlFieldEnum.buildDependencySha1, build_dependencies, sha1, false),
    buildDependencyMd5(AqlFieldEnum.buildDependencyMd5, build_dependencies, md5, false),
    buildArtifactName(AqlFieldEnum.buildArtifactName, build_artifacts, artifact_name, false),
    buildArtifactType(AqlFieldEnum.buildArtifactType, build_artifacts, artifact_type, false),
    buildArtifactSha1(AqlFieldEnum.buildArtifactSha1, build_artifacts, sha1, false),
    buildArtifactMd5(AqlFieldEnum.buildArtifactMd5, build_artifacts, md5, false),
    buildPropertyKey(AqlFieldEnum.buildPropertyKey, build_props, prop_key, false),
    buildPropertyValue(AqlFieldEnum.buildPropertyValue, build_props, prop_value, false),
    buildUrl(AqlFieldEnum.buildUrl, builds, ci_url, false),
    buildName(AqlFieldEnum.buildName, builds, build_name, false),
    buildNumber(AqlFieldEnum.buildNumber, builds, build_number, false),
    buildCreated(AqlFieldEnum.buildCreated, builds, created, false),
    buildCreatedBy(AqlFieldEnum.buildCreatedBy, builds, created_by, true),
    buildModified(AqlFieldEnum.buildModified, builds, modified, true),
    buildModifiedBy(AqlFieldEnum.buildModifiedBy, builds, modified_by, true);

    private AqlFieldEnum aqlField;
    public SqlTableEnum table;
    public AqlTableFieldsEnum tableField;
    private boolean nullable;

    AqlFieldExtensionEnum(AqlFieldEnum aqlField, SqlTableEnum table, AqlTableFieldsEnum tableField,
            boolean nullable) {
        this.aqlField = aqlField;
        this.table = table;
        this.tableField = tableField;
        this.nullable = nullable;
    }

    public boolean isNullable() {
        return nullable;
    }

    public static AqlFieldExtensionEnum getExtensionFor(AqlFieldEnum field) {
        for (AqlFieldExtensionEnum fieldExtensionEnum : values()) {
            if (fieldExtensionEnum.aqlField == field) {
                return fieldExtensionEnum;
            }
        }
        return null;
    }
}
