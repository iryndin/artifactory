/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable.model;

import org.apache.commons.collections15.OrderedMap;
import org.apache.log4j.Logger;
import org.artifactory.api.common.PackagingType;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.ItemAction;
import org.artifactory.webapp.actionable.ItemActionEvent;
import static org.artifactory.webapp.actionable.model.ActionDescriptor.REMOVE;
import static org.artifactory.webapp.actionable.model.ActionDescriptor.ZAP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class FolderActionableItem extends RepoAwareActionableItemBase
        implements HierarchicActionableItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ActionableItemBase.class);

    private FolderInfo folder;
    private String displayName;
    private RepoPath compactedFolderRepoPath;

    private List<RepoAwareActionableItem> children;

    public FolderActionableItem(FolderInfo folder) {
        super(folder.getRepoPath());
        this.folder = folder;
        displayName = folder.getName();
        //Check whether the folder can be compacted for empty middle folders
        List<FolderInfo> folderList = getRepoService().getWithEmptyChildren(folder);
        for (int i = 1; i < folderList.size(); i++) {
            FolderInfo jcrFolder = folderList.get(i);
            displayName += '/' + jcrFolder.getName();
        }
        //Change the icon if compacted
        int size = folderList.size();
        if (size > 1) {
            this.folder = folderList.get(size - 1);
            compactedFolderRepoPath = this.folder.getRepoPath();
        }
        OrderedMap<ActionDescriptor, ItemAction> actions = getActions();
        actions.put(REMOVE, new ItemAction(REMOVE.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
                getRepoService().undeploy(getRepoPath());
            }
        });
        actions.put(ZAP, new ItemAction(ZAP.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
            }
        });
    }

    public FolderInfo getFolder() {
        if (folder == null) {
            if (compactedFolderRepoPath == null) {
                folder = (FolderInfo) getItemInfo();
            } else {
                folder = (FolderInfo) getItemInfo(compactedFolderRepoPath);
            }
        }
        return folder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconRes() {
        return compactedFolderRepoPath != null ?
                "/images/folder-compact.png" : "/images/folder.png";
    }

    public List<RepoAwareActionableItem> getChildren(AuthorizationService authService) {
        if (children == null) {
            List<DirectoryItem> items =
                    getRepoService().getDirectoryItems(getFolder().getRepoPath(), false);
            children = new ArrayList<RepoAwareActionableItem>(items.size());
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
                if (PackagingType.isChecksum(name)) {
                    continue;
                }
                RepoAwareActionableItem child;
                if (item.isFolder()) {
                    child = new FolderActionableItem(((FolderInfo) item));
                } else {
                    child = new FileActionableItem(((FileInfo) item));
                }
                children.add(child);
            }
        } else {
            //Child items can potentially be removed externally. If a child node does no longer
            //exists we need to remove it from the cache.
            //Need an external collection to avoid ConcurrentModificationException
            List<RepoAwareActionableItem> childrenToRemove =
                    new ArrayList<RepoAwareActionableItem>(children.size());
            for (RepoAwareActionableItem item : children) {
                RepositoryService repoService = getRepoService();
                RepoPath repoPath = item.getRepoPath();
                boolean exists = repoService.exists(repoPath);
                if (!exists) {
                    childrenToRemove.add(item);
                }
            }
            //Now remove waht's needed
            for (RepoAwareActionableItem item : childrenToRemove) {
                children.remove(item);
            }
        }
        return children;
    }

    public boolean hasChildren(AuthorizationService authService) {
        RepoPath repoPath = getFolder().getRepoPath();
        return getRepoService().hasChildren(repoPath);
    }

    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getFolder().getRepoPath();
        OrderedMap<ActionDescriptor, ItemAction> actions = getActions();
        boolean canDelete = authService.canDelete(repoPath);
        if (!canDelete) {
            actions.get(REMOVE).setEnabled(false);
        }
        boolean canDeploy = authService.canDeploy(repoPath);
        if (!canDeploy) {
            actions.get(ZAP).setEnabled(false);
        } else if (!getRepo().isCache()) {
            actions.get(ZAP).setEnabled(false);
        }
    }

    public boolean hasStatsInfo() {
        return false;
    }
}