package org.artifactory.api.bintray;

import com.jfrog.bintray.client.api.details.PackageDetails;
import com.jfrog.bintray.client.api.details.VersionDetails;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container class for the Bintray client's PackageDetails and VersionDetails to allow easy deserialization from
 * Artifactory's own json format which also includes the file and property filters.
 *
 * @author Dan Feldman
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BintrayUploadInfo implements Serializable {

    @JsonProperty("package")
    PackageDetails packageDetails;
    @JsonProperty("version")
    VersionDetails versionDetails;
    @JsonProperty(value = "applyToFiles")
    private List<String> artifactPaths;
    @JsonProperty(value = "applyToProps")
    private Set<Map<String, Collection<String>>> filterProps;
    @JsonProperty(value = "publish")
    private Boolean publish;

    public BintrayUploadInfo(){

    }

    @JsonIgnore
    public BintrayUploadInfo(BintrayUploadInfoOverride override) {
        this.packageDetails = new PackageDetails(override.packageName);
        this.packageDetails.setSubject(override.subject);
        this.packageDetails.setRepo(override.repoName);
        this.packageDetails.setLicenses(override.licenses);
        this.versionDetails = new VersionDetails(override.versionName);
    }

    public PackageDetails getPackageDetails() {
        return packageDetails;
    }

    public void setPackageDetails(PackageDetails packageDetails) {
        this.packageDetails = packageDetails;
    }

    public VersionDetails getVersionDetails() {
        return versionDetails;
    }

    public void setVersionDetails(VersionDetails versionDetails) {
        this.versionDetails = versionDetails;
    }

    public List<String> getArtifactPaths() {
        return artifactPaths;
    }

    public void setArtifactPaths(List<String> artifactPaths) {
        this.artifactPaths = artifactPaths;
    }

    public Set<Map<String, Collection<String>>> getFilterProps() {
        return filterProps;
    }

    public void setFilterProps(Set<Map<String, Collection<String>>> filterProps) {
        this.filterProps = filterProps;
    }

    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }
}