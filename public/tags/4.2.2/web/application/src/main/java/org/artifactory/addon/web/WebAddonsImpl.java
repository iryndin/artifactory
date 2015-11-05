/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.addon.web;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonType;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.build.BuildAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.build.artifacts.BuildArtifactsRequest;
import org.artifactory.api.rest.build.diff.BuildsDiff;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.build.ArtifactoryBuildArtifact;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.fs.FileInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.util.HttpUtils;
import org.jfrog.build.api.BaseBuildFileBean;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * Default implementation of the addons interface. Represents a normal execution of artifactory.
 * <p/>
 * <strong>NOTE!</strong> Do not create anonymous or non-static inner classes in addon
 *
 * @author freds
 * @author Yossi Shaul
 */
@org.springframework.stereotype.Component
public final class WebAddonsImpl implements CoreAddons, BuildAddon {
    private static final Logger log = LoggerFactory.getLogger(WebAddonsImpl.class);

    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public Set<ArtifactoryBuildArtifact> getBuildArtifactsFileInfosWithFallback(Build build) {
        return Sets.newHashSet();
    }

    @Override
    public Set<ArtifactoryBuildArtifact> getBuildArtifactsFileInfos(Build build) {
        return Sets.newHashSet();
    }

    @Override
    public Map<Dependency, FileInfo> getBuildDependenciesFileInfos(Build build) {
        return Maps.newHashMap();
    }

    @Override
    public void renameBuildNameProperty(String from,
            String to) {
    }

    @Override
    public void discardOldBuildsByDate(String buildName, BuildRetention buildRetention,
            BasicStatusHolder multiStatusHolder) {
        // nop
    }

    @Override
    public void discardOldBuildsByCount(String buildName, BuildRetention discard, BasicStatusHolder multiStatusHolder) {
        // nop
    }

    @Override
    public BuildPatternArtifacts getBuildPatternArtifacts(
            @Nonnull BuildPatternArtifactsRequest buildPatternArtifactsRequest, String servletContextUrl) {
        return new BuildPatternArtifacts();
    }

    @Override
    public Map<FileInfo, String> getBuildArtifacts(BuildArtifactsRequest buildArtifactsRequest) {
        return null;
    }

    @Override
    public File getBuildArtifactsArchive(BuildArtifactsRequest buildArtifactsRequest) {
        return null;
    }

    @Override
    public BuildsDiff getBuildsDiff(Build firstBuild, Build secondBuild, String baseStorageInfoUri) {
        return null;
    }

    @Override
    public FileInfo getFileBeanInfo(BaseBuildFileBean artifact, Build build) {
        return null;
    }

    @Override
    public String getListBrowsingVersion() {
        VersionInfo versionInfo = centralConfigService.getVersionInfo();
        return format("Artifactory/%s", versionInfo.getVersion());
    }

    @Override
    public String getArtifactoryUrl() {
        MutableCentralConfigDescriptor mutableCentralConfigDescriptor = centralConfigService.getMutableDescriptor();
        MailServerDescriptor mailServer = mutableCentralConfigDescriptor.getMailServer();
        if (mailServer != null && StringUtils.isNotBlank(mailServer.getArtifactoryUrl())) {
            return mailServer.getArtifactoryUrl();
        }
        return null;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public boolean isCreateDefaultAdminAccountAllowed() {
        return true;
    }

    @Override
    public boolean isAolAdmin() {
        return false;
    }

    @Override
    public boolean isAolAdmin(UserInfo userInfo) {
        return false;
    }

    @Override
    public boolean isAol() {
        return false;
    }

    @Override
    @Nonnull
    public List<String> getUsersForBackupNotifications() {
        List<UserInfo> allUsers = ContextHelper.get().beanForType(UserGroupService.class).getAllUsers(true);
        List<String> adminEmails = Lists.newArrayList();
        for (UserInfo user : allUsers) {
            if (user.isAdmin()) {
                if (StringUtils.isNotBlank(user.getEmail())) {
                    adminEmails.add(user.getEmail());
                } else {
                    log.debug("User '{}' has no email address.", user.getUsername());
                }
            }
        }
        return adminEmails;
    }

    /**
     * Validates that given licenseHash is different from license installed on this instance,
     * unless artifactoryId and current instance artifactoryId are equal (e.g same Artifactory)
     *
     * @param licenseHash license to check
     * @param artifactoryId artifactory id of the checked license
     */
    @Override
    public boolean validateTargetHasDifferentLicense(String licenseHash, String artifactoryId) {
        AddonsManager addonsManager = getAddonsManager();
        if (Strings.isNullOrEmpty(licenseHash)) {
            log.debug("LicenseHash is empty, validation isn't possible");
        } else {
            if (!addonsManager.getLicenseKeyHash().equals(licenseHash)) {
                return true;
            } else {
                if (!Strings.isNullOrEmpty(artifactoryId)) {
                    if(HttpUtils.getHostId().equals(artifactoryId))
                        return true;
                } else {
                    log.debug("LicenseHash is equal to currently used license, but artifactoryId is empty, " +
                            "validation of destination and source artifactories being same instance isn't possible");
                }
            }
        }
        return false;
    }

    @Override
    public void validateTargetHasDifferentLicenseKeyHash(String targetLicenseHash, List<String> addons) {
        AddonsManager addonsManager = getAddonsManager();
        // Skip Trial license
        if (isTrial(addonsManager)) {
            log.debug("Source has trial license, skipping target instance license validation.");
            return;
        }
        if (StringUtils.isBlank(targetLicenseHash)) {
            if (addons == null || !addons.contains(AddonType.REPLICATION.getAddonName())) {
                throw new IllegalArgumentException(
                        "Replication between an open-source Artifactory instance is not supported.");
            }

            throw new IllegalArgumentException(
                    "Could not retrieve license key from remote target, user must have deploy permissions.");
        }
        if (addonsManager.getLicenseKeyHash().equals(targetLicenseHash)) {
            throw new IllegalArgumentException("Replication between same-license servers is not supported.");
        }
    }

    @Override
    public void validateMultiPushReplicationSupportedForTargetLicense(String targetLicenseKey,
            boolean isMultiPushConfigure, String targetUrl) {
        AddonsManager addonsManager = getAddonsManager();
        if (!addonsManager.isLicenseKeyHashHAType(targetLicenseKey) && isMultiPushConfigure) {
            log.info("Multi Push Replication is not supported for target :" + targetUrl);
            throw new IllegalArgumentException(
                    "Multi Push Replication is supported for targets with an enterprise license only");
        }
    }

    @Override
    public String getBuildNum() {
        VersionInfo versionInfo = centralConfigService.getVersionInfo();
        return format("%s rev %s", versionInfo.getVersion(), versionInfo.getRevision());
    }

    private boolean isTrial(AddonsManager addonsManager) {
        return addonsManager.isLicenseInstalled() && "Trial".equalsIgnoreCase(addonsManager.getLicenseDetails()[2]);
    }

    private AddonsManager getAddonsManager() {
        return ContextHelper.get().beanForType(AddonsManager.class);
    }
}
