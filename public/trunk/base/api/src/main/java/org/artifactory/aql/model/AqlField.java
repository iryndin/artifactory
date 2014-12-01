package org.artifactory.aql.model;

import static org.artifactory.aql.model.AqlVariableTypeEnum.*;

/**
 * @author Gidi Shabat
 */
public enum AqlField {
    // node
    artifactRepo("_artifact_repo", string),
    artifactPath("_artifact_path", string),
    artifactName("_artifact_name", string),
    artifactCreated("_artifact_created", date),
    artifactModified("_artifact_modified", date),
    artifactUpdated("_artifact_updated", date),
    artifactCreatedBy("_artifact_created_by", string),
    artifactModifiedBy("_artifact_modified_by", string),
    artifactType("_artifact_type", integer),
    artifactDepth("_artifact_depth", integer),
    artifactNodeId("_artifact_node", integer),
    artifactOriginalMd5("_artifact_original_md5", string),
    artifactActualMd5("_artifact_actual_md5", string),
    artifactOriginalSha1("_artifact_original_sha1", string),
    artifactActualSha1("_artifact_actual_sha1", string),
    artifactSize("_artifact_size", longInt),
    // stats
    artifactDownloaded("_artifact_downloaded", date),
    artifactDownloads("_artifact_downloads", integer),
    artifactDownloadedBy("_artifact_downloaded_by", string),
    // properties
    propertyKey("_property_key", string),
    propertyValue("_property_value", string),
    // archive entries
    archiveEntryName("_archive_entry_name", string),
    archiveEntryPath("_archive_entry_path", string),
    // builds
    buildModuleName("_build_module_name", string),
    buildDependencyName("_build_dependency_name", string),
    buildDependencyScope("_build_dependency_scope", string),
    buildDependencyType("_build_dependency_type", string),
    buildDependencySha1("_build_dependency_sha1", string),
    buildDependencyMd5("_build_dependency_md5", string),
    buildArtifactName("_build_artifact_name", string),
    buildArtifactType("_build_artifact_type", string),
    buildArtifactSha1("_build_artifact_sha1", string),
    buildArtifactMd5("_build_artifact_md5", string),
    buildPropertyKey("_build_property_key", string),
    buildPropertyValue("_build_property_value", string),
    buildUrl("_build_url", string),
    buildName("_build_name", string),
    buildNumber("_build_number", string),
    buildCreated("_build_created", date),
    buildCreatedBy("_build_created_by", string),
    buildModified("_build_modified", date),
    buildModifiedBy("_build_modified_by", string);
    public String signature;
    public AqlVariableTypeEnum type;

    AqlField(String signature, AqlVariableTypeEnum type) {
        this.signature = signature;
        this.type = type;
    }

    public static AqlField value(String signature) {
        signature = signature.toLowerCase();
        for (AqlField field : values()) {
            if (field.signature.equals(signature)) {
                return field;
            }
        }
        return null;
    }
}
