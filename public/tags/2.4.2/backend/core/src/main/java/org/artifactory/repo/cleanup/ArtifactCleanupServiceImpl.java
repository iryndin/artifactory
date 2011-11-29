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

package org.artifactory.repo.cleanup;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.search.xml.metadata.stats.StatsSearchControls;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.fs.StatsInfo;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.BaseTaskServiceDescriptorHandler;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.FileUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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
    private InternalRepositoryService repositoryService;

    @Autowired
    private SearchService searchService;

    public void init() {
        reload(null);
    }

    public void reload(@Nullable CentralConfigDescriptor oldDescriptor) {
        List<LocalCacheRepoDescriptor> oldCacheRepoDescriptors = Lists.newArrayList();
        if (oldDescriptor != null) {
            Map<String, RemoteRepoDescriptor> remoteRepositoriesMap = oldDescriptor.getRemoteRepositoriesMap();
            for (RemoteRepoDescriptor remoteRepoDescriptor : remoteRepositoriesMap.values()) {
                LocalCacheRepoDescriptor descriptor = new LocalCacheRepoDescriptor();
                descriptor.setDescription(remoteRepoDescriptor.getDescription() + " (local file cache)");
                descriptor.setKey(remoteRepoDescriptor.getKey() + LocalCacheRepo.PATH_SUFFIX);
                descriptor.setRemoteRepo(remoteRepoDescriptor);
                oldCacheRepoDescriptors.add(descriptor);
            }
        }
        new LocalCacheDescriptorHandler(repositoryService.getCachedRepoDescriptors(),
                oldCacheRepoDescriptors).reschedule();
    }

    public void destroy() {
        new LocalCacheDescriptorHandler(null, null).unschedule();
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    static class LocalCacheDescriptorHandler extends BaseTaskServiceDescriptorHandler<LocalCacheRepoDescriptor> {
        final List<LocalCacheRepoDescriptor> newCacheRepoDescriptors = Lists.newArrayList();
        final List<LocalCacheRepoDescriptor> oldCacheRepoDescriptors = Lists.newArrayList();
        final int cleanupIntervalHours;

        public String jobName() {
            return "Artifact Cleanup";
        }

        LocalCacheDescriptorHandler(List<LocalCacheRepoDescriptor> newCacheRepoDescriptors,
                List<LocalCacheRepoDescriptor> oldCacheRepoDescriptors) {
            cleanupIntervalHours = ConstantValues.repoCleanupIntervalHours.getInt();
            if (cleanupIntervalHours < 1) {
                throw new IllegalArgumentException(
                        "Remote repository cache cleanup interval hours cannot be less than 1.");
            }
            if (newCacheRepoDescriptors != null) {
                this.newCacheRepoDescriptors.addAll(newCacheRepoDescriptors);
            }
            if (oldCacheRepoDescriptors != null) {
                this.oldCacheRepoDescriptors.addAll(oldCacheRepoDescriptors);
            }
        }

        public List<LocalCacheRepoDescriptor> getNewDescriptors() {
            return this.newCacheRepoDescriptors;
        }

        public List<LocalCacheRepoDescriptor> getOldDescriptors() {
            return this.oldCacheRepoDescriptors;
        }

        public Predicate<Task> getAllPredicate() {
            return new Predicate<Task>() {
                public boolean apply(@Nullable Task input) {
                    return input == null || ArtifactCleanupJob.class.isAssignableFrom(input.getType());
                }
            };
        }

        public Predicate<Task> getPredicate(@Nonnull final LocalCacheRepoDescriptor descriptor) {
            return new Predicate<Task>() {
                public boolean apply(@Nullable Task input) {
                    return input == null || (
                            ArtifactCleanupJob.class.isAssignableFrom(input.getType()) &&
                                    descriptor.getKey().equals(input.getAttribute(Task.REPO_KEY))
                    );
                }
            };
        }

        public LocalCacheRepoDescriptor findOldFromNew(@Nonnull LocalCacheRepoDescriptor newDescriptor) {
            for (LocalCacheRepoDescriptor oldLocalCacheDescriptor : oldCacheRepoDescriptors) {
                if (oldLocalCacheDescriptor.getKey().equals(newDescriptor.getKey())) {
                    return oldLocalCacheDescriptor;
                }
            }
            return null;
        }

        public void activate(@Nonnull LocalCacheRepoDescriptor descriptor, boolean manual) {
            int periodHours = descriptor.getRemoteRepo().getUnusedArtifactsCleanupPeriodHours();
            if (periodHours > 0) {
                long interval = getHoursInMillies(cleanupIntervalHours);
                TaskBase task = TaskUtils.createRepeatingTask(ArtifactCleanupJob.class,
                        interval,
                        FileUtils.nextLong(interval));
                String cachedRepoKey = descriptor.getKey();
                task.addAttribute(Task.REPO_KEY, cachedRepoKey);
                task.addAttribute(ArtifactCleanupJob.PERIOD_MILLIS, getHoursInMillies(periodHours));
                InternalContextHelper.get().getBean(TaskService.class).startTask(task, false);
                log.info("Scheduled auto-cleanup to run every {} hours on {}.", cleanupIntervalHours,
                        cachedRepoKey);
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

        if (periodMillis <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Cannot cleanup repository  '%s' based on wrong period %s.", repoKey, periodMillis));
        }

        log.info("Auto-clean has begun on the repository '{}' with period of {} millis.", repoKey, periodMillis);

        //Calculate unused artifact expiry
        long expiryMillis = (System.currentTimeMillis() - periodMillis);

        //Perform a metadata search on the given repo. Look for artifacts that have lastDownloaded stats
        StatsSearchControls searchControls = new StatsSearchControls();
        searchControls.addRepoToSearch(repoKey);
        searchControls.setValue(JcrHelper.getCalendar(expiryMillis));
        ItemSearchResults<GenericMetadataSearchResult<StatsInfo>> metadataSearchResults =
                searchService.searchArtifactsNotDownloadedSince(searchControls);

        int iterationCount = 0;
        int cleanedArtifactsCount = 0;
        for (GenericMetadataSearchResult<StatsInfo> metadataSearchResult : metadataSearchResults.getResults()) {
            if ((++iterationCount % 10 == 0) && TaskUtils.pauseOrBreak()) {
                throw new TaskInterruptedException();
            }
            StatsInfo statsInfo = metadataSearchResult.getMetadataObject();
            RepoPath repoPath = metadataSearchResult.getItemInfo().getRepoPath();
            long lastDownloaded = statsInfo.getLastDownloaded();

            //If the artifact wasn't downloaded within the expiry window, remove it
            if (expiryMillis > lastDownloaded) {
                try {
                    storingRepo.undeploy(repoPath, false);  // no need for maven metadata calculation on cache repos
                    cleanedArtifactsCount++;
                } catch (Exception e) {
                    log.error(String.format("Could not auto-clean artifact '%s'.", repoPath.getId()), e);
                }
            } else {
                log.warn("Querying for unused artifacts returned a used one " + repoPath);
            }
        }

        log.info("Auto-clean on the repository '{}' has ended. {} artifact(s) were cleaned",
                repoKey, cleanedArtifactsCount);
    }
}
