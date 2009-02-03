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

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.DeleteVersionsAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.RemoveAction;
import org.artifactory.webapp.actionable.action.ZapAction;
import org.artifactory.webapp.wicket.common.component.panel.actionable.maven.MavenMetadataTabPanel;
import org.artifactory.webapp.wicket.utils.CssClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yoavl
 */
public class FolderActionableItem extends RepoAwareActionableItemBase implements HierarchicActionableItem {

    private FolderInfo folder;
    private String displayName;
    private RepoPath compactedFolderRepoPath;
    private ItemAction removeAction;
    private ItemAction zapAction;
    private DeleteVersionsAction delVersions;

    private List<RepoAwareActionableItem> children;

    public FolderActionableItem(FolderInfo folder) {
        super(folder.getRepoPath());
        this.folder = folder;
        displayName = folder.getName();

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

        addActions();
    }

    private boolean hasViewableMetadata() {
        return getRepoService().hasXmlMetdata(getCanonicalPath(), MavenNaming.MAVEN_METADATA_NAME);
    }

    @Override
    public void addTabs(List<ITab> tabs) {
        super.addTabs(tabs);
        if (hasViewableMetadata()) {
            // add pom view panel
            tabs.add(new AbstractTab(new Model("Maven Metadata")) {
                @Override
                public Panel getPanel(String panelId) {
                    return new MavenMetadataTabPanel(panelId, FolderActionableItem.this);
                }
            });
        }
    }

    /**
     * The repo path of the last element of the compacted folder or the current folder.
     *
     * @return the actual canonical repo path of this folder
     */
    public RepoPath getCanonicalPath() {
        return getFolder().getRepoPath();
    }

    private void addActions() {
        Set<ItemAction> actions = getActions();
        removeAction = new RemoveAction();
        actions.add(removeAction);
        zapAction = new ZapAction();
        actions.add(zapAction);
        delVersions = new DeleteVersionsAction();
        actions.add(delVersions);
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

    public String getCssClass() {
        return compactedFolderRepoPath != null ?
                CssClass.folderCompact.cssClass() : CssClass.folder.cssClass();
    }

    public List<RepoAwareActionableItem> getChildren(AuthorizationService authService) {
        if (children == null) {
            List<DirectoryItem> items =
                    getRepoService().getDirectoryItems(getCanonicalPath(), false);
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
                if (NamingUtils.isChecksum(name)) {
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
            //Now remove what's needed
            for (RepoAwareActionableItem item : childrenToRemove) {
                children.remove(item);
            }
        }
        return children;
    }

    public boolean hasChildren(AuthorizationService authService) {
        RepoPath repoPath = getCanonicalPath();
        return getRepoService().hasChildren(repoPath);
    }

    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getCanonicalPath();
        boolean canDelete = authService.canDelete(repoPath);
        if (!canDelete) {
            removeAction.setEnabled(false);
        }
        boolean canAdmin = authService.canAdmin(repoPath);
        if (!canAdmin) {
            zapAction.setEnabled(false);
        } else if (!getRepo().isCache()) {
            zapAction.setEnabled(false);
        }

        // only admin can cleanup by version
        if (!authService.isAdmin()) {
            delVersions.setEnabled(false);
        }
    }

    public boolean hasStatsInfo() {
        return false;
    }
}
