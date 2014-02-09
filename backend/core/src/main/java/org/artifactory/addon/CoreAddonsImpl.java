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
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.ha.message.HaMessage;
import org.artifactory.addon.ha.message.HaMessageTopic;
import org.artifactory.addon.ha.semaphore.JVMSemaphoreWrapper;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.addon.license.LicenseStatus;
import org.artifactory.addon.license.LicensesAddon;
import org.artifactory.addon.replication.LocalReplicationSettings;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.request.ArtifactoryResponse;
import org.artifactory.api.rest.compliance.FileComplianceInfo;
import org.artifactory.api.rest.replication.ReplicationStatus;
import org.artifactory.api.rest.replication.ReplicationStatusType;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.replication.ReplicationBaseDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.RepoResource;
import org.artifactory.md.Properties;
import org.artifactory.repo.HttpRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.service.mover.MoverConfig;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.Request;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.schedule.Task;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.storage.fs.lock.FsItemsVault;
import org.artifactory.storage.fs.lock.FsItemsVaultCacheImpl;
import org.artifactory.storage.fs.lock.provider.JVMLockProvider;
import org.artifactory.storage.fs.lock.provider.LockProvider;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the core-related addon factories.
 *
 * @author Yossi Shaul
 */
@Component
public class CoreAddonsImpl implements WebstartAddon, LdapGroupAddon, LicensesAddon, PropertiesAddon, LayoutsCoreAddon,
        FilteredResourcesAddon, ReplicationAddon, YumAddon, NuGetAddon, RestCoreAddon, CrowdAddon, BlackDuckAddon,
        GemsAddon, HaAddon {

    private static final Logger log = LoggerFactory.getLogger(CoreAddonsImpl.class);

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public VirtualRepo createVirtualRepo(InternalRepositoryService repoService, VirtualRepoDescriptor descriptor) {
        return new VirtualRepo(descriptor, repoService);
    }

    @Override
    public void importKeyStore(ImportSettings settings) {
        // do nothing
    }

    @Override
    public void exportKeyStore(ExportSettings exportSettings) {
        // do nothing
    }

    @Override
    public void addExternalGroups(String userName, Set<UserGroupInfo> groups) {
        // nop
    }

    @Override
    public void populateGroups(DirContextOperations dirContextOperations, MutableUserInfo userInfo) {
        // do nothing
    }

    @Override
    public void populateGroups(String dn, MutableUserInfo info) {
        // do nothing
    }

    @Override
    public List<LdapSetting> getEnabledLdapSettings() {
        CentralConfigDescriptor descriptor = ContextHelper.get().beanForType(
                CentralConfigService.class).getDescriptor();
        List<LdapSetting> enabledLdapSettings = descriptor.getSecurity().getEnabledLdapSettings();
        if (enabledLdapSettings != null && !enabledLdapSettings.isEmpty()) {
            return Lists.newArrayList(enabledLdapSettings.get(0));
        }
        return Lists.newArrayList();
    }

    @Override
    public List<FilterBasedLdapUserSearch> getLdapUserSearches(ContextSource ctx, LdapSetting settings) {
        SearchPattern searchPattern = settings.getSearch();
        String searchBase = searchPattern.getSearchBase();
        if (searchBase == null) {
            searchBase = "";
        }
        ArrayList<FilterBasedLdapUserSearch> result = new ArrayList<>();
        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(searchBase,
                searchPattern.getSearchFilter(), (BaseLdapPathContextSource) ctx);
        userSearch.setSearchSubtree(searchPattern.isSearchSubTree());
        result.add(userSearch);
        return result;
    }

    @Override
    public void performOnBuildArtifacts(Build build) {
        // NOP
    }

    @Override
    public void addPropertySetToRepository(RealRepoDescriptor descriptor) {
        // NOP
    }

    @Override
    public void importLicenses(ImportSettings settings) {
        // NOP
    }

    @Override
    public void exportLicenses(ExportSettings exportSettings) {
        // nop
    }

    @Override
    public List findLicensesInRepos(List<String> repoKeys, LicenseStatus status) {
        return Lists.newArrayList();
    }

    @Override
    public void reloadLicensesCache() {
    }

    @Override
    public Properties getProperties(RepoPath repoPath) {
        return (Properties) InfoFactoryHolder.get().createProperties();
    }

    @Override
    public Map<RepoPath, Properties> getProperties(Set<RepoPath> repoPaths) {
        return Maps.newHashMap();
    }

    @Override
    public void deleteProperty(RepoPath repoPath, String property) {
        // nop
    }

    @Override
    public void addProperty(RepoPath repoPath, PropertySet propertySet, Property property, String... values) {
        //nop
    }

    @Override
    public RepoResource assembleDynamicMetadata(InternalRequestContext context, RepoPath metadataRepoPath) {
        return new FileResource(ContextHelper.get().getRepositoryService().getFileInfo(metadataRepoPath));
    }

    @Override
    public boolean isFilteredResourceFile(RepoPath repoPath) {
        return false;
    }

    @Override
    public boolean isFilteredResourceFile(RepoPath repoPath, Properties props) {
        return false;
    }

    @Override
    public RepoResource getFilteredResource(Request request, FileInfo fileInfo, InputStream fileInputStream) {
        return new UnfoundRepoResource(fileInfo.getRepoPath(),
                "Creation of a filtered resource requires the Properties add-on.", HttpStatus.SC_FORBIDDEN);
    }

    @Override
    public RepoResource getZipResource(Request request, FileInfo fileInfo, InputStream stream) {
        return new UnfoundRepoResource(fileInfo.getRepoPath(),
                "Direct resource download from zip requires the Filtered resources add-on.", HttpStatus.SC_FORBIDDEN);
    }

    @Override
    public ResourceStreamHandle getZipResourceHandle(RepoResource resource, InputStream stream) {
        throw new UnsupportedOperationException(
                "Direct resource download from zip requires the Filtered resources add-on.");
    }

    @Override
    public String filterResource(Request request, Properties contextProperties, Reader reader) throws Exception {
        try {
            return IOUtils.toString(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public void toggleResourceFilterState(RepoPath repoPath, boolean filtered) {
    }

    @Override
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

    @Override
    public boolean canCrossLayouts(RepoLayout source, RepoLayout target) {
        return false;
    }

    @Override
    public void performCrossLayoutMoveOrCopy(MoveMultiStatusHolder status, MoverConfig moverConfig,
            LocalRepo sourceRepo, LocalRepo targetLocalRepo, VfsItem sourceItem) {
        throw new UnsupportedOperationException(
                "Cross layout move or copy operations require the Repository Layouts addon.");
    }

    @Override
    public String translateArtifactPath(RepoLayout sourceRepoLayout, RepoLayout targetRepoLayout, String path) {
        return translateArtifactPath(sourceRepoLayout, targetRepoLayout, path, null);
    }

    @Override
    public String translateArtifactPath(RepoLayout sourceRepoLayout, RepoLayout targetRepoLayout, String path,
            @Nullable MultiStatusHolder multiStatusHolder) {
        return path;
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

    @Override
    public MultiStatusHolder performRemoteReplication(RemoteReplicationSettings settings) {
        return getReplicationRequiredStatusHolder();
    }

    @Override
    public MultiStatusHolder performLocalReplication(LocalReplicationSettings settings) {
        return getReplicationRequiredStatusHolder();
    }

    @Override
    public void scheduleImmediateLocalReplicationTask(LocalReplicationDescriptor replicationDescriptor,
            MultiStatusHolder statusHolder) {
        statusHolder.error("Error: the replication addon is required for this operation.", HttpStatus.SC_BAD_REQUEST,
                log);
    }

    @Override
    public void scheduleImmediateRemoteReplicationTask(RemoteReplicationDescriptor replicationDescriptor,
            MultiStatusHolder statusHolder) {
        statusHolder.error("Error: the replication addon is required for this operation.", HttpStatus.SC_BAD_REQUEST,
                log);
    }

    @Override
    public ReplicationStatus getReplicationStatus(RepoPath repoPath) {
        return new ReplicationStatus(ReplicationStatusType.ERROR, "Error");
    }

    @Override
    public void offerLocalReplicationDeploymentEvent(RepoPath repoPath) {
    }

    @Override
    public void offerLocalReplicationMkDirEvent(RepoPath repoPath) {
    }

    @Override
    public void offerLocalReplicationDeleteEvent(RepoPath repoPath) {
    }

    @Override
    public void offerLocalReplicationPropertiesChangeEvent(RepoPath repoPath) {
    }

    @Override
    public void validateTargetIsDifferentInstance(ReplicationBaseDescriptor descriptor,
            RealRepoDescriptor repoDescriptor) throws IOException {
    }

    private MultiStatusHolder getReplicationRequiredStatusHolder() {
        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();
        multiStatusHolder.error("Error: the replication addon is required for this operation.",
                HttpStatus.SC_BAD_REQUEST, log);
        return multiStatusHolder;
    }

    @Override
    public void requestAsyncRepositoryYumMetadataCalculation(RepoPath... repoPaths) {
    }

    @Override
    public void requestAsyncRepositoryYumMetadataCalculation(LocalRepoDescriptor repo) {
    }

    @Override
    public void requestYumMetadataCalculation(LocalRepoDescriptor repo) {
    }

    @Override
    public void extractNuPkgInfo(FileInfo fileInfo, MutableStatusHolder statusHolder, boolean addToCache) {
    }

    @Override
    public void extractNuPkgInfoSynchronously(FileInfo file, MutableStatusHolder statusHolder) {
    }

    @Override
    public void addNuPkgToRepoCache(RepoPath repoPath, Properties properties) {
    }

    @Override
    public void removeNuPkgFromRepoCache(String repoKey, String packageId, String packageVersion) {
    }

    @Override
    public void internalAddNuPkgToRepoCache(RepoPath repoPath, Properties properties) {
    }

    @Override
    public void internalRemoveNuPkgFromRepoCache(String repoKey, String packageId, String packageVersion) {
    }

    @Nonnull
    @Override
    public RemoteRepo createRemoteRepo(InternalRepositoryService repoService, RemoteRepoDescriptor repoDescriptor,
            boolean offlineMode, RemoteRepo oldRemoteRepo) {
        return new HttpRepo((HttpRepoDescriptor) repoDescriptor, repoService, offlineMode, oldRemoteRepo);
    }

    @Override
    public void deployArchiveBundle(ArtifactoryRequest request, ArtifactoryResponse response, LocalRepo repo)
            throws IOException {
        response.sendError(HttpStatus.SC_BAD_REQUEST, "This REST API is available only in Artifactory Pro.", log);
    }

    @Override
    public InternalRequestContext getDynamicVersionContext(Repo repo, InternalRequestContext originalRequestContext,
            boolean isRemote) {
        return originalRequestContext;
    }

    @Override
    public boolean isCrowdAuthenticationSupported(Class<?> authentication) {
        return false;
    }

    @Override
    public Authentication authenticateCrowd(Authentication authentication) {
        throw new UnsupportedOperationException("This feature requires the Crowd SSO addon.");
    }

    @Override
    public boolean findUser(String userName) {
        return false;
    }

    @Override
    public FileComplianceInfo getExternalInfoFromMetadata(RepoPath repoPath) {
        throw new UnsupportedOperationException("This feature requires the Black Duck addon.");
    }

    @Override
    public void performBlackDuckOnBuildArtifacts(Build build) {
        // NOP
    }

    @Override
    public void reindexAsync(String repoKey) {
    }

    @Override
    public void afterRepoInit(String repoKey) {
    }

    @Override
    public void requestAsyncReindexNuPkgs(String repoKey) {
    }

    @Override
    public boolean isHaEnabled() {
        return false;
    }

    @Override
    public boolean isPrimary() {
        return true;
    }

    @Override
    public boolean isHaConfigured() {
        try {
            return ArtifactoryHome.get().isHaConfigured();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void notify(HaMessageTopic haMessageTopic, HaMessage haMessage) {
    }

    @Override
    public String getHostId() {
        return HttpUtils.getHostId();
    }

    @Override
    public void updateArtifactoryServerRole() {
    }

    @Override
    public boolean deleteArtifactoryServer(String serverId) {
        return false;
    }

    @Override
    public void propagateTaskToPrimary(Task Task) {
    }

    @Override
    public LockProvider getLockProvider() {
        return new JVMLockProvider();
    }

    @Override
    public boolean tryLockRemoteDownload(String path, long leaseTime, TimeUnit timeUnit) {
        throw new UnsupportedOperationException("No locks for Non-High-Availability node");
    }

    @Override
    public void unlockRemoteDownload(String path) {
    }

    @Override
    public void init() {
    }

    @Override
    public FsItemsVault getFsItemVault() {
        LockProvider lockProvider = new JVMLockProvider();
        return new FsItemsVaultCacheImpl(lockProvider);
    }

    @Override
    public SemaphoreWrapper getSemaphore(String semaphoreName) {
        Semaphore semaphore = new Semaphore(HaCommonAddon.DEFAULT_SEMAPHORE_PERMITS);
        return new JVMSemaphoreWrapper(semaphore);
    }

    @Override
    public void shutdown() {
    }
}
