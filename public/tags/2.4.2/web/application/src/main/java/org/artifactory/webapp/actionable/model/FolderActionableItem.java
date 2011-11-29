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

package org.artifactory.webapp.actionable.model;

import com.google.common.collect.Lists;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.CannonicalEnabledActionableFolder;
import org.artifactory.webapp.actionable.RefreshableActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.CopyAction;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.DeleteVersionsAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.MoveAction;
import org.artifactory.webapp.actionable.action.RefreshNodeAction;
import org.artifactory.webapp.actionable.action.ZapAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.List;
import java.util.Set;

/**
 * @author yoavl
 */
public class FolderActionableItem extends RepoAwareActionableItemBase
        implements HierarchicActionableItem, CannonicalEnabledActionableFolder, RefreshableActionableItem {

    /**
     * The folder info of the last element of the compacted folder or the current folder if not compacted.
     */
    private FolderInfo folderInfo;
    private String displayName;
    private ItemAction deleteAction;
    private MoveAction moveAction;
    private CopyAction copyAction;
    private ItemAction zapAction;
    private DeleteVersionsAction delVersions;
    private List<ActionableItem> children;
    private boolean compactAllowed;
    private ItemAction watchAction;

    public FolderActionableItem(org.artifactory.fs.FolderInfo folderInfo, boolean compactAllowed) {
        super(folderInfo.getRepoPath());
        this.folderInfo = folderInfo;
        this.compactAllowed = compactAllowed;
        this.displayName = this.folderInfo.getName();

        if (isCompactAllowed()) {
            compact();
        }

        addActions();
    }

    private void compact() {
        List<FolderInfo> compactedFolders = getRepoService().getWithEmptyChildren(this.folderInfo);

        //Change the icon if compacted
        int size = compactedFolders.size();
        if (size > 1) {
            displayName = calcCompactedDisplayName(compactedFolders);
            folderInfo = compactedFolders.get(size - 1);
        }
    }

    private String calcCompactedDisplayName(List<FolderInfo> folderList) {
        StringBuilder name = new StringBuilder();
        for (FolderInfo jcrFolder : folderList) {
            name.append('/').append(jcrFolder.getName());
        }
        return name.substring(1);
    }

    public boolean isCompactAllowed() {
        return compactAllowed;
    }

    public void setCompactAllowed(boolean compactAllowed) {
        this.compactAllowed = compactAllowed;
    }

    public RepoPath getCanonicalPath() {
        return getFolderInfo().getRepoPath();
    }

    private void addActions() {
        Set<ItemAction> actions = getActions();
        actions.add(new RefreshNodeAction());
        moveAction = new MoveAction();
        actions.add(moveAction);
        copyAction = new CopyAction();
        actions.add(copyAction);
        deleteAction = new DeleteAction();
        actions.add(deleteAction);
        zapAction = new ZapAction();
        actions.add(zapAction);
        delVersions = new DeleteVersionsAction();
        actions.add(delVersions);

        AddonsManager addonsManager = getAddonsProvider();
        WatchAddon watchAddon = addonsManager.addonByType(WatchAddon.class);
        watchAction = watchAddon.getWatchAction(folderInfo.getRepoPath());
        actions.add(watchAction);
    }

    /**
     * The folder info of the last element of the compacted folder or the current folder if not compacted.
     */
    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssClass() {
        return isCompacted() ? ItemCssClass.folderCompact.getCssClass() : ItemCssClass.folder.getCssClass();
    }

    public void refresh() {
        children = null;    // set the children to null will force reload
    }

    public List<ActionableItem> getChildren(AuthorizationService authService) {
        boolean childrenCacheUpToDate = childrenCacheUpToDate(authService);
        if (!childrenCacheUpToDate) {
            RepositoryService repoService = getRepoService();
            List<ItemInfo> items = repoService.getChildren(getCanonicalPath());

            children = Lists.newArrayListWithExpectedSize(items.size());

            for (ItemInfo pathItem : items) {

                //Check if we should return the child
                String relativePath = pathItem.getRelPath();

                RepoPath childRepoPath = InternalRepoPathFactory.create(pathItem.getRepoKey(), relativePath);

                if (!repoService.isRepoPathVisible(childRepoPath)) {
                    continue;
                }

                String name = pathItem.getName();
                //Skip checksum files
                if (NamingUtils.isChecksum(name)) {
                    continue;
                }
                RepoAwareActionableItem child;
                if (pathItem.isFolder()) {
                    child = new FolderActionableItem(((org.artifactory.fs.FolderInfo) pathItem), isCompactAllowed());
                } else {
                    MimeType mimeType = NamingUtils.getMimeType(relativePath);
                    if (mimeType.isArchive()) {
                        child = new ZipFileActionableItem((org.artifactory.fs.FileInfo) pathItem, isCompactAllowed());
                    } else {
                        child = new FileActionableItem((FileInfo) pathItem);
                    }
                }
                children.add(child);
            }
        }
        return children;
    }

    //Child items can potentially be removed externally. If a child node does no longer
    //exists we need to recalculate the children.
    // We don't simply remove the deleted item since it might be a compacted folder and we don't
    // want to repeat the same logic.

    private boolean childrenCacheUpToDate(AuthorizationService authService) {
        if (children == null) {
            return false;
        }
        for (ActionableItem item : children) {
            if (item instanceof RepoAwareActionableItem) {
                RepoAwareActionableItem repoAwareItem = (RepoAwareActionableItem) item;
                RepoPath repoPath = repoAwareItem.getRepoPath();
                if (repoAwareItem instanceof FolderActionableItem) {
                    repoPath = ((FolderActionableItem) repoAwareItem).getCanonicalPath();
                }
                RepositoryService repoService = getRepoService();
                if (!repoService.exists(repoPath) || !repoService.isRepoPathVisible(repoPath)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasChildren(AuthorizationService authService) {
        RepoPath repoPath = getCanonicalPath();
        return getRepoService().hasChildren(repoPath);
    }

    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getCanonicalPath();
        boolean canDelete = authService.canDelete(repoPath);
        if (!canDelete) {
            deleteAction.setEnabled(false);
        }
        if (!canDelete) {
            zapAction.setEnabled(false);
        } else if (!getRepo().isCache()) {
            zapAction.setEnabled(false);
        }

        // only admin can cleanup by version
        if (!authService.isAdmin()) {
            delVersions.setEnabled(false);
        }

        if (!canDelete || NamingUtils.isSystem(repoPath.getPath()) || !authService.canDeployToLocalRepository()) {
            moveAction.setEnabled(false);
        }

        boolean canRead = authService.canRead(repoPath);
        if (!canRead || NamingUtils.isSystem(repoPath.getPath()) || !authService.canDeployToLocalRepository()) {
            copyAction.setEnabled(false);
        }

        if (!canRead || authService.isAnonymous()) {
            watchAction.setEnabled(false);
        }
    }

    private boolean isCompacted() {
        return !getItemInfo().getRepoPath().equals(folderInfo.getRepoPath());
    }

    public boolean hasStatsInfo() {
        return false;
    }
}
