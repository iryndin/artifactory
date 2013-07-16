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

package org.artifactory.addon;

import com.google.common.collect.Lists;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.security.ArtifactoryPermission;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Yossi Shaul
 */
@Component
public class OssAddonsManager implements AddonsManager, AddonsWebManager {

    @Override
    public <T extends Addon> T addonByType(Class<T> type) {
        return ContextHelper.get().beanForType(type);
    }

    @Override
    public String getProductName() {
        return "Artifactory";
    }

    @Override
    public String getLicenseRequiredMessage(String licensePageUrl) {
        return "Add-ons are currently disabled.";
    }

    @Override
    public void onNoInstalledLicense(boolean userVisitedLicensePage, NoInstalledLicenseAction action) {
    }

    @Override
    public boolean isAdminPageAccessible() {
        AuthorizationService authService = ContextHelper.get().beanForType(AuthorizationService.class);
        return authService.isAdmin() || authService.hasPermission(ArtifactoryPermission.MANAGE);
    }

    @Override
    public List<AddonInfo> getInstalledAddons(Set<String> excludedAddonKeys) {
        List<AddonInfo> addonInfos = Lists.newArrayList();
        for (AddonType addonType : AddonType.values()) {
            if (AddonType.AOL.equals(addonType)) {
                continue;
            }
            addonInfos.add(new AddonInfo(addonType.getAddonName(), addonType.getAddonDisplayName(), null,
                    AddonState.INACTIVATED, null, addonType.getDisplayOrdinal()));
        }

        Collections.sort(addonInfos);
        return addonInfos;
    }

    @Override
    public List<String> getEnabledAddonNames() {
        return Collections.emptyList();
    }

    @Override
    public void importFrom(ImportSettings settings) {

    }

    @Override
    public boolean isLicenseInstalled() {
        return false;
    }

    @Override
    public boolean isLicenseKeyValid(String licenseKey) {
        return false;
    }

    @Override
    public String getAddonProperty(String addonName, String addonKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void installLicense(String licenseKey) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLicenseKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLicenseKeyHash() {
        return "";
    }

    @Override
    public String[] getLicenseDetails() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFooterMessage(boolean admin) {
        return null;
    }

    @Override
    public String getLicenseFooterMessage() {
        return null;
    }

    @Override
    public boolean lockdown() {
        return false;
    }

    @Override
    public boolean isInstantiationAuthorized(Class componentClass) {
        return true;
    }

    @Override
    public void interceptResponse(ArtifactoryResponse response) {
    }
}
