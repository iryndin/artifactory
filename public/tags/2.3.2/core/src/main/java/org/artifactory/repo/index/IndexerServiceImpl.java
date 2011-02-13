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

package org.artifactory.repo.index;

import org.apache.lucene.store.FSDirectory;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.repo.RepoBaseDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.FileUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Yoav Landman
 */
@Service
@Reloadable(beanClass = InternalIndexerService.class, initAfter = {TaskService.class, InternalRepositoryService.class})
public class IndexerServiceImpl implements InternalIndexerService {
    private static final Logger log = LoggerFactory.getLogger(IndexerServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    private IndexerDescriptor descriptor;

    public void init() {
        descriptor = centralConfig.getDescriptor().getIndexer();
        if (descriptor == null) {
            descriptor = new IndexerDescriptor();
        }
        if (descriptor.getExcludedRepositories() == null) {
            //Auto exclude all remote and virtual repos
            SortedSet<RepoBaseDescriptor> set = new TreeSet<RepoBaseDescriptor>();
            set.addAll(repositoryService.getRemoteRepoDescriptors());
            set.addAll(getAllVirtualReposExceptGlobal());
            descriptor.setExcludedRepositories(set);
        }

        if (descriptor.isEnabled()) {
            scheduleIndexing(false);
        } else {
            taskService.cancelTasks(IndexerJob.class, false);
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        init();
    }

    public void scheduleImmediateIndexing() {
        scheduleIndexing(true);
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void index(Date fireTime, boolean manualRun) {
        if (!descriptor.isEnabled() && !manualRun) {
            log.debug("Indexer is disabled - doing nothing.");
            return;
        }
        log.info("Starting repositories indexing...");
        Set<? extends RepoDescriptor> excludedRepositories = descriptor.getExcludedRepositories();
        List<RealRepo> indexedRepos = getNonVirtualRepositoriesToIndex(excludedRepositories);

        //Do the indexing work
        for (RealRepo indexedRepo : indexedRepos) {
            //Check if we need to stop/suspend
            if (taskService.pauseOrBreak()) {
                return;
            }
            MavenIndexManager mavenIndexManager = new MavenIndexManager(indexedRepo);
            try {
                //Execute separate tasks in order to have shorter transactions - can be done in a more elegant way...
                findOrCreateRepositoryIndex(fireTime, mavenIndexManager);
                //Check again if we need to stop/suspend
                if (taskService.pauseOrBreak()) {
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

    private void findOrCreateRepositoryIndex(Date fireTime, MavenIndexManager mavenIndexManager) {
        QuartzTask taskFindOrCreateIndex = new QuartzTask(FindOrCreateIndexJob.class, "FindOrCreateIndex");
        taskFindOrCreateIndex.addAttribute(MavenIndexManager.class.getName(), mavenIndexManager);
        taskFindOrCreateIndex.addAttribute(Date.class.getName(), fireTime);
        taskService.startTask(taskFindOrCreateIndex);
        taskService.waitForTaskCompletion(taskFindOrCreateIndex.getToken());
    }

    private void saveIndex(MavenIndexManager mavenIndexManager) {
        QuartzTask saveIndexFileTask = new QuartzTask(SaveIndexFileJob.class, "SaveIndexFile");
        saveIndexFileTask.addAttribute(MavenIndexManager.class.getName(), mavenIndexManager);
        taskService.startTask(saveIndexFileTask);
        //No real need to wait, but since other task are waiting for indexer completion, leaving it
        taskService.waitForTaskCompletion(saveIndexFileTask.getToken());
    }

    private List<RealRepo> getNonVirtualRepositoriesToIndex(Set<? extends RepoDescriptor> excludedRepositories) {
        List<RealRepo> realRepositories = repositoryService.getLocalAndRemoteRepositories();
        List<RealRepo> indexedRepos = new ArrayList<RealRepo>();
        //Skip excluded repositories and remote repositories that are currently offline
        for (RealRepo repo : realRepositories) {
            boolean excluded = false;
            for (RepoDescriptor excludedRepo : excludedRepositories) {
                if (excludedRepo.getKey().equals(repo.getKey())) {
                    excluded = true;
                    break;
                }
            }
            boolean offlineRemote = false;
            if (!repo.isLocal()) {
                offlineRemote = ((RemoteRepo) repo).isOffline();
            }
            if (!excluded && !offlineRemote) {
                indexedRepos.add(repo);
            }
        }
        return indexedRepos;
    }

    public void mergeVirtualRepoIndexes(Set<? extends RepoDescriptor> excludedRepositories,
            List<RealRepo> indexedRepos) {
        List<VirtualRepo> virtualRepos = filterExcludedVirtualRepos(excludedRepositories);

        //Keep a list of extracted index dirs for all the local repo indexes for merging
        Map<StoringRepo, FSDirectory> extractedLocalRepoIndexes = new HashMap<StoringRepo, FSDirectory>();
        try {
            //Merge virtual repo indexes
            for (VirtualRepo virtualRepo : virtualRepos) {
                //Check if we need to stop/suspend
                if (taskService.pauseOrBreak()) {
                    return;
                }
                Set<LocalRepo> localRepos = new HashSet<LocalRepo>();
                localRepos.addAll(virtualRepo.getResolvedLocalRepos());
                localRepos.addAll(virtualRepo.getResolvedLocalCachedRepos());
                //Create a temp lucene dir and merge each local into it
                ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
                File dir = FileUtils.createRandomDir(artifactoryHome.getWorkTmpDir(), "artifactory.merged-index");
                try {
                    FSDirectory virtualIndexMergeDir = FSDirectory.getDirectory(dir);
                    RepoIndexer indexer = new RepoIndexer(virtualRepo);
                    indexer.createContext(virtualIndexMergeDir);

                    //Take the local index from each relevant indexed repo (for remote take local cache)
                    for (RealRepo indexedRepo : indexedRepos) {
                        //Check if we need to stop/suspend
                        if (taskService.pauseOrBreak()) {
                            return;
                        }
                        LocalRepo localRepo = indexedRepo.isLocal() ? (LocalRepo) indexedRepo :
                                ((RemoteRepo) indexedRepo).getLocalCacheRepo();
                        //Extract aside the index from the local repo
                        if (localRepos.contains(localRepo)) {
                            indexer.mergeInto(localRepo, extractedLocalRepoIndexes);
                        }
                    }
                    //Store the index into the virtual repo
                    //Get the last gz and props and store them - we need to return them or create them from the dir
                    ResourceStreamHandle indexHandle = indexer.createIndex(virtualIndexMergeDir, false);
                    MavenIndexManager mavenIndexManager =
                            new MavenIndexManager(indexer.getRepo(), indexHandle, indexer.getProperties());
                    mavenIndexManager.saveIndexFiles();
                    virtualIndexMergeDir.close();
                } finally {
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
        List<VirtualRepo> virtualRepositoriesCopy = new ArrayList<VirtualRepo>(virtualRepositories);
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
        List<VirtualRepoDescriptor> virtualRepoDescriptors = repositoryService.getVirtualRepoDescriptors();
        List<VirtualRepoDescriptor> virtualRepositoriesCopy =
                new ArrayList<VirtualRepoDescriptor>(virtualRepoDescriptors);
        for (VirtualRepoDescriptor virtualRepository : virtualRepoDescriptors) {
            if (VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(virtualRepository.getKey())) {
                virtualRepositoriesCopy.remove(virtualRepository);
            }
        }

        return virtualRepositoriesCopy;
    }

    private void scheduleIndexing(boolean runNow) {
        //If scheduled fo immediate run, wait for the previous task to stop
        taskService.stopTasks(IndexerJob.class, runNow);
        //Schedule the indexing
        try {
            long interval = descriptor.getIndexingIntervalHours() * 60L * 60L * 1000L;
            QuartzTask task = new QuartzTask(IndexerJob.class, interval, runNow ? 0 : interval);
            task.addAttribute(IndexerJob.MANUAL_RUN, runNow);
            taskService.startTask(task);
        } catch (Exception e) {
            log.error("Error scheduling the indexer.", e);
        }
        // don't print out log message if the indexer is not enabled (invoked manually) or enabled and was executed manually
        if (descriptor.isEnabled() && !runNow) {
            log.info("Scheduled indexer to run every {} hours.", descriptor.getIndexingIntervalHours());
        }
    }

    private static InternalIndexerService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalIndexerService.class);
    }

    public static class FindOrCreateIndexJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                MavenIndexManager mavenIndexManager =
                        (MavenIndexManager) callbackContext.getMergedJobDataMap().get(
                                MavenIndexManager.class.getName());
                Date fireTime = (Date) callbackContext.getMergedJobDataMap().get(Date.class.getName());
                InternalIndexerService indexer = InternalContextHelper.get().beanForType(InternalIndexerService.class);
                indexer.fetchOrCreateIndex(mavenIndexManager, fireTime);
            } catch (Exception e) {
                log.error("Fetching index files failed: {}.", e.getMessage());
            }
        }
    }

    public static class SaveIndexFileJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                MavenIndexManager mavenIndexManager =
                        (MavenIndexManager) callbackContext.getMergedJobDataMap().get(
                                MavenIndexManager.class.getName());
                InternalIndexerService indexer = getTransactionalMe();
                indexer.saveIndexFiles(mavenIndexManager);
            } catch (Exception e) {
                log.error("Saving index files failed.", e);
            }
        }
    }

    public void fetchOrCreateIndex(MavenIndexManager mavenIndexManager, Date fireTime) {
        boolean remoteIndexExists = mavenIndexManager.fetchRemoteIndex();
        mavenIndexManager.createLocalIndex(fireTime, remoteIndexExists);
    }

    public void saveIndexFiles(MavenIndexManager mavenIndexManager) {
        mavenIndexManager.saveIndexFiles();
    }
}