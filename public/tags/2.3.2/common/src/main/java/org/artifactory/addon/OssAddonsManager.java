/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.artifactory.api.context.ContextHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Yossi Shaul
 */
@Component
public class OssAddonsManager implements AddonsManager {

    public <T extends Addon> T addonByType(Class<T> type) {
        return ContextHelper.get().beanForType(type);
    }

    public String getProductName() {
        return "Artifactory";
    }

    public List<String> getInstalledAddonNames() {
        return Collections.emptyList();
    }

    public List<String> getEnabledAddonNames() {
        return Collections.emptyList();
    }

    public void refresh() {

    }

    public AddonInfo getAddonInfoByName(String addonName) {
        return null;
    }

    public boolean isLicenseInstalled() {
        return false;
    }

    public boolean isLicenseKeyValid(String licenseKey) {
        return false;
    }

    public String getAddonProperty(String addonName, String addonKey) {
        throw new UnsupportedOperationException();
    }

    public boolean isAddonActivated(String addonName) {
        return false;
    }

    public void installLicense(String licenseKey) throws IOException {
        throw new UnsupportedOperationException();
    }

    public String getLicenseKey() {
        throw new UnsupportedOperationException();
    }

    public String getLicenseKeyHash() {
        return "";
    }

    public String[] getLicenseDetails() {
        throw new UnsupportedOperationException();
    }

    public String getFooterMessage(boolean admin) {
        return null;
    }

    public String getLicenseFooterMessage() {
        return null;
    }

    public boolean lockdown() {
        return false;
    }

    public boolean isInstantiationAuthorized(Class componentClass) {
        return true;
    }

    public String interceptRequest() {
        return null;
    }
}
