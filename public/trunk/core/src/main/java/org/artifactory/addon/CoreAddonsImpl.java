/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.UserInfo;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of the core-related addon factories.
 *
 * @author Yossi Shaul
 */
@Component
public class CoreAddonsImpl implements WebstartAddon, LdapGroupAddon {
    private static final String POWERPACK_MESSAGE =
            "This API is available only in the Artifactory Addon-ons Power Pack (http://www.jfrog.org/addons.php).";

    public boolean isDefault() {
        return true;
    }

    public VirtualRepo createVirtualRepo(InternalRepositoryService repoService, VirtualRepoDescriptor descriptor) {
        return new VirtualRepo(repoService, descriptor);
    }

    public void importKeyStore(ImportSettings settings) {
        // do nothing
    }

    public void exportKeyStore(ExportSettings exportSettings) {
        // do nothing
    }

    public void populateGroups(DirContextOperations dirContextOperations, UserInfo userInfo) {
        // do nothing
    }

    public void populateGroups(String dn, UserInfo info) {
        // do nothing
    }

    public List<LdapSetting> getEnabledLdapSettings() {
        CentralConfigDescriptor descriptor = ContextHelper.get().beanForType(
                CentralConfigService.class).getDescriptor();
        List<LdapSetting> enabledLdapSettings = descriptor.getSecurity().getEnabledLdapSettings();
        if (enabledLdapSettings != null && !enabledLdapSettings.isEmpty()) {
            return Lists.newArrayList(enabledLdapSettings.get(0));
        }
        return Lists.newArrayList();
    }
}
