/*
 * This file is part of Artifactory.
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

package org.artifactory.repo.cleanup;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * The main implementation of the clean-up service
 *
 * @author Noam Tenne
 */
@Service
public class ArtifactCleanupServiceImpl implements InternalArtifactCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactCleanupServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private SearchService searchService;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(InternalArtifactCleanupService.class);
    }

    public void init() {
        scheduleCleanup();
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{TaskService.class, InternalRepositoryService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        init();
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void clean(String repoKey, long periodMillis) {
        LocalRepoDescriptor descriptor = repositoryService.localOrCachedRepoDescriptorByKey(repoKey);

        //Perform sanity checks
        if (descriptor == null) {
            log.warn("Could no find the repository '{}' - auto-clean was not performed.", repoKey);
            return;
        }
        if (!descriptor.isCache()) {
            throw new IllegalArgumentException("Cannot cleanup a non-cache repository.");
        }
        log.debug("Auto-clean has begun on the repository '{}'", repoKey);
        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();

        //Perform a metadata search on the given repo. Look for artifacts that have lastDownloaded stats
        MetadataSearchControls<StatsInfo> searchControls = new MetadataSearchControls<StatsInfo>();
        searchControls.setRepoToSearch(repoKey);
        searchControls.setMetadataName(StatsInfo.ROOT);
        searchControls.setPath(StatsInfo.ROOT + "/lastDownloaded");
        searchControls.setMetadataObjectClass(StatsInfo.class);
        SearchResults<MetadataSearchResult> metadataSearchResults = searchService.searchMetadata(searchControls);

        int cleanedItemsCounter = 0;

        //Calculate unused artifact expiry
        long expiryMillis = (System.currentTimeMillis() - periodMillis);
        for (MetadataSearchResult metadataSearchResult : metadataSearchResults.getResults()) {
            StatsInfo statsInfo = (StatsInfo) metadataSearchResult.getMetadataObject();
            long lastDownloaded = statsInfo.getLastDownloaded();

            //If the artifact wasn't downloaded within the expiry window, remove it
            if (expiryMillis > lastDownloaded) {
                RepoPath repoPath = metadataSearchResult.getItemInfo().getRepoPath();
                //We need to write lock for delete, so release the read lock held by the fetching the query result first
                LockingHelper.releaseReadLock(repoPath);
                StatusHolder statusHolder = repositoryService.undeploy(repoPath);
                if (!statusHolder.isError()) {
                    cleanedItemsCounter++;
                    log.debug("The item '{}' has successfully been removed from the repository '{}' during auto-clean.",
                            repoPath.getId(), repoKey);
                }
                multiStatusHolder.merge(statusHolder);
            }
        }

        boolean warningsProduced = multiStatusHolder.hasWarnings();
        if (warningsProduced) {
            log.warn("Warnings have been produced while auto-cleaning the repository '{}'", repoKey);
        }
        boolean errorsProduced = multiStatusHolder.hasErrors();
        if (errorsProduced) {
            log.error("Errors have been produced while auto-cleaning the repository '{}'", repoKey);
        }

        if (!warningsProduced && !errorsProduced) {
            String cleanedItems = "";
            if (cleanedItemsCounter != 0) {
                cleanedItems = " of " + cleanedItemsCounter + " items";
            }
            log.info("The repository '{}' has been succesfully cleaned{}", repoKey, cleanedItems);
        }
    }

    /**
     * Schedules the auto-cleaner job for remote repository caches that were configured to
     */
    private void scheduleCleanup() {
        taskService.stopTasks(ArtifactCleanupJob.class, false);

        int cleanupIntervalHours = ConstantValues.repoCleanupIntervalHours.getInt();
        if (cleanupIntervalHours < 1) {
            throw new IllegalArgumentException(
                    "Remote repository cache clean-up interval hours cannot be less than 1.");
        }

        List<LocalCacheRepoDescriptor> cachedRepoDescriptors = repositoryService.getCachedRepoDescriptors();
        for (LocalCacheRepoDescriptor cachedRepoDescriptor : cachedRepoDescriptors) {
            RemoteRepoDescriptor remoteRepoDescriptor = cachedRepoDescriptor.getRemoteRepo();

            //If the remote repo is configured to auto clean
            if (remoteRepoDescriptor.isUnusedArtifactsCleanupEnabled()) {
                int periodHours = remoteRepoDescriptor.getUnusedArtifactsCleanupPeriodHours();

                //If the period hours are valid
                if (periodHours > 0) {
                    long interval = getHoursInMillies(cleanupIntervalHours);
                    QuartzTask task = new QuartzTask(ArtifactCleanupJob.class, interval, 0);
                    String cachedRepoKey = cachedRepoDescriptor.getKey();
                    task.addAttribute(ArtifactCleanupJob.REPO_KEY, cachedRepoKey);
                    task.addAttribute(ArtifactCleanupJob.PERIOD_MILLIS, getHoursInMillies(periodHours));
                    taskService.startTask(task);
                    log.info("Scheduled auto-cleanup to run every {} hours on {}.", cleanupIntervalHours,
                            cachedRepoKey);
                }
            }
        }
    }

    /**
     * Returns the given number of hours, in milliseconds
     *
     * @param hours Number of hours to convert
     * @return Number of hours - In milliseconds
     */
    private long getHoursInMillies(int hours) {
        return (hours * 60L * 60L * 1000L);
    }
}
