package org.artifactory.ui.rest.model.general;

import org.artifactory.rest.common.model.BaseModel;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Chen Keinan
 */
public class Footer extends BaseModel {
    private String versionInfo;
    private String buildNumber;
    private String licenseInfo;
    private String copyRights;
    private String copyRightsUrl;
    private boolean isAol;
    private String versionID;
    private boolean globalRepoEnabled;

    public Footer(String licenseInfo, String versionInfo, String copyRights, String copyRightsUrl,
                  String buildNumber, boolean isAol, boolean isGlobalRepoEnabled, String versionID) {
        this.licenseInfo = licenseInfo;
        this.versionInfo = versionInfo;
        this.copyRights = copyRights;
        this.copyRightsUrl = copyRightsUrl;
        this.buildNumber = buildNumber;
        this.isAol = isAol;
        this.globalRepoEnabled = isGlobalRepoEnabled;
        this.versionID = versionID;
    }

    public String getLicenseInfo() {
        return licenseInfo;
    }

    public void setLicenseInfo(String licenseInfo) {
        this.licenseInfo = licenseInfo;
    }

    public String getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(String versionInfo) {
        this.versionInfo = versionInfo;
    }

    public String getCopyRights() {
        return copyRights;
    }

    public void setCopyRights(String copyRights) {
        this.copyRights = copyRights;
    }

    public String getCopyRightsUrl() {
        return copyRightsUrl;
    }

    public void setCopyRightsUrl(String copyRightsUrl) {
        this.copyRightsUrl = copyRightsUrl;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @JsonProperty("isAol")
    public boolean isAol() {
        return isAol;
    }

    public String getVersionID() {
        return versionID;
    }

    public void setVersionID(String versionID) {
        this.versionID = versionID;
    }

    public boolean isGlobalRepoEnabled() {
        return globalRepoEnabled;
    }
}
