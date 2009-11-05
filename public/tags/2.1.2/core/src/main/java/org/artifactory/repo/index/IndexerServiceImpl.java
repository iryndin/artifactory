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

package org.artifactory.repo.index;

import org.apache.commons.collections15.OrderedMap;
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
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.FileUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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
 * Created by IntelliJ IDEA. User: yoavl
 */
@Service
public class IndexerServiceImpl implements InternalIndexerService {
    private static final Logger log = LoggerFactory.getLogger(IndexerServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    private IndexerDescriptor descriptor;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(InternalIndexerService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{TaskService.class, InternalRepositoryService.class};
    }

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
        scheduleIndexing(false);
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
        Set<? extends RepoDescriptor> excludedRepositories = descriptor.getExcludedRepositories();
        List<RealRepo> indexedRepos = getNonVirtualRepositoriesToIndex(excludedRepositories);

        //Do the indexing work
        for (RealRepo indexedRepo : indexedRepos) {
            //Check if we need to stop/suspend
            if (taskService.blockIfPausedAndShouldBreak()) {
                return;
            }
            RepoIndexerData repoIndexerData = new RepoIndexerData(indexedRepo);
            try {
                //TODO: [by YS] why do we execute a tasks here instead of direct call??
                QuartzTask taskFindOrCreateIndex = new QuartzTask(FindOrCreateIndexJob.class, "FindOrCreateIndex");
                taskFindOrCreateIndex.addAttribute(RepoIndexerData.class.getName(), repoIndexerData);
                taskFindOrCreateIndex.addAttribute(Date.class.getName(), fireTime);
                taskService.startTask(taskFindOrCreateIndex);
                taskService.waitForTaskCompletion(taskFindOrCreateIndex.getToken());
                //Check again if we need to stop/suspend
                if (taskService.blockIfPausedAndShouldBreak()) {
                    return;
                }
                QuartzTask saveIndexFileTask = new QuartzTask(SaveIndexFileJob.class, "SaveIndexFile");
                saveIndexFileTask.addAttribute(RepoIndexerData.class.getName(), repoIndexerData);
                taskService.startTask(saveIndexFileTask);
                //No real need to wait, but since other task are waiting for indexer completion, leaving it
                taskService.waitForTaskCompletion(saveIndexFileTask.getToken());
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
                return;
            }
        }
        getTransactionalMe().mergeVirtualReposIndexes(excludedRepositories, indexedRepos);
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

    public void mergeVirtualReposIndexes(Set<? extends RepoDescriptor> excludedRepositories,
            List<RealRepo> indexedRepos) {
        List<VirtualRepo> virtualRepos = filterExcludedVirtualRepos(excludedRepositories);

        //Keep a list of extracted index dirs for all the local repo indexes for merging
        Map<StoringRepo, FSDirectory> extractedLocalRepoIndexes = new HashMap<StoringRepo, FSDirectory>();
        try {
            //Merge virtual repo indexes
            for (VirtualRepo virtualRepo : virtualRepos) {
                //Check if we need to stop/suspend
                if (taskService.blockIfPausedAndShouldBreak()) {
                    return;
                }
                OrderedMap<String, LocalRepo> searchableLocalRepositories =
                        virtualRepo.getSearchableLocalRepositories();
                OrderedMap<String, LocalCacheRepo> searchableLocalCacheRepositories =
                        virtualRepo.getSearchableLocalCacheRepositories();
                Set<LocalRepo> localRepos = new HashSet<LocalRepo>();
                localRepos.addAll(searchableLocalRepositories.values());
                localRepos.addAll(searchableLocalCacheRepositories.values());
                if (indexedRepos.size() == 0) {
                    return;
                }
                //Create a temp lucene dir and merge each local into it
                ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
                File dir = FileUtils.createRandomDir(artifactoryHome.getWorkTmpDir(), "artifactory.merged-index");
                try {
                    FSDirectory virtualIndexMergeDir = FSDirectory.getDirectory(dir);
                    RepoIndexer repoIndexer = new RepoIndexer(virtualRepo);
                    repoIndexer.createContext(virtualIndexMergeDir);

                    //Take the local index from each relevant indexed repo (for remote take local cache)
                    for (RealRepo indexedRepo : indexedRepos) {
                        //Check if we need to stop/suspend
                        if (taskService.blockIfPausedAndShouldBreak()) {
                            return;
                        }
                        LocalRepo localRepo = indexedRepo.isLocal() ? (LocalRepo) indexedRepo :
                                ((RemoteRepo) indexedRepo).getLocalCacheRepo();
                        //Extract aside the index from the local repo
                        if (localRepos.contains(localRepo)) {
                            repoIndexer.mergeInto(virtualIndexMergeDir, localRepo, extractedLocalRepoIndexes);
                        }
                    }
                    //Store the index into the virtual repo
                    //Get the last zip and props and store them - we need to return them or create them from the dir
                    RepoIndexerData indexerData = new RepoIndexerData(repoIndexer, virtualIndexMergeDir);
                    saveIndexFiles(indexerData);
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

    private void scheduleIndexing(boolean now) {
        //If scheduled fo immediate run, wait for the previous task to stop
        taskService.stopTasks(IndexerJob.class, now);
        //Schedule the indexing
        int indexingIntervalHours = descriptor.getIndexingIntervalHours();
        long interval = indexingIntervalHours * 60L * 60L * 1000L;
        QuartzTask task = new QuartzTask(IndexerJob.class, interval, now ? 0 : interval);
        task.addAttribute(IndexerJob.MANUAL_RUN, true);
        try {
            taskService.startTask(task);
            log.info("Scheduled indexer to run every {} hours.", indexingIntervalHours);
        } catch (Exception e) {
            throw new RuntimeException("Error in scheduling the indexer.", e);
        }
    }

    private static InternalIndexerService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalIndexerService.class);
    }

    public static class FindOrCreateIndexJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                RepoIndexerData repoIndexerData =
                        (RepoIndexerData) callbackContext.getMergedJobDataMap().get(RepoIndexerData.class.getName());
                Date fireTime = (Date) callbackContext.getMergedJobDataMap().get(Date.class.getName());
                InternalIndexerService indexer = InternalContextHelper.get().beanForType(InternalIndexerService.class);
                indexer.fecthOrCreateIndex(repoIndexerData, fireTime);
            } catch (Exception e) {
                log.error("Fetching index files failed: {}.", e.getMessage());
            }
        }
    }

    public static class SaveIndexFileJob extends QuartzCommand {
        @Override
        protected void onExecute(JobExecutionContext callbackContext) {
            try {
                RepoIndexerData repoIndexerData =
                        (RepoIndexerData) callbackContext.getMergedJobDataMap().get(RepoIndexerData.class.getName());
                InternalIndexerService indexer = getTransactionalMe();
                indexer.saveIndexFiles(repoIndexerData);
            } catch (Exception e) {
                log.error("Saving index files failed.", e);
            }
        }

    }

    public void fecthOrCreateIndex(RepoIndexerData repoIndexerData, Date fireTime) {
        log.debug("Fetching or creating index files for {}", repoIndexerData.indexedRepo);
        boolean remoteIndexExists = repoIndexerData.fetchRemoteIndex();
        repoIndexerData.createLocalIndex(fireTime, remoteIndexExists);
        log.debug("Fetch or create index files for {}", repoIndexerData.indexedRepo);
    }

    public void saveIndexFiles(RepoIndexerData repoIndexerData) {
        log.debug("Saving index file for {}", repoIndexerData.indexStorageRepo);
        repoIndexerData.saveIndexFiles();
        log.debug("Saved index file for {}", repoIndexerData.indexStorageRepo);
    }
}