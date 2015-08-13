package org.artifactory.aql.result;

import org.artifactory.aql.model.AqlDomainEnum;
import org.artifactory.aql.model.AqlItemTypeEnum;
import org.artifactory.aql.model.AqlPermissionProvider;
import org.artifactory.aql.util.AqlUtils;
import org.artifactory.repo.RepoPath;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.List;

/**
 * @author Gidi Shabat
 */
public abstract class AqlRestResult implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(AqlRestResult.class);
    private AqlPermissionProvider permissionProvider;

    public AqlRestResult(AqlPermissionProvider permissionProvider) {
        this.permissionProvider = permissionProvider;
    }

    protected boolean canRead(AqlDomainEnum domain, final ResultSet resultSet) {
        if (permissionProvider.isAdmin()) {
            return true;
        } else {
            if (AqlDomainEnum.items == domain) {
                try {
                    String itemRepo = resultSet.getString("repo");
                    String itemPath = resultSet.getString("node_path");
                    String itemName = resultSet.getString("node_name");
                    RepoPath repoPath = AqlUtils.repoPathFromAql(itemRepo, itemPath, itemName);
                    return permissionProvider.canRead(repoPath);
                } catch (Exception e) {
                    log.error("AQL minimal field expectation error: repo, path and name");
                }
            }
            return false;
        }
    }

    protected boolean canRead(AqlDomainEnum domain, final String repo, final String path, final String name) {
        if (permissionProvider.isAdmin()) {
            return true;
        } else {
            if (AqlDomainEnum.items == domain) {
                try {
                    RepoPath repoPath = AqlUtils.repoPathFromAql(repo, path, name);
                    return permissionProvider.canRead(repoPath);
                } catch (Exception e) {
                    log.error("AQL minimal field expectation error: repo, path and name");
                }
            }
            return false;
        }
    }

    public abstract byte[] read() throws IOException;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonPropertyOrder(value = {"itemRepo", "itemPath", "itemName", "itemType", "itemSize", "itemCreated", "itemCreatedBy", "itemModified", "itemModifiedBy", "itemUpdated", "itemDepth"}, alphabetic = true)
    protected class Row {
        @JsonProperty("repo")
        protected String itemRepo;
        @JsonProperty("path")
        protected String itemPath;
        @JsonProperty("name")
        protected String itemName;
        @JsonProperty("size")
        protected Long itemSize;
        @JsonProperty("depth")
        protected Integer itemDepth;
        @JsonProperty("id")
        protected Integer itemNodeId;
        @JsonProperty("modified")
        protected String itemModified;
        @JsonProperty("created")
        protected String itemCreated;
        @JsonProperty("updated")
        protected String itemUpdated;
        @JsonProperty("created_by")
        protected String itemCreatedBy;
        @JsonProperty("modified_by")
        protected String itemModifiedBy;
        @JsonProperty("type")
        protected AqlItemTypeEnum itemType;
        @JsonProperty("original_md5")
        protected String itemOriginalMd5;
        @JsonProperty("actual_md5")
        protected String itemActualMd5;
        @JsonProperty("original_sha1")
        protected String itemOriginalSha1;
        @JsonProperty("actual_sha1")
        protected String itemActualSha1;

        @JsonProperty("downloaded")
        protected String statDownloaded;
        @JsonProperty("downloads")
        protected Integer statDownloads;
        @JsonProperty("downloaded_by")
        protected String statDownloadedBy;

        @JsonProperty("key")
        protected String propertyKey;
        @JsonProperty("value")
        protected String propertyValue;

        @JsonProperty("entry.name")
        protected String archiveEntryName;
        @JsonProperty("entry.path")
        protected String archiveEntryPath;
        @JsonProperty("module.name")
        protected String buildModuleName;
        @JsonProperty("dependency.name")
        protected String buildDependencyName;
        @JsonProperty("dependency.scope")
        protected String buildDependencyScope;
        @JsonProperty("dependency.type")
        protected String buildDependencyType;
        @JsonProperty("dependency.sha1")
        protected String buildDependencySha1;
        @JsonProperty("dependency.md5")
        protected String buildDependencyMd5;
        @JsonProperty("artifact.name")
        protected String buildArtifactName;
        @JsonProperty("artifact.type")
        protected String buildArtifactType;
        @JsonProperty("artifact.sha1")
        protected String buildArtifactSha1;
        @JsonProperty("artifact.md5")
        protected String buildArtifactMd5;
        @JsonProperty("build.property.key")
        protected String buildPropertyKey;
        @JsonProperty("build.property.value")
        protected String buildPropertyValue;
        @JsonProperty("build.url")
        protected String buildUrl;
        @JsonProperty("build.name")
        protected String buildName;
        @JsonProperty("build.number")
        protected String buildNumber;
        @JsonProperty("build.created")
        protected String buildCreated;
        @JsonProperty("build.created_by")
        protected String buildCreatedBy;
        @JsonProperty("build.modified")
        protected String buildModified;
        @JsonProperty("build.modified_by")
        protected String buildModifiedBy;
        @JsonProperty("properties")
        protected List<Property> properties;
        public String getKey() {
            StringBuilder builder;
            builder = new StringBuilder();
            builder.append(itemRepo).append(itemPath).append(itemName).append(itemSize).append(itemDepth).append(
                    itemNodeId).append(itemModified).append(itemCreated).append(itemUpdated).append(
                    itemCreatedBy).append(itemModifiedBy).append(itemType).append(itemOriginalMd5).append(
                    itemActualMd5).append(itemOriginalSha1).append(itemActualSha1).append(
                    statDownloaded).append(statDownloads).append(statDownloadedBy).append(archiveEntryName).append(
                    archiveEntryPath).append(buildModuleName).append(buildDependencyName).append(
                    buildDependencyScope).append(buildDependencyType).append(buildDependencySha1).append(
                    buildDependencyMd5).append(buildArtifactName).append(buildArtifactType).append(
                    buildArtifactSha1).append(buildArtifactMd5).append(buildPropertyKey).append(buildPropertyValue).append(
                    buildUrl).append(buildName).append(buildNumber).append(buildCreated).append(buildCreatedBy).append(
                    buildModified).append(buildModifiedBy);
            return builder.toString();
        }

        public void put(String fieldName, Object value) {
            try {
                Field declaredField = getClass().getDeclaredField(fieldName);
                declaredField.setAccessible(true);
                declaredField.set(this, value);
            } catch (Exception e) {
                log.error("Failed to fill Aql result Object.");
            }
        }
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonPropertyOrder(value = {"propertyKey", "propertyValue"}, alphabetic = true)
    protected class Property {
        @JsonProperty("key")
        protected String propertyKey;
        @JsonProperty("value")
        protected String propertyValue;

        public void put(String fieldName, Object value) throws Exception {
            Field declaredField = getClass().getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            declaredField.set(this, value);
        }
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonPropertyOrder(value = {"start", "end", "total"}, alphabetic = true)
    protected class Range {

        @JsonProperty("start_pos")
        protected Long start;
        @JsonProperty("end_pos")
        protected Long end;
        @JsonProperty("total")
        protected Long total;
        @JsonProperty("limit")
        protected Long limited;

        public Range(long start, long end, long limited) {
            this.start = start;
            this.end = end;
            this.limited = Long.MAX_VALUE == limited ? null : limited;
        }

        public Range(long start, long end, long total, long limited) {
            this.start = start;
            this.end = end;
            this.total = total;
            this.limited = Long.MAX_VALUE == limited ? null : limited;
        }
    }
}
