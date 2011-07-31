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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.artifactory.addon.license.LicenseStatus;
import org.artifactory.addon.license.LicensesAddon;
import org.artifactory.addon.replication.LocalReplicationSettings;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.md.PropertiesImpl;
import org.artifactory.api.security.UserInfo;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.fs.FileInfo;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.Request;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.util.RepoLayoutUtils;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of the core-related addon factories.
 *
 * @author Yossi Shaul
 */
@Component
public class CoreAddonsImpl implements WebstartAddon, LdapGroupAddon, LicensesAddon, PropertiesAddon, LayoutsCoreAddon,
        FilteredResourcesAddon, ReplicationAddon {

    private static final Logger log = LoggerFactory.getLogger(CoreAddonsImpl.class);

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

    public void addExternalGroups(String userName, Set<UserInfo.UserGroupInfo> groups) {
        // nop
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

    public List<FilterBasedLdapUserSearch> getLdapUserSearches(ContextSource ctx, LdapSetting settings) {
        SearchPattern searchPattern = settings.getSearch();
        String searchBase = searchPattern.getSearchBase();
        if (searchBase == null) {
            searchBase = "";
        }
        ArrayList<FilterBasedLdapUserSearch> result = new ArrayList<FilterBasedLdapUserSearch>();
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(searchBase,
                searchPattern.getSearchFilter(), (BaseLdapPathContextSource) ctx);
        userSearch.setSearchSubtree(searchPattern.isSearchSubTree());
        result.add(userSearch);
        return result;
    }

    public void performOnBuildArtifacts(Build build) {
        // NOP
    }

    public void addPropertySetToRepository(RealRepoDescriptor descriptor) {
        // NOP
    }

    public void importLicenses(ImportSettings settings) {
        // NOP
    }

    public void exportLicenses(ExportSettings exportSettings) {
        // nop
    }

    public List findLicensesInRepos(List<RepoDescriptor> repositories, LicenseStatus status) {
        return Lists.newArrayList();
    }

    public Properties getProperties(RepoPath repoPath) {
        return new PropertiesImpl();
    }

    public Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths) {
        return Maps.newHashMap();
    }

    public void deleteProperty(RepoPath repoPath, String property) {
        // nop
    }

    public void addProperty(RepoPath repoPath, PropertySet propertySet, Property property, String... values) {
        //nop
    }

    public RepoResource assembleDynamicMetadata(MetadataInfo info, JcrFsItem<?> metadataHostingItem,
            Properties queryProps, RepoPath metadataRepoPath) {
        return new MetadataResource(info);
    }

    public boolean isFilteredResourceFile(RepoPath repoPath) {
        return false;
    }

    public RepoResource getFilteredResource(Request request, FileInfo fileInfo, InputStream fileInputStream) {
        return new UnfoundRepoResource(fileInfo.getRepoPath(),
                "Creation of a filtered resource requires the Properties add-on.", HttpStatus.SC_FORBIDDEN);
    }

    public RepoResource getZipResource(Request request, FileInfo fileInfo, InputStream stream) {
        return new UnfoundRepoResource(fileInfo.getRepoPath(),
                "Direct resource download from zip requires the Filtered resources add-on.", HttpStatus.SC_FORBIDDEN);
    }

    public ResourceStreamHandle getZipResourceHandle(RepoResource resource, InputStream stream) {
        throw new UnsupportedOperationException(
                "Direct resource download from zip requires the Filtered resources add-on.");
    }

    public String filterResource(Request request, Properties contextProperties, Reader reader) throws Exception {
        try {
            return IOUtils.toString(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public void toggleResourceFilterState(RepoPath repoPath, boolean filtered) {
    }

    public void assertLayoutConfigurationsBeforeSave(CentralConfigDescriptor newDescriptor) {
        List<RepoLayout> repoLayouts = newDescriptor.getRepoLayouts();
        if ((repoLayouts == null) || repoLayouts.isEmpty()) {
            throw new ConfigurationException("Could not find any repository layouts.");
        }

        if (repoLayouts.size() != 4) {
            throw new ConfigurationException("There should be 4 default repository layouts.");
        }

        assertLayoutsExistsAndEqual(repoLayouts, RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.IVY_DEFAULT,
                RepoLayoutUtils.GRADLE_DEFAULT, RepoLayoutUtils.MAVEN_1_DEFAULT);
    }

    private void assertLayoutsExistsAndEqual(List<RepoLayout> repoLayouts, RepoLayout... expectedLayouts) {
        for (RepoLayout expectedLayout : expectedLayouts) {
            assertLayoutExistsAndEqual(repoLayouts, expectedLayout);
        }
    }

    private void assertLayoutExistsAndEqual(List<RepoLayout> repoLayouts, RepoLayout expectedLayout) {

        if (!repoLayouts.contains(expectedLayout)) {
            throw new ConfigurationException("Could not find the default repository layout: " +
                    expectedLayout.getName());
        }

        RepoLayout existingLayoutConfig = repoLayouts.get(repoLayouts.indexOf(expectedLayout));
        if (!EqualsBuilder.reflectionEquals(existingLayoutConfig, expectedLayout)) {
            throw new ConfigurationException("The configured repository layout '" + expectedLayout.getName() +
                    "' is different from the default configuration.");
        }
    }

    public MultiStatusHolder performRemoteReplication(RemoteReplicationSettings settings) {
        return getReplicationRequiredStatusHolder();
    }

    public MultiStatusHolder performLocalReplication(LocalReplicationSettings settings) {
        return getReplicationRequiredStatusHolder();
    }

    private MultiStatusHolder getReplicationRequiredStatusHolder() {
        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();
        multiStatusHolder.setError("Error: the replication addon is required for this operation.",
                HttpStatus.SC_BAD_REQUEST, log);
        return multiStatusHolder;
    }
}
