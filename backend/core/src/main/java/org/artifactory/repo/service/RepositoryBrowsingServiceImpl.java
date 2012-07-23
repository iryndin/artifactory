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

package org.artifactory.repo.service;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.BaseBrowsableItem;
import org.artifactory.api.repo.BrowsableItem;
import org.artifactory.api.repo.BrowsableItemCriteria;
import org.artifactory.api.repo.RemoteBrowsableItem;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.VirtualBrowsableItem;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tomer Cohen
 */
@Service
public class RepositoryBrowsingServiceImpl implements RepositoryBrowsingService {
    private static final Logger log = LoggerFactory.getLogger(RepositoryBrowsingServiceImpl.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private InternalRepositoryService repoService;

    @Override
    public BrowsableItem getLocalRepoBrowsableItem(RepoPath repoPath) {
        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem != null) {
            ItemInfo itemInfo = fsItem.getInfo();
            return BrowsableItem.getItem(itemInfo);
        } else {
            return null;
        }
    }

    @Override
    public VirtualBrowsableItem getVirtualRepoBrowsableItem(RepoPath repoPath) {
        VirtualRepoItem virtualRepoItem = getVirtualRepoItem(repoPath);
        if (virtualRepoItem != null) {
            ItemInfo itemInfo = virtualRepoItem.getItemInfo();
            return new VirtualBrowsableItem(itemInfo.getName(), itemInfo.isFolder(), itemInfo.getCreated(),
                    itemInfo.getLastModified(), itemInfo.isFolder() ? -1 : ((FileInfo) itemInfo).getSize(),
                    repoPath, virtualRepoItem.getRepoKeys());
        } else {
            return null;
        }
    }

    private JcrFsItem getFsItem(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            throw new IllegalArgumentException("No local or cache repo found: " + repoKey);
        }
        if (!repo.itemExists(repoPath.getPath())) {
            throw new ItemNotFoundRuntimeException("Couldn't find item: " + repoPath);
        }
        if (repo.isBlackedOut()) {
            return null;
        }
        return repo.getJcrFsItem(repoPath);
    }

    @Override
    @Nonnull
    public List<BaseBrowsableItem> getLocalRepoBrowsableChildren(BrowsableItemCriteria criteria) {
        RepoPath repoPath = criteria.getRepoPath();
        String repoKey = repoPath.getRepoKey();
        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return Lists.newArrayList();
        }
        if (!fsItem.isDirectory()) {
            throw new FolderExpectedException(repoPath);
        }

        JcrFolder repoPathFolder = (JcrFolder) fsItem;
        List<JcrFsItem> children = repoPathFolder.getJcrItems();
        LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            log.debug("Repository '{}' does not exist", repoKey);
            return Lists.newArrayList();
        }

        List<BaseBrowsableItem> repoPathChildren = Lists.newArrayList();
        for (JcrFsItem child : children) {
            //Check if we should return the child
            RepoPath childRepoPath = child.getRepoPath();

            ItemInfo itemInfo = child.getInfo();
            BrowsableItem browsableItem = BrowsableItem.getItem(itemInfo);

            if (authService.canImplicitlyReadParentPath(childRepoPath) && repo.accepts(childRepoPath.getPath())) {
                boolean isMatchingFile = child.isFile() && isPropertiesMatch(child, criteria.getRequestProperties());
                if (isMatchingFile || child.isFolder()) {
                    repoPathChildren.add(browsableItem);
                }
                if (isMatchingFile && criteria.isIncludeChecksums()) {
                    repoPathChildren.addAll(getBrowsableItemChecksumItems(repo,
                            ((FileInfo) itemInfo).getChecksumsInfo(), browsableItem));
                }
            }
            LockingHelper.releaseReadLock(childRepoPath);
        }

        if (criteria.isIncludeMavenMetadata()) {
            addBrowsableMetadataAndChecksums(repo, repoPathFolder, repoPathChildren, criteria);
        }
        LockingHelper.releaseReadLock(repoPathFolder.getRepoPath());
        //TODO: [by ys] the sort can make the up path not be the first
        Collections.sort(repoPathChildren);
        return repoPathChildren;
    }

    private boolean isPropertiesMatch(JcrFsItem<?, ?> fsItem, Properties requestProps) {
        if (requestProps == null || requestProps.isEmpty()) {
            return true;
        }
        Properties nodeProps = fsItem.getMetadata(Properties.class);
        if (nodeProps == null) {
            return true;
        }
        Properties.MatchResult result = nodeProps.matchQuery(requestProps);
        return !Properties.MatchResult.CONFLICT.equals(result);
    }

    @Override
    @Nonnull
    public List<BaseBrowsableItem> getRemoteRepoBrowsableChildren(BrowsableItemCriteria criteria) {
        RepoPath repoPath = criteria.getRepoPath();
        String repoKey = repoPath.getRepoKey();
        String relativePath = repoPath.getPath();
        RemoteRepo repo = repoService.remoteRepositoryByKey(repoKey);
        if (repo == null) {
            throw new IllegalArgumentException("Remote repo not found: " + repoKey);
        }

        // include remote resources based on the flag and the offline mode
        boolean includeRemoteResources = criteria.isIncludeRemoteResources() && repo.isListRemoteFolderItems();

        // first get all the cached items
        List<BaseBrowsableItem> children = Lists.newArrayList();
        boolean pathExistsInCache = false;
        if (repo.isStoreArtifactsLocally()) {
            try {
                BrowsableItemCriteria cacheCriteria = new BrowsableItemCriteria.Builder(criteria).
                        repoPath(InternalRepoPathFactory.create(repo.getLocalCacheRepo().getKey(),
                                relativePath)).build();
                children = getLocalRepoBrowsableChildren(cacheCriteria);
                pathExistsInCache = true;
            } catch (ItemNotFoundRuntimeException e) {
                // this is legit only if we also want to add remote items
                if (!includeRemoteResources) {
                    throw e;
                }
            }
        }
        if (includeRemoteResources) {
            listRemoteBrowsableChildren(children, repo, relativePath, pathExistsInCache);
        }
        Collections.sort(children);
        return children;
    }

    private void listRemoteBrowsableChildren(List<BaseBrowsableItem> children, RemoteRepo repo, String relativePath,
            boolean pathExistsInCache) {
        RepoPath repoPath = repo.getRepoPath(relativePath);
        List<RemoteItem> remoteItems = Lists.newArrayList();
        try {
            remoteItems.addAll(repo.listRemoteResources(relativePath));
        } catch (IOException e) {
            log.debug("Error while listing remote resources", e);
            // probably remote not found - return 404 only if current folder doesn't exist in the cache
            if (!pathExistsInCache) {
                // no cache and remote failed - signal 404
                throw new ItemNotFoundRuntimeException("Couldn't find item: " + repoPath);
            }
        }
        // filter already existing local items
        remoteItems = Lists.newArrayList(
                Iterables.filter(remoteItems, new RemoteOnlyBrowsableItemPredicate(children)));
        for (RemoteItem remoteItem : remoteItems) {
            // remove the remote repository base url
            String path = StringUtils.removeStart(remoteItem.getUrl(), repo.getUrl());
            RepoPath remoteRepoPath = InternalRepoPathFactory.create(repoPath.getRepoKey(), path);
            String pathToTest = remoteRepoPath.getPath();
            if (remoteItem.isDirectory()) {
                pathToTest += "/";
            }
            if (authService.canImplicitlyReadParentPath(repoPath) && repo.accepts(pathToTest)) {
                BrowsableItem browsableItem = new RemoteBrowsableItem(remoteItem, remoteRepoPath);
                children.add(browsableItem);
            }
        }
    }

    @Override
    @Nonnull
    public List<BaseBrowsableItem> getVirtualRepoBrowsableChildren(BrowsableItemCriteria criteria) {
        RepoPath repoPath = criteria.getRepoPath();
        String virtualRepoKey = repoPath.getRepoKey();
        VirtualRepo virtualRepo = repoService.virtualRepositoryByKey(virtualRepoKey);
        if (virtualRepo == null) {
            throw new IllegalArgumentException("No virtual repo found: " + virtualRepoKey);
        }

        List<BaseBrowsableItem> candidateChildren = Lists.newArrayList();
        List<VirtualRepo> searchableRepos = getSearchableRepos(virtualRepo, repoPath);
        Multimap<String, VirtualRepo> pathToVirtualRepos = HashMultimap.create();
        // add children from all local repos
        for (VirtualRepo repo : searchableRepos) {
            addVirtualBrowsableItemsFromLocal(criteria, repo, candidateChildren, pathToVirtualRepos);
            addVirtualBrowsableItemsFromRemote(criteria, repo, candidateChildren, pathToVirtualRepos);
        }
        // only add the candidate that this virtual repository accepts via its include/exclude rules
        Map<String, BaseBrowsableItem> childrenToReturn = Maps.newHashMap();
        for (BaseBrowsableItem child : candidateChildren) {

            String childRelativePath = child.getRelativePath();
            if (virtualRepoAccepts(virtualRepo, childRelativePath)) {
                VirtualBrowsableItem virtualItem;
                if (childrenToReturn.containsKey(childRelativePath)) {
                    virtualItem = (VirtualBrowsableItem) childrenToReturn.get(childRelativePath);
                } else {
                    Collection<VirtualRepo> virtualRepos = pathToVirtualRepos.get(childRelativePath);
                    virtualItem = new VirtualBrowsableItem(child.getName(), child.isFolder(), child.getCreated(),
                            child.getLastModified(), child.getSize(), InternalRepoPathFactory.create(virtualRepoKey,
                            childRelativePath),
                            Lists.<String>newArrayList(getSearchableRepoKeys(virtualRepos)));
                    virtualItem.setRemote(true);    // default to true
                    childrenToReturn.put(childRelativePath, virtualItem);
                }
                virtualItem.setRemote(virtualItem.isRemote() && child.isRemote());   // remote if all are remote
                virtualItem.addRepoKey(child.getRepoKey());
            }
        }
        return Lists.newArrayList(childrenToReturn.values());
    }

    private void addVirtualBrowsableItemsFromLocal(BrowsableItemCriteria criteria, VirtualRepo repo,
            List<BaseBrowsableItem> candidateChildren, Multimap<String, VirtualRepo> pathToVirtualRepos) {
        String relativePath = criteria.getRepoPath().getPath();
        List<LocalRepo> localRepositories = repo.getLocalRepositories();

        for (LocalRepo localRepo : localRepositories) {
            RepoPath path = InternalRepoPathFactory.create(localRepo.getKey(), relativePath);
            try {
                BrowsableItemCriteria localCriteria = new BrowsableItemCriteria.Builder(criteria).repoPath(path).
                        build();
                List<BaseBrowsableItem> localRepoBrowsableChildren = getLocalRepoBrowsableChildren(localCriteria);
                // go over all local repo browsable children, these have already been filtered according
                // to each local repo's rules, now all that is left is to check that the virtual repo that
                // the local repo belongs to accepts as well.
                for (BaseBrowsableItem localRepoBrowsableChild : localRepoBrowsableChildren) {
                    if (virtualRepoAccepts(repo, localRepoBrowsableChild.getRelativePath())) {
                        pathToVirtualRepos.put(localRepoBrowsableChild.getRelativePath(), repo);
                        candidateChildren.add(localRepoBrowsableChild);
                    }
                }
            } catch (ItemNotFoundRuntimeException e) {
                log.trace("Could not find local browsable children at '{}'", criteria + " " + e.getMessage());
            }
        }
    }

    private void addVirtualBrowsableItemsFromRemote(BrowsableItemCriteria criteria, VirtualRepo repo,
            List<BaseBrowsableItem> candidateChildren, Multimap<String, VirtualRepo> pathToVirtualRepos) {
        List<RemoteRepo> remoteRepositories = repo.getRemoteRepositories();
        // add children from all remote repos (and their caches)
        for (RemoteRepo remoteRepo : remoteRepositories) {
            RepoPath remoteRepoPath = InternalRepoPathFactory.create(remoteRepo.getKey(),
                    criteria.getRepoPath().getPath());
            try {
                BrowsableItemCriteria remoteCriteria = new BrowsableItemCriteria.Builder(criteria).
                        repoPath(remoteRepoPath).build();
                List<BaseBrowsableItem> remoteRepoBrowsableChildren =
                        getRemoteRepoBrowsableChildren(remoteCriteria);
                for (BaseBrowsableItem remoteRepoBrowsableChild : remoteRepoBrowsableChildren) {
                    if (virtualRepoAccepts(repo, remoteRepoBrowsableChild.getRelativePath())) {
                        pathToVirtualRepos.put(remoteRepoBrowsableChild.getRelativePath(), repo);
                        candidateChildren.add(remoteRepoBrowsableChild);
                    }
                }
            } catch (ItemNotFoundRuntimeException e) {
                log.trace("Could not find local browsable children at '{}'",
                        criteria + " " + e.getMessage());
            }
        }
    }

    private List<VirtualRepo> getSearchableRepos(VirtualRepo virtualRepo, RepoPath pathToCheck) {
        List<VirtualRepo> repos = Lists.newArrayList();
        List<VirtualRepo> allVirtualRepos = virtualRepo.getResolvedVirtualRepos();
        for (VirtualRepo repo : allVirtualRepos) {
            if (repo.accepts(pathToCheck.getPath())) {
                repos.add(repo);
            }
        }
        return repos;
    }

    private Collection<String> getSearchableRepoKeys(Collection<VirtualRepo> virtualRepos) {
        return Collections2.transform(virtualRepos, new Function<VirtualRepo, String>() {
            @Override
            public String apply(@Nonnull VirtualRepo input) {
                return input.getKey();
            }
        });
    }

    private boolean virtualRepoAccepts(VirtualRepo virtualRepo, String relativePath) {
        //If the path is not accepted, return immediately
        if (!virtualRepo.accepts(relativePath)) {
            log.debug("Virtual repo '{}' did not accept path '{}'", virtualRepo, relativePath);
            return false;
        }

        if (relativePath.contains(MavenNaming.NEXUS_INDEX_DIR) || MavenNaming.isIndex(relativePath)) {
            return false;
        }

        //The path is accepted, make sure this is not a checksum, if it is, strip the ext and test the source artifact
        return !NamingUtils.isChecksum(relativePath) ||
                virtualRepo.accepts(FilenameUtils.removeExtension(relativePath));
    }

    @Override
    public VirtualRepoItem getVirtualRepoItem(RepoPath repoPath) {
        VirtualRepo virtualRepo = repoService.virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new IllegalArgumentException("Repository " + repoPath.getRepoKey() + " does not exists!");
        }
        VirtualRepoItem repoItem = virtualRepo.getVirtualRepoItem(repoPath);
        if (repoItem == null) {
            return null;
        }

        //Security - check that we can return the child
        Iterator<String> repoKeysIterator = repoItem.getRepoKeys().iterator();
        while (repoKeysIterator.hasNext()) {
            String realRepoKey = repoKeysIterator.next();
            RepoPath realRepoPath = InternalRepoPathFactory.create(realRepoKey, repoPath.getPath());
            boolean canRead = authService.canRead(realRepoPath);
            if (!canRead) {
                //Don't bother with stuff that we do not have read access to
                repoKeysIterator.remove();
            }
        }

        // return null if user doesn't have permissions for any of the real repo paths
        if (repoItem.getRepoKeys().isEmpty()) {
            return null;
        } else {
            return repoItem;
        }
    }

    @Override
    public List<VirtualRepoItem> getVirtualRepoItems(RepoPath folderPath) {
        VirtualRepo virtualRepo = repoService.virtualRepositoryByKey(folderPath.getRepoKey());
        if (virtualRepo == null) {
            throw new RepositoryRuntimeException(
                    "Repository " + folderPath.getRepoKey() + " does not exists!");
        }
        //Get a deep children view of the virtual repository (including contained virtual repos)
        Set<String> children = virtualRepo.getChildrenNamesDeeply(folderPath);
        List<VirtualRepoItem> result = new ArrayList<VirtualRepoItem>(children.size());
        for (String childName : children) {
            //Do not add or check hidden items
            RepoPath childPath = InternalRepoPathFactory.create(folderPath, childName);
            VirtualRepoItem virtualRepoItem = getVirtualRepoItem(childPath);
            if (virtualRepoItem != null) {
                result.add(virtualRepoItem);
            }
        }
        return result;
    }

    /**
     * This predicate returns true if a given item, represented by URL, doesn't already exists in the local items.
     */
    private static class RemoteOnlyBrowsableItemPredicate implements Predicate<RemoteItem> {
        private List<BaseBrowsableItem> localItems;

        private RemoteOnlyBrowsableItemPredicate(List<BaseBrowsableItem> localItems) {
            this.localItems = localItems;
        }

        @Override
        public boolean apply(@Nonnull RemoteItem input) {
            for (BaseBrowsableItem localItem : localItems) {
                if (localItem.getName().equals(input.getName())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Adds local-repo browsable items of metadata and complying checksums for the given jcr folder (if exists)
     *
     * @param repo             Browsed Repo
     * @param repoPathFolder   Folder to search and add metadata for
     * @param repoPathChildren Folder children list
     * @param criteria         Browsable item conditions
     */
    private void addBrowsableMetadataAndChecksums(LocalRepo repo, JcrFolder repoPathFolder,
            List<BaseBrowsableItem> repoPathChildren, BrowsableItemCriteria criteria) {
        MetadataInfo metadataInfo = repoService.getMetadataInfo(repoPathFolder.getRepoPath(),
                MavenNaming.MAVEN_METADATA_NAME);

        if (metadataInfo != null) {
            BrowsableItem browsableItem = BrowsableItem.getMetadataItem(metadataInfo);

            RepoPath metadataItemRepoPath = browsableItem.getRepoPath();

            if (authService.canImplicitlyReadParentPath(metadataItemRepoPath) &&
                    repo.accepts(metadataItemRepoPath.getPath())) {
                repoPathChildren.add(browsableItem);

                if (criteria.isIncludeChecksums()) {
                    repoPathChildren.addAll(getBrowsableItemChecksumItems(repo, metadataInfo.getChecksumsInfo(),
                            browsableItem));
                }
            }
        }
    }

    /**
     * Returns local-repo browsable items of checksums for the given browsable item
     *
     * @param repo          Browsed repo
     * @param checksumsInfo Item's checksum info
     * @param browsableItem Browsable item to create checksum items for    @return Checksum browsable items
     */
    private List<BrowsableItem> getBrowsableItemChecksumItems(LocalRepo repo,
            ChecksumsInfo checksumsInfo, BrowsableItem browsableItem) {
        List<BrowsableItem> browsableChecksumItems = Lists.newArrayList();
        Set<ChecksumInfo> checksums = checksumsInfo.getChecksums();
        for (ChecksumType checksumType : ChecksumType.values()) {
            String checksumValue = repo.getChecksumPolicy().getChecksum(checksumType, checksums);
            if (org.apache.commons.lang.StringUtils.isNotBlank(checksumValue)) {
                BrowsableItem checksumItem = BrowsableItem.getChecksumItem(browsableItem, checksumType,
                        checksumValue.getBytes().length);

                RepoPath checksumItemRepoPath = checksumItem.getRepoPath();
                if (authService.canImplicitlyReadParentPath(checksumItemRepoPath) &&
                        repo.accepts(checksumItemRepoPath.getPath())) {
                    browsableChecksumItems.add(checksumItem);
                }
            }
        }

        return browsableChecksumItems;
    }
}
