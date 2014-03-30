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

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.storage.fs.service.PropertiesService;
import org.artifactory.storage.fs.tree.ItemNode;
import org.artifactory.storage.fs.tree.ItemTree;
import org.artifactory.storage.fs.tree.TreeBrowsingCriteriaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Autowired
    private PropertiesService propertiesService;

    @Override
    public BrowsableItem getLocalRepoBrowsableItem(RepoPath repoPath) {
        ItemInfo itemInfo = getItemInfo(repoPath);
        return (itemInfo != null) ? BrowsableItem.getItem(itemInfo) : null;
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

    private ItemInfo getItemInfo(RepoPath repoPath) {
        LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo == null) {
            throw new IllegalArgumentException("No local or cache repo found: " + repoPath.getRepoKey());
        }

        if (repo.isBlackedOut()) {
            return null;
        }

        return repoService.getItemInfo(repoPath);
    }

    @Override
    @Nonnull
    public List<BaseBrowsableItem> getLocalRepoBrowsableChildren(BrowsableItemCriteria criteria) {
        RepoPath repoPath = criteria.getRepoPath();
        LocalRepo repo = repoService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo == null) {
            throw new IllegalArgumentException("No local or cache repo found: " + repoPath.getRepoKey());
        }

        if (repo.isBlackedOut()) {
            return Lists.newArrayListWithCapacity(0);
        }

        ItemTree tree = new ItemTree(criteria.getRepoPath(), new TreeBrowsingCriteriaBuilder()
                .applyRepoIncludeExclude().applySecurity().cacheChildren(false).build());
        ItemNode rootNode = tree.getRootNode();
        if (rootNode == null) {
            throw new ItemNotFoundRuntimeException(repoPath);
        }
        if (!rootNode.isFolder()) {
            throw new FolderExpectedException(repoPath);
        }

        List<ItemNode> children = rootNode.getChildren();
        if (children.isEmpty()) {
            return Lists.newArrayListWithCapacity(0);
        }

        List<BaseBrowsableItem> repoPathChildren = Lists.newArrayList();
        for (ItemNode child : children) {
            //Check if we should return the child
            ItemInfo childItemInfo = child.getItemInfo();
            BrowsableItem browsableItem = BrowsableItem.getItem(childItemInfo);
            if (child.isFolder()) {
                repoPathChildren.add(browsableItem);
            } else if (isPropertiesMatch(childItemInfo, criteria.getRequestProperties())) {   // match props for files
                repoPathChildren.add(browsableItem);
                if (criteria.isIncludeChecksums()) {
                    repoPathChildren.addAll(getBrowsableItemChecksumItems(repo,
                            ((FileInfo) childItemInfo).getChecksumsInfo(), browsableItem));
                }
            }
        }

        Collections.sort(repoPathChildren);
        return repoPathChildren;
    }

    private boolean canRead(RealRepo repo, RepoPath childRepoPath) {
        return authService.canRead(childRepoPath) && repo.accepts(childRepoPath);
    }

    private boolean isPropertiesMatch(ItemInfo itemInfo, Properties requestProps) {
        if (requestProps == null || requestProps.isEmpty()) {
            return true;
        }
        Properties nodeProps = propertiesService.getProperties(itemInfo.getRepoPath());
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
            log.info("Error while listing remote resources: {}", e.getMessage());
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
            RepoPath remoteRepoPath = InternalRepoPathFactory.create(repoPath.getRepoKey(), path,
                    remoteItem.isDirectory());
            RepoPath cacheRepoPath = InternalRepoPathFactory.cacheRepoPath(remoteRepoPath);
            if (canRead(repo, cacheRepoPath)) {
                RemoteBrowsableItem browsableItem = new RemoteBrowsableItem(remoteItem, remoteRepoPath);
                if (remoteItem.getEffectiveUrl() != null) {
                    browsableItem.setEffectiveUrl(remoteItem.getEffectiveUrl());
                }
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
            if (virtualRepoAccepts(virtualRepo, child.getRepoPath())) {
                VirtualBrowsableItem virtualItem;
                if (childrenToReturn.containsKey(childRelativePath)) {
                    virtualItem = (VirtualBrowsableItem) childrenToReturn.get(childRelativePath);
                    if (!child.isRemote() && virtualItem.isRemote()) {
                        virtualItem.setCreated(child.getCreated());
                        virtualItem.setLastModified(child.getLastModified());
                        virtualItem.setSize(child.getSize());
                    }
                } else {
                    // New
                    Collection<VirtualRepo> virtualRepos = pathToVirtualRepos.get(childRelativePath);
                    virtualItem = new VirtualBrowsableItem(child.getName(), child.isFolder(), child.getCreated(),
                            child.getLastModified(), child.getSize(), InternalRepoPathFactory.create(virtualRepoKey,
                            childRelativePath),
                            Lists.newArrayList(getSearchableRepoKeys(virtualRepos)));
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
                    if (virtualRepoAccepts(repo, localRepoBrowsableChild.getRepoPath())) {
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
                    if (virtualRepoAccepts(repo, remoteRepoBrowsableChild.getRepoPath())) {
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
            if (repo.accepts(pathToCheck)) {
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

    private boolean virtualRepoAccepts(VirtualRepo virtualRepo, RepoPath repoPath) {
        String path = repoPath.getPath();
        if (repoPath.isFolder()) {
            path += "/";
        }

        //If the path is not accepted, return immediately
        if (!virtualRepo.accepts(repoPath)) {
            log.debug("Virtual repo '{}' did not accept path '{}'", virtualRepo, repoPath);
            return false;
        }

        if (path.contains(MavenNaming.NEXUS_INDEX_DIR) || MavenNaming.isIndex(path)) {
            return false;
        }
        return true;
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
        List<VirtualRepoItem> result = new ArrayList<>(children.size());
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
                        checksumValue.getBytes(Charsets.UTF_8).length);

                RepoPath checksumItemRepoPath = checksumItem.getRepoPath();
                if (authService.canRead(checksumItemRepoPath) && repo.accepts(checksumItemRepoPath)) {
                    browsableChecksumItems.add(checksumItem);
                }
            }
        }

        return browsableChecksumItems;
    }
}
