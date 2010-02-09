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

package org.artifactory.webapp.actionable.model;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.CopyAction;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.DeleteVersionsAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.MoveAction;
import org.artifactory.webapp.actionable.action.ZapAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yoavl
 */
public class FolderActionableItem extends RepoAwareActionableItemBase implements HierarchicActionableItem {

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

    public FolderActionableItem(FolderInfo folderInfo) {
        this(folderInfo, true);
    }

    public FolderActionableItem(FolderInfo folderInfo, boolean compactAllowed) {
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

    /**
     * The repo path of the last element of the compacted folder or the current folder.
     *
     * @return the actual canonical repo path of this folder
     */
    public RepoPath getCanonicalPath() {
        return getFolderInfo().getRepoPath();
    }

    private void addActions() {
        Set<ItemAction> actions = getActions();
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

    public List<ActionableItem> getChildren(AuthorizationService authService) {
        boolean childrenCacheUpToDate = childrenCacheUpToDate();
        if (!childrenCacheUpToDate) {
            List<DirectoryItem> items = getRepoService().getDirectoryItems(getCanonicalPath(), false);
            children = new ArrayList<ActionableItem>(items.size());
            for (DirectoryItem dirItem : items) {
                ItemInfo item = dirItem.getItemInfo();
                //Check if we should return the child
                RepoPath childRepoPath = new RepoPath(item.getRepoKey(), item.getRelPath());
                boolean childReader = authService.canRead(childRepoPath);
                if (!childReader) {
                    //Don't bother with stuff that we do not have read access to
                    continue;
                }
                String name = item.getName();
                //Skip checksum files
                if (NamingUtils.isChecksum(name)) {
                    continue;
                }
                RepoAwareActionableItem child;
                if (item.isFolder()) {
                    child = new FolderActionableItem(((FolderInfo) item), isCompactAllowed());
                } else {
                    child = new FileActionableItem(((FileInfo) item));
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

    private boolean childrenCacheUpToDate() {
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
                if (!repoService.exists(repoPath)) {
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

        if (!canDelete || NamingUtils.isSystem(repoPath.getPath()) ||
                !authService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            moveAction.setEnabled(false);
        }

        if (!canDelete || NamingUtils.isSystem(repoPath.getPath()) ||
                !authService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            copyAction.setEnabled(false);
        }

        boolean canRead = authService.canRead(repoPath);
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
