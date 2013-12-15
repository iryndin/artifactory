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

package org.artifactory.search.archive;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.HaAddon;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.ZipEntryInfo;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.model.xstream.fs.ZipEntryImpl;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.fs.VfsZipFile;
import org.artifactory.storage.fs.service.ArchiveEntriesService;
import org.artifactory.storage.fs.service.FileService;
import org.artifactory.storage.fs.service.TasksService;
import org.artifactory.storage.fs.tree.ItemNode;
import org.artifactory.storage.fs.tree.ItemNodeFilter;
import org.artifactory.storage.fs.tree.ItemTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;

/**
 * A utility for marking archives and indexing their content
 *
 * @author Noam Tenne
 */
@Service
public class ArchiveIndexerImpl implements InternalArchiveIndexer, ContextReadinessListener {
    private static final Logger log = LoggerFactory.getLogger(ArchiveIndexerImpl.class);

    @Autowired
    private TasksService tasksService;

    @Autowired
    private ArchiveEntriesService archiveEntriesService;

    @Autowired
    private FileService fileService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private DbService dbService;

    @Autowired
    private AddonsManager addonsManager;

    // a semaphore to guard against parallel indexing
    private SemaphoreWrapper indexingSemaphore;
    // queue of repository paths waiting for indexing
    private final Queue<RepoPath> indexingQueue = new ConcurrentLinkedQueue<>();

    private SemaphoreWrapper indexMarkedArchivesSemaphore;

    @Override
    public void onContextCreated() {
        //Index archives marked for indexing (might have left overs from abrupt shutdown after deploy)
        if (!ContextHelper.get().isOffline()) {
            getAdvisedMe().asyncIndexMarkedArchives();
        }
    }

    @Override
    public boolean index(RepoPath archiveRepoPath) {
        try {
            // check the repo path is eligible for archive indexing
            if (!isIndexSupported(archiveRepoPath)) {
                log.trace("Indexing of '{}' not active.", archiveRepoPath);
                return false;
            }

            // get the fs item and make sure it is a file
            LocalRepo localRepo = repoService.localOrCachedRepositoryByKey(archiveRepoPath.getRepoKey());
            if (localRepo == null) {
                log.debug("Skipping archive indexing for {} - repo not found", archiveRepoPath);
                return false;
            }
            VfsItem immutableItem = localRepo.getImmutableFsItem(archiveRepoPath);
            if (immutableItem == null || !immutableItem.isFile()) {
                log.debug("Skipping archive indexing for {} - item does not exist or not a file.", archiveRepoPath);
                return false;
            }

            VfsFile vfsFile = (VfsFile) immutableItem;

            // check if the checksum is already indexed
            if (archiveEntriesService.isIndexed(vfsFile.getSha1())) {
                log.debug("Archive '{}' with checksum '{}' is already indexed", archiveRepoPath, vfsFile.getSha1());
                return false;
            }

            // start indexing ...
            log.info("Indexing archive: {}", vfsFile);
            VfsZipFile jar = null;
            try {
                jar = new VfsZipFile(vfsFile);

                List<? extends ZipEntry> entries = jar.entries();
                Set<ZipEntryInfo> zipEntryInfos = Sets.newHashSet();
                for (ZipEntry zipEntry : entries) {
                    if (!zipEntry.isDirectory()) {
                        zipEntryInfos.add(new ZipEntryImpl(zipEntry));
                    }
                }

                ArchiveEntriesService archiveEntriesService = ContextHelper.get().beanForType(
                        ArchiveEntriesService.class);
                archiveEntriesService.addArchiveEntries(vfsFile.getSha1(), zipEntryInfos);
                log.debug("Indexed archive: {}", vfsFile);
                return true;
            } catch (IOException e) {
                log.error("Failed to index '{}': {}", archiveRepoPath, e.getMessage());
                log.debug("Failed to index:", e);
                return false;
            } finally {
                Closeables.closeQuietly(jar);
            }
        } finally {
            // remove the task in any case if it exists
            tasksService.removeIndexTask(archiveRepoPath);
        }
    }

    @Override
    public void asyncIndex(RepoPath repoPath) {
        indexingQueue.add(repoPath);
        triggerQueueIndexing();
    }

    private void triggerQueueIndexing() {
        if (!getIndexingSemaphore().tryAcquire()) {
            log.trace("Archive indexing already running by another thread");
            return;
        }
        try {
            if (indexingQueue.size() > 0) {
                log.debug("Indexing {} queued items", indexingQueue.size());
            }
            RepoPath repoPath;
            while ((repoPath = indexingQueue.poll()) != null) {
                try {
                    getAdvisedMe().index(repoPath);
                    if (!InternalContextHelper.get().isReady()) {
                        break;  // stop execution if the context is not ready (shutting down, refreshing conf etc.)
                    }
                } catch (Exception e) {
                    log.error("Exception indexing " + repoPath, e);
                    forceArchiveIndexerTaskDeletion(repoPath);
                }
            }
        } finally {
            getIndexingSemaphore().release();
        }
    }

    @Override
    public void asyncIndexMarkedArchives() {
        if (!getIndexMarkedArchivesSemaphore().tryAcquire()) {
            log.info("Received index all marked archives request but an indexing process is already running.");
            return;
        }
        try {
            log.debug("Scheduling indexing for marked archives.");
            Set<RepoPath> archiveRepoPaths = tasksService.getIndexTasks();
            indexingQueue.addAll(archiveRepoPaths);
            triggerQueueIndexing();
        } finally {
            getIndexMarkedArchivesSemaphore().release();
        }
    }

    @Override
    public void markArchiveForIndexing(RepoPath repoPath) {
        if (!isIndexSupported(repoPath)) {
            return;
        }

        tasksService.addIndexTask(repoPath);
    }

    @Override
    public void recursiveMarkArchivesForIndexing(RepoPath baseRepoPath, boolean indexAllRepos) {
        List<RepoPath> toIndex = Lists.newArrayList();
        if (indexAllRepos) {
            List<LocalRepoDescriptor> localRepos = repoService.getLocalAndCachedRepoDescriptors();
            for (LocalRepoDescriptor localRepo : localRepos) {
                toIndex.add(InternalRepoPathFactory.repoRootPath(localRepo.getKey()));
            }
        } else {
            toIndex.add(baseRepoPath);
        }
        for (RepoPath repoPath : toIndex) {
            ItemTree itemTree = new ItemTree(repoPath, new ItemNodeFilter() {
                @Override
                public boolean accepts(ItemInfo item) {
                    return item.isFolder() || isIndexSupported(item.getRepoPath());
                }
            });
            ItemNode rootNode = itemTree.getRootNode();
            if (rootNode != null) {
                log.info("Recursively marking paths under '{}' for archive indexing", repoPath);
                addIndexTaskRecursively(rootNode);
            } else {
                log.warn("Root path for archive indexing not found: {}", repoPath);
            }
        }
        asyncIndexMarkedArchives();
    }

    @Override
    public boolean isIndexed(RepoPath repoPath) {
        String sha1 = fileService.getNodeSha1(repoPath);
        return isIndexed(sha1);
    }

    @Override
    public boolean isIndexed(String sha1) {
        return archiveEntriesService.isIndexed(sha1);
    }

    private void addIndexTaskRecursively(ItemNode treeNode) {
        if (!treeNode.isFolder()) {
            getAdvisedMe().markArchiveForIndexing(treeNode.getRepoPath());
        } else {
            // Recursive call to calculate and set
            List<ItemNode> children = treeNode.getChildren();
            if (children != null) {
                for (ItemNode child : children) {
                    addIndexTaskRecursively(child);
                }
            }
        }
    }

    private void forceArchiveIndexerTaskDeletion(final RepoPath repoPath) {
        dbService.invokeInTransaction(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    tasksService.removeIndexTask(repoPath);
                } catch (Exception e) {
                    log.error("Fail to delete task from database", e);
                }
                return null;
            }
        });
    }

    private boolean isIndexSupported(RepoPath repoPath) {
        MimeType mimeType = NamingUtils.getMimeType(repoPath.getName());
        return mimeType.isArchive() && mimeType.isIndex();
    }

    private InternalArchiveIndexer getAdvisedMe() {
        return ContextHelper.get().beanForType(InternalArchiveIndexer.class);
    }

    private SemaphoreWrapper getIndexingSemaphore() {
        if (indexingSemaphore == null) {
            HaAddon haAddon = addonsManager.addonByType(HaAddon.class);
            indexingSemaphore = haAddon.getSemaphore(HaCommonAddon.INDEXING_SEMAPHORE_NAME);
        }
        return indexingSemaphore;
    }

    private SemaphoreWrapper getIndexMarkedArchivesSemaphore() {
        if (indexMarkedArchivesSemaphore == null) {
            HaAddon haAddon = addonsManager.addonByType(HaAddon.class);
            indexMarkedArchivesSemaphore = haAddon.getSemaphore(HaCommonAddon.INDEX_MARKED_ARCHIVES_SEMAPHORE_NAME);
        }
        return indexMarkedArchivesSemaphore;
    }

    @Override
    public void onContextReady() {
        // nothing to do
    }

    @Override
    public void onContextUnready() {
        // nothing to do
    }
}