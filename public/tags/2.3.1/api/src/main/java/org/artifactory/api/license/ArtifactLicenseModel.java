/*
 * Copyright 2010 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.api.license;

import java.io.Serializable;

/**
 * @author Tomer Cohen
 */
public class ArtifactLicenseModel implements Serializable {
    public static final String UNAPPROVED = "Unapproved";
    public static final String APPROVED = "Approved";

    private String name;
    private String longName;
    private String url;
    private String comments;
    private String status;
    private String regexp;
    private boolean isApproved;

    public ArtifactLicenseModel() {
    }

    public ArtifactLicenseModel(LicenseInfo licenseInfo) {
        this.comments = licenseInfo.getComments();
        this.longName = licenseInfo.getLongName();
        this.name = licenseInfo.getName();
        this.status = licenseInfo.isApproved() ? APPROVED : UNAPPROVED;
        this.url = licenseInfo.getUrl();
        this.regexp = licenseInfo.getRegexp();
        this.isApproved = licenseInfo.isApproved();
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public void changeStatus() {
        if (APPROVED.equalsIgnoreCase(getStatus())) {
            setApproved(false);
            setStatus(UNAPPROVED);
        } else {
            setApproved(true);
            setStatus(APPROVED);
        }
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public LicenseInfo buildLicenseInfo() {
        LicenseInfo info = new LicenseInfo();
        info.setApproved(isApproved);
        info.setName(name);
        info.setLongName(longName);
        info.setComments(comments);
        info.setRegexp(regexp);
        info.setUrl(url);
        return info;
    }
}
