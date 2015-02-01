/*
 * Copyright 2012 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.addon.model.license;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.license.LicenseInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.ShowInTreeAction;

import java.util.List;

/**
 * @author Tomer Cohen
 */
public class ModuleLicenseModel extends RepoAwareActionableItemBase {
    private String id;
    private String md5;
    private String sha1;
    private LicenseInfo license;
    private String scopes;
    private List<String> scopesList;
    private boolean selected;
    private boolean overridable;
    private boolean isNeutral = false;

    private LicenseInfo calculatedLicense;

    public ModuleLicenseModel(LicenseInfo license, String id, RepoPath repoPath) {
        super(repoPath);
        this.license = license;
        this.id = id;
        this.calculatedLicense = license;
    }

    public LicenseInfo getLicense() {
        return license;
    }

    public void setLicense(LicenseInfo license) {
        this.license = license;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        setScopesList(scopes);
        this.scopes = PathUtils.collectionToDelimitedString(scopes);
    }

    public LicenseInfo getCalculatedLicense() {
        return calculatedLicense;
    }

    public void setCalculatedLicense(LicenseInfo calculatedLicense) {
        this.calculatedLicense = calculatedLicense;
    }

    public boolean isNeutral() {
        return isNeutral;
    }

    public void setNeutral(boolean neutral) {
        isNeutral = neutral;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }


    public List<String> getScopesList() {
        return scopesList;
    }

    public void setScopesList(List<String> scopesList) {
        this.scopesList = scopesList != null ? scopesList : Lists.<String>newArrayList();
    }

    public boolean isOverridable() {
        return overridable;
    }

    public void setOverridable(boolean overridable) {
        this.overridable = overridable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModuleLicenseModel that = (ModuleLicenseModel) o;

        if (!getId().equals(that.getId())) {
            return false;
        }
        if (!license.equals(that.license)) {
            return false;
        }
        if ((getRepoPath() != null) && !getRepoPath().equals(that.getRepoPath())) {
            return false;
        }
        if ((calculatedLicense != null && that.calculatedLicense != null) && !calculatedLicense.equals(
                that.calculatedLicense)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = 31 * getId().hashCode();
        result = 31 * result + license.hashCode();
        if (getRepoPath() != null) {
            result = 31 * result + getRepoPath().hashCode();
        }
        return result;
    }

    @Override
    public String getDisplayName() {
        return getId();
    }

    @Override
    public String getCssClass() {
        return null;
    }

    @Override
    public void filterActions(AuthorizationService authService) {
        if (getRepoPath() != null && authService.canRead(getRepoPath())) {
            getActions().add(new ShowInTreeAction());
        }
    }

    public String getDependencyScope() {
        String dependencyScopes = getScopes();
        if (dependencyScopes == null || StringUtils.isBlank(dependencyScopes)) {
            return "";
        } else {
            return dependencyScopes;
        }
    }

    public boolean isInConflict() {
        return getCalculatedLicense() != null && !getLicense().equals(getCalculatedLicense());
    }
}
