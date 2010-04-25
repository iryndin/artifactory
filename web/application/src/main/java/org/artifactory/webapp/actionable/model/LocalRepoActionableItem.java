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
import org.artifactory.addon.wicket.BuildAddon;
import org.artifactory.addon.wicket.WatchAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.mime.MimeType;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.DeleteVersionsAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ZapAction;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class LocalRepoActionableItem extends RepoAwareActionableItemBase
        implements HierarchicActionableItem {
    private ItemAction deleteAction;
    private ItemAction zapAction;
    private DeleteVersionsAction delVersions;
    private ItemAction watchAction;
    private boolean compactAllowed;

    public LocalRepoActionableItem(LocalRepoDescriptor repo) {
        super(new RepoPath(repo.getKey(), ""));
        Set<ItemAction> actions = getActions();
        deleteAction = new RepoDeleteAction();
        actions.add(deleteAction);
        delVersions = new DeleteVersionsAction();
        actions.add(delVersions);
        zapAction = new ZapAction();
        actions.add(zapAction);

        AddonsManager addonsManager = getAddonsProvider();
        WatchAddon watchAddon = addonsManager.addonByType(WatchAddon.class);
        watchAction = watchAddon.getWatchAction(new RepoPath(repo.getKey(), ""));
        actions.add(watchAction);
    }

    public boolean isCompactAllowed() {
        return compactAllowed;
    }

    public void setCompactAllowed(boolean compactAllowed) {
        this.compactAllowed = compactAllowed;
    }

    public String getDisplayName() {
        return getRepoPath().getRepoKey();
    }

    public String getCssClass() {
        if (getRepo().isCache()) {
            return ItemCssClass.repositoryCache.getCssClass();
        } else {
            return ItemCssClass.repository.getCssClass();
        }
    }

    public List<ActionableItem> getChildren(AuthorizationService authService) {
        List<DirectoryItem> items = getRepoService().getDirectoryItems(getRepoPath(), false);
        List<ActionableItem> result = new ArrayList<ActionableItem>(items.size());
        for (DirectoryItem item : items) {
            RepoAwareActionableItem child;
            if (item.isDirectory()) {
                child = new FolderActionableItem((FolderInfo) item.getItemInfo(), isCompactAllowed());
            } else {
                MimeType mimeType = NamingUtils.getContentType(item.getPath());
                if (mimeType.isArchive()) {
                    child = new ZipFileActionableItem((FileInfo) item.getItemInfo(), compactAllowed);
                } else {
                    child = new FileActionableItem((FileInfo) item.getItemInfo());
                }
            }
            result.add(child);
        }
        return result;
    }

    public boolean hasChildren(AuthorizationService authService) {
        RepoPath repoPath = getRepoPath();
        return getRepoService().hasChildren(repoPath);
    }

    public void filterActions(AuthorizationService authService) {
        String key = getRepoPath().getRepoKey();
        boolean isAnonymous = authService.isAnonymous();
        boolean deployer = authService.canDeploy(RepoPath.secureRepoPathForRepo(key));
        boolean canDelete = authService.canDelete(RepoPath.secureRepoPathForRepo(key));
        boolean canRead = authService.canRead(RepoPath.secureRepoPathForRepo(key));

        if (!canDelete) {
            deleteAction.setEnabled(false);
        }

        if (isAnonymous) {
            zapAction.setEnabled(false);
        } else if (!deployer) {
            zapAction.setEnabled(false);
        } else if (!getRepo().isCache()) {
            zapAction.setEnabled(false);
        }

        // only admin can cleanup by version
        if (!authService.isAdmin()) {
            delVersions.setEnabled(false);
        }

        if (!canRead || isAnonymous) {
            watchAction.setEnabled(false);
        }
    }


    private static class RepoDeleteAction extends DeleteAction {

        @Override
        protected String getDeleteSuccessMessage(RepoPath repoPath) {
            return "Successfully deleted repository '" + repoPath.getRepoKey() + "', content.";
        }

        @Override
        public String getDisplayName(ActionableItem actionableItem) {
            return "Delete Content";
        }

        @Override
        public String getCssClass() {
            return DeleteAction.class.getSimpleName();
        }

        @Override
        protected String getDeleteConfirmMessage(RepoAwareItemEvent e) {
            String key = e.getSource().getDisplayName();
            ArtifactCount count = getRepoService().getArtifactCount(key);
            //if remote repo has no cash
            long totalCount = count.getCount();
            StringBuilder builder = new StringBuilder("Are you sure you wish to delete the repository");
            if (totalCount == 0) {
                builder.append("?");
            } else {
                builder.append(" (").append(totalCount).append(" artifacts will be permanently deleted)?");
            }
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            BuildAddon buildAddon = addonsManager.addonByType(BuildAddon.class);
            return buildAddon.getDeleteItemWarningMessage(e.getSource().getItemInfo(), builder.toString());
        }
    }
}
