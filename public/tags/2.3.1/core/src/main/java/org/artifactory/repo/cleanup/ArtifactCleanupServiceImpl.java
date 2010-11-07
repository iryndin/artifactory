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

package org.artifactory.repo.cleanup;

import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The main implementation of the clean-up service
 *
 * @author Noam Tenne
 */
@Service
@Reloadable(beanClass = InternalArtifactCleanupService.class,
        initAfter = {TaskService.class, InternalRepositoryService.class})
public class ArtifactCleanupServiceImpl implements InternalArtifactCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ArtifactCleanupServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private SearchService searchService;

    public void init() {
        scheduleCleanup();
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
        LocalRepo storingRepo = (LocalRepo) repositoryService.repositoryByKey(repoKey);

        //Perform sanity checks
        if (descriptor == null) {
            log.warn("Could not find the repository '{}' - auto-clean was not performed.", repoKey);
            return;
        }
        if (storingRepo == null) {
            log.warn("Could not find the storing repository '{}' - auto-clean was not performed.", repoKey);
            return;
        }
        if (!descriptor.isCache() || !storingRepo.isCache()) {
            throw new IllegalArgumentException(String.format("Cannot cleanup non-cache repository '%s'.", repoKey));
        }

        log.debug("Auto-clean has begun on the repository '{}'.", repoKey);

        //Calculate unused artifact expiry
        long expiryMillis = (System.currentTimeMillis() - periodMillis);

        //Perform a metadata search on the given repo. Look for artifacts that have lastDownloaded stats
        GenericMetadataSearchControls<StatsInfo> searchControls =
                new GenericMetadataSearchControls<StatsInfo>(StatsInfo.class);
        searchControls.addRepoToSearch(repoKey);
        searchControls.setPropertyName("lastDownloaded");
        searchControls.setOperation(GenericMetadataSearchControls.Operation.LT);
        searchControls.setValue(JcrHelper.getCalendar(expiryMillis));
        SearchResults<GenericMetadataSearchResult<StatsInfo>> metadataSearchResults =
                searchService.searchGenericMetadata(searchControls);

        for (GenericMetadataSearchResult<StatsInfo> metadataSearchResult : metadataSearchResults.getResults()) {
            StatsInfo statsInfo = metadataSearchResult.getMetadataObject();
            RepoPath repoPath = metadataSearchResult.getItemInfo().getRepoPath();
            long lastDownloaded = statsInfo.getLastDownloaded();

            //If the artifact wasn't downloaded within the expiry window, remove it
            if (expiryMillis > lastDownloaded) {
                try {
                    storingRepo.undeploy(repoPath, false);  // no need for maven metadata calculation on cache repos
                } catch (Exception e) {
                    log.error(String.format("Could not auto-clean artifact '%s'.", repoPath.getId()), e);
                }
            } else {
                log.warn("Querying for unused artifacts returned a used one " + repoPath);
            }
        }

        log.debug("Auto-clean on the repository '{}' has ended.", repoKey);
    }

    /**
     * Schedules the auto-cleaner job for remote repository caches that were configured to
     */
    private void scheduleCleanup() {
        taskService.stopTasks(ArtifactCleanupJob.class, false);

        int cleanupIntervalHours = ConstantValues.repoCleanupIntervalHours.getInt();
        if (cleanupIntervalHours < 1) {
            throw new IllegalArgumentException("Remote repository cache cleanup interval hours cannot be less than 1.");
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
