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

package org.artifactory.maven.index;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.lucene.store.FSDirectory;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.io.TempFileStreamHandle;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.schedule.BaseTaskServiceDescriptorHandler;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.Files;
import org.artifactory.util.Pair;
import org.artifactory.version.CompoundVersionDetails;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * @author Yoav Landman
 */
@Service
@Reloadable(beanClass = InternalMavenIndexerService.class,
        initAfter = {TaskService.class, InternalRepositoryService.class})
public class MavenIndexerServiceImpl implements InternalMavenIndexerService {
    private static final Logger log = LoggerFactory.getLogger(MavenIndexerServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Override
    public void init() {
        new IndexerSchedulerHandler(getAndCheckDescriptor(), null).reschedule();
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        new IndexerSchedulerHandler(getAndCheckDescriptor(), oldDescriptor.getIndexer()).reschedule();
    }

    private IndexerDescriptor getAndCheckDescriptor() {
        IndexerDescriptor descriptor = centralConfig.getDescriptor().getIndexer();
        if (descriptor != null) {
            SortedSet<RepoBaseDescriptor> set = new TreeSet<>();
            if (descriptor.getExcludedRepositories() == null) {
                //Auto exclude all remote and virtual repos
                set.addAll(repositoryService.getRemoteRepoDescriptors());
                set.addAll(getAllVirtualReposExceptGlobal());
            } else {
                set.addAll(descriptor.getExcludedRepositories());
                // Always remove globalVirtual one
                VirtualRepoDescriptor dummyGlobal = new VirtualRepoDescriptor();
                dummyGlobal.setKey(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY);
                set.remove(dummyGlobal);
            }
            descriptor.setExcludedRepositories(set);
        }
        return descriptor;
    }

    static class IndexerSchedulerHandler extends BaseTaskServiceDescriptorHandler<IndexerDescriptor> {
        final List<IndexerDescriptor> oldDescriptorHolder = Lists.newArrayList();
        final List<IndexerDescriptor> newDescriptorHolder = Lists.newArrayList();

        IndexerSchedulerHandler(IndexerDescriptor newDesc, IndexerDescriptor oldDesc) {
            if (newDesc != null) {
                newDescriptorHolder.add(newDesc);
            }
            if (oldDesc != null) {
                oldDescriptorHolder.add(oldDesc);
            }
        }

        @Override
        public String jobName() {
            return "Indexer";
        }

        @Override
        public List<IndexerDescriptor> getNewDescriptors() {
            return newDescriptorHolder;
        }

        @Override
        public List<IndexerDescriptor> getOldDescriptors() {
            return oldDescriptorHolder;
        }

        @Override
        public Predicate<Task> getAllPredicate() {
            return new Predicate<Task>() {
                @Override
                public boolean apply(Task input) {
                    return AbstractMavenIndexerJobs.class.isAssignableFrom(input.getType());
                }
            };
        }

        @Override
        public Predicate<Task> getPredicate(@Nonnull IndexerDescriptor descriptor) {
            return getAllPredicate();
        }

        @Override
        public void activate(@Nonnull IndexerDescriptor descriptor, boolean manual) {
            String cronExp = descriptor.getCronExp();
            if (descriptor.isEnabled() && cronExp != null) {
                TaskBase task = TaskUtils.createCronTask(MavenIndexerJob.class, cronExp);
                // Passing null for repo keys because they are taken from the indexer descriptor
                MavenIndexerRunSettings settings = new MavenIndexerRunSettings(false, false, null);
                task.addAttribute(MavenIndexerJob.SETTINGS, settings);
                InternalContextHelper.get().getBean(TaskService.class).startTask(task, false, manual);
                log.info("Indexer activated with cron expression '{}'.", cronExp);
            } else {
                log.debug("No indexer cron expression is configured. Indexer will be disabled.");
            }
        }

        @Override
        public IndexerDescriptor findOldFromNew(@Nonnull IndexerDescriptor newDescriptor) {
            return oldDescriptorHolder.isEmpty() ? null : oldDescriptorHolder.get(0);
        }
    }

    @Override
    public void scheduleImmediateIndexing(MutableStatusHolder statusHolder) {
        scheduleIndexer(statusHolder, new MavenIndexerRunSettings(true, false, null));
    }

    @Override
    public void runSpecificIndexer(MutableStatusHolder statusHolder, List<String> repoKeys,
            boolean forceRemoteDownload) {
        scheduleIndexer(statusHolder, new MavenIndexerRunSettings(true, forceRemoteDownload, repoKeys));
    }

    private void scheduleIndexer(MutableStatusHolder statusHolder, MavenIndexerRunSettings settings) {
        taskService.checkCanStartManualTask(MavenIndexerJob.class, statusHolder);
        if (!statusHolder.isError()) {
            try {
                StringBuilder logMessageBuilder = new StringBuilder("Activating indexer ");
                List<String> repoKeys = settings.getRepoKeys();
                if ((repoKeys != null) && !repoKeys.isEmpty()) {
                    logMessageBuilder.append("for repo '").append(Arrays.toString(repoKeys.toArray())).append("' ");
                }
                logMessageBuilder.append("manually");
                log.info(logMessageBuilder.toString());
                TaskBase task = TaskUtils.createManualTask(MavenIndexerJob.class, 0L);
                task.addAttribute(MavenIndexerJob.SETTINGS, settings);
                taskService.startTask(task, true, true);
            } catch (Exception e) {
                log.error("Error scheduling the indexer.", e);
            }
        }
    }

    @Override
    public void destroy() {
        new IndexerSchedulerHandler(null, null).unschedule();
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @Override
    public void index(MavenIndexerRunSettings settings) {
        IndexerDescriptor descriptor = getAndCheckDescriptor();
        if (!settings.isForceRemoteDownload() && !descriptor.isEnabled() && !settings.isManualRun()) {
            log.debug("Indexer is disabled - doing nothing.");
            return;
        }

        Set<? extends RepoDescriptor> excludedRepositories;
        List<String> repoKeys = settings.getRepoKeys();
        if ((repoKeys == null) || repoKeys.isEmpty()) {
            excludedRepositories = descriptor.getExcludedRepositories();
        } else {
            // everything is excluded besides this one repo
            excludedRepositories = calcSpecificRepoForIndexing(settings.getRepoKeys());
        }

        log.info("Starting non virtual repositories indexing...");
        List<RealRepo> indexedRepos = getNonVirtualRepositoriesToIndex(excludedRepositories);
        log.info("Non virtual repositories to index: {}", indexedRepos);
        //Do the indexing work
        for (RealRepo indexedRepo : indexedRepos) {
            //Check if we need to stop/suspend
            if (taskService.pauseOrBreak()) {
                log.info("Stopped indexing on demand");
                return;
            }
            MavenIndexManager mavenIndexManager = new MavenIndexManager(indexedRepo);
            try {
                //Execute separate tasks in order to have shorter transactions - can be done in a more elegant way...
                findOrCreateRepositoryIndex(settings.getFireTime(), settings.isForceRemoteDownload(),
                        mavenIndexManager);
                //Check again if we need to stop/suspend
                if (taskService.pauseOrBreak()) {
                    log.info("Stopped indexing on demand");
                    return;
                }
                saveIndex(mavenIndexManager);
            } catch (Exception e) {
                //If we failed to index because of a socket timeout, issue a terse warning instead of a complete stack
                //trace
                Throwable cause = ExceptionUtils.getCauseOfTypes(e, SocketTimeoutException.class);
                if (cause != null) {
                    log.warn("Indexing for repo '" + indexedRepo.getKey() + "' failed: " + e.getMessage() + ".");
                } else {
                    //Just report - don't stop indexing of other repos
                    log.error("Indexing for repo '" + indexedRepo.getKey() + "' failed.", e);
                }
            }
        }
        getTransactionalMe().mergeVirtualRepoIndexes(excludedRepositories, indexedRepos);
        log.info("Finished repositories indexing...");
    }

    private Set<? extends RepoDescriptor> calcSpecificRepoForIndexing(@Nullable final List<String> repoKeys) {
        Set<RepoBaseDescriptor> excludedRepos = Sets.newHashSet();
        excludedRepos.addAll(repositoryService.getLocalRepoDescriptors());
        excludedRepos.addAll(repositoryService.getRemoteRepoDescriptors());
        excludedRepos.addAll(getAllVirtualReposExceptGlobal());
        if ((repoKeys != null) && !repoKeys.isEmpty()) {
            Iterables.removeIf(excludedRepos, new Predicate<RepoBaseDescriptor>() {
                @Override
                public boolean apply(@Nullable RepoBaseDescriptor repoBaseDescriptor) {
                    if (repoBaseDescriptor == null) {
                        return false;
                    }
                    return repoKeys.contains(repoBaseDescriptor.getKey());
                }
            });
        }

        return excludedRepos;
    }

    private void findOrCreateRepositoryIndex(Date fireTime, boolean forceRemoteDownload,
            MavenIndexManager mavenIndexManager) {
        TaskBase taskFindOrCreateIndex = TaskUtils.createManualTask(FindOrCreateMavenIndexJob.class, 0L);
        taskFindOrCreateIndex.addAttribute(MavenIndexManager.class.getName(), mavenIndexManager);
        taskFindOrCreateIndex.addAttribute(Date.class.getName(), fireTime);
        taskFindOrCreateIndex.addAttribute(AbstractMavenIndexerJobs.FORCE_REMOTE, forceRemoteDownload);
        taskService.startTask(taskFindOrCreateIndex, true);
        taskService.waitForTaskCompletion(taskFindOrCreateIndex.getToken());
    }

    private void saveIndex(MavenIndexManager mavenIndexManager) {
        TaskBase saveIndexFileTask = TaskUtils.createManualTask(SaveMavenIndexFileJob.class, 0L);
        saveIndexFileTask.addAttribute(MavenIndexManager.class.getName(), mavenIndexManager);
        taskService.startTask(saveIndexFileTask, true);
        //No real need to wait, but since other task are waiting for indexer completion, leaving it
        taskService.waitForTaskCompletion(saveIndexFileTask.getToken());
    }

    private List<RealRepo> getNonVirtualRepositoriesToIndex(
            @Nullable Set<? extends RepoDescriptor> excludedRepositories) {
        List<RealRepo> realRepositories = repositoryService.getLocalAndRemoteRepositories();
        List<RealRepo> indexedRepos = new ArrayList<>();
        //Skip excluded repositories and remote repositories that are currently offline
        for (RealRepo repo : realRepositories) {
            boolean excluded = false;
            if (excludedRepositories != null) {
                for (RepoDescriptor excludedRepo : excludedRepositories) {
                    if (excludedRepo.getKey().equals(repo.getKey())) {
                        excluded = true;
                        break;
                    }
                }
            }
            if (!excluded) {
                indexedRepos.add(repo);
            }
        }
        return indexedRepos;
    }

    @Override
    public void mergeVirtualRepoIndexes(Set<? extends RepoDescriptor> excludedRepositories,
            List<RealRepo> indexedRepos) {
        List<VirtualRepo> virtualRepos = filterExcludedVirtualRepos(excludedRepositories);
        log.info("Virtual repositories to index: {}", virtualRepos);
        //Keep a list of extracted index dirs for all the local repo indexes for merging
        Map<StoringRepo, FSDirectory> extractedLocalRepoIndexes = new HashMap<>();
        try {
            //Merge virtual repo indexes
            for (VirtualRepo virtualRepo : virtualRepos) {
                //Check if we need to stop/suspend
                if (taskService.pauseOrBreak()) {
                    log.info("Stopped indexing on demand");
                    return;
                }
                Set<LocalRepo> localRepos = new HashSet<>();
                localRepos.addAll(virtualRepo.getResolvedLocalRepos());
                localRepos.addAll(virtualRepo.getResolvedLocalCachedRepos());
                //Create a temp lucene dir and merge each local into it
                ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
                File dir = Files.createRandomDir(artifactoryHome.getTempWorkDir(), "artifactory.merged-index");
                RepoIndexer indexer = null;
                try {
                    indexer = new RepoIndexer(virtualRepo);
                    indexer.createContext(dir);

                    log.info("Creating virtual repository index '{}'", virtualRepo);
                    //Take the local index from each relevant indexed repo (for remote take local cache)
                    for (RealRepo indexedRepo : indexedRepos) {
                        //Check if we need to stop/suspend
                        if (taskService.pauseOrBreak()) {
                            log.info("Stopped indexing on demand");
                            return;
                        }
                        LocalRepo localRepo = indexedRepo.isLocal() ? (LocalRepo) indexedRepo :
                                ((RemoteRepo) indexedRepo).getLocalCacheRepo();
                        try {
                            //Extract aside the index from the local repo
                            if (localRepos.contains(localRepo)) {
                                log.debug("Merging index of '{}' to index of virtual repo '{}'", localRepo,
                                        virtualRepo);
                                indexer.mergeInto(localRepo, extractedLocalRepoIndexes);
                            }
                        } catch (Exception e) {
                            log.warn("Could not merge index of local repo '{}' into virtual repo '{}'",
                                    localRepo.getKey(), virtualRepo.getKey());
                        }
                    }
                    //Store the index into the virtual repo
                    //Get the last gz and props and store them - we need to return them or create them from the dir
                    Pair<TempFileStreamHandle, TempFileStreamHandle> tempFileStreamHandlesPair = indexer.createIndex(
                            dir,
                            false);
                    ResourceStreamHandle indexHandle = tempFileStreamHandlesPair.getFirst();
                    ResourceStreamHandle properties = tempFileStreamHandlesPair.getSecond();
                    MavenIndexManager mavenIndexManager =
                            new MavenIndexManager(indexer.getRepo(), indexHandle, properties);
                    mavenIndexManager.saveIndexFiles();
                } finally {
                    if (indexer != null) {
                        indexer.removeTempIndexFiles(dir);
                    }
                    org.apache.commons.io.FileUtils.deleteQuietly(dir);
                }
            }
        } catch (Exception e) {
            log.error("Could not merge virtual repository indexes.", e);
        } finally {
            //Delete temp extracted dirs
            for (FSDirectory directory : extractedLocalRepoIndexes.values()) {
                org.apache.commons.io.FileUtils.deleteQuietly(directory.getFile());
            }
        }
    }

    /**
     * Returns a filtered list of virtual repositories based on the excluded repository list
     *
     * @param excludedRepositories List of repositories excluded from the indexer
     * @return List of virtual repositories which weren't excluded
     */
    private List<VirtualRepo> filterExcludedVirtualRepos(Set<? extends RepoDescriptor> excludedRepositories) {
        List<VirtualRepo> virtualRepositories = repositoryService.getVirtualRepositories();
        List<VirtualRepo> virtualRepositoriesCopy = new ArrayList<>(virtualRepositories);
        for (RepoDescriptor excludedRepository : excludedRepositories) {
            String excludedKey = excludedRepository.getKey();
            for (VirtualRepo virtualRepository : virtualRepositories) {
                if (excludedKey.equals(virtualRepository.getKey())) {
                    virtualRepositoriesCopy.remove(virtualRepository);
                }
            }
        }
        return virtualRepositoriesCopy;
    }

    /**
     * Returns the complete list of virtual repository descriptors, apart from the global one (repo)
     *
     * @return List of all virtual repository descriptors apart from the global one
     */
    private List<VirtualRepoDescriptor> getAllVirtualReposExceptGlobal() {
        List<VirtualRepoDescriptor> virtualRepositoriesCopy =
                new ArrayList<>(repositoryService.getVirtualRepoDescriptors());
        VirtualRepoDescriptor dummyGlobal = new VirtualRepoDescriptor();
        dummyGlobal.setKey(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY);
        virtualRepositoriesCopy.remove(dummyGlobal);
        return virtualRepositoriesCopy;
    }

    private static InternalMavenIndexerService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalMavenIndexerService.class);
    }

    @JobCommand(manualUser = TaskUser.CURRENT)
    public static class FindOrCreateMavenIndexJob extends AbstractMavenIndexerJobs {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                MavenIndexManager mavenIndexManager =
                        (MavenIndexManager) callbackContext.getMergedJobDataMap().get(
                                MavenIndexManager.class.getName());
                Date fireTime = (Date) callbackContext.getMergedJobDataMap().get(Date.class.getName());
                boolean forceRemoteDownload = (Boolean) callbackContext.getMergedJobDataMap().get(
                        AbstractMavenIndexerJobs.FORCE_REMOTE);
                InternalMavenIndexerService indexer = InternalContextHelper.get().beanForType(
                        InternalMavenIndexerService.class);
                indexer.fetchOrCreateIndex(mavenIndexManager, fireTime, forceRemoteDownload);
            } catch (Exception e) {
                log.error("Indexing failed: {}", e.getMessage());
                log.debug("Indexing failed.", e);
            }
        }
    }

    @JobCommand(manualUser = TaskUser.CURRENT)
    public static class SaveMavenIndexFileJob extends AbstractMavenIndexerJobs {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                MavenIndexManager mavenIndexManager =
                        (MavenIndexManager) callbackContext.getMergedJobDataMap().get(
                                MavenIndexManager.class.getName());
                InternalMavenIndexerService indexer = getTransactionalMe();
                indexer.saveIndexFiles(mavenIndexManager);
            } catch (TaskInterruptedException e) {
                log.warn(e.getMessage());
            } catch (Exception e) {
                log.error("Saving index files failed.", e);
            }
        }
    }

    @Override
    public void fetchOrCreateIndex(MavenIndexManager mavenIndexManager, Date fireTime, boolean forceRemoteDownload) {
        boolean remoteIndexExists = mavenIndexManager.fetchRemoteIndex(forceRemoteDownload);
        mavenIndexManager.createLocalIndex(fireTime, remoteIndexExists);
    }

    @Override
    public void saveIndexFiles(MavenIndexManager mavenIndexManager) {
        mavenIndexManager.saveIndexFiles();
    }
}