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
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.actionable.ItemAction;
import org.artifactory.webapp.actionable.ItemActionEvent;
import static org.artifactory.webapp.actionable.model.ActionDescriptor.REMOVE;
import static org.artifactory.webapp.actionable.model.ActionDescriptor.ZAP;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class LocalRepoActionableItem extends RepoAwareActionableItemBase
        implements HierarchicActionableItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(ActionableItemBase.class);

    public LocalRepoActionableItem(LocalRepoDescriptor repo) {
        super(new RepoPath(repo.getKey(), ""));
        OrderedMap<ActionDescriptor, ItemAction> actions = getActions();
        actions.put(REMOVE, new RemoveItemAction() {
            @Override
            protected void remove() {
                getRepoService().undeploy(getRepoPath());
            }
        });
        actions.put(ZAP, new ItemAction(ZAP.getName()) {
            @Override
            public void actionPerformed(ItemActionEvent e) {
            }
        });
    }

    public String getDisplayName() {
        return getRepoPath().getRepoKey();
    }

    public String getIconRes() {
        if (getRepo().isCache()) {
            return "/images/repository-cache.png";
        } else {
            return "/images/repository.png";
        }
    }

    public List<RepoAwareActionableItem> getChildren(AuthorizationService authService) {
        List<DirectoryItem> items = getRepoService().getDirectoryItems(getRepoPath(), false);
        List<RepoAwareActionableItem> result = new ArrayList<RepoAwareActionableItem>(items.size());
        for (DirectoryItem item : items) {
            RepoAwareActionableItem child;
            if (item.isDirectory()) {
                child = new FolderActionableItem((FolderInfo) item.getItemInfo());
            } else {
                child = new FileActionableItem((FileInfo) item.getItemInfo());
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
        boolean deployer = authService.canDeploy(RepoPath.repoPathForRepo(key));
        OrderedMap<ActionDescriptor, ItemAction> actions = getActions();
        if (!deployer) {
            actions.get(REMOVE).setEnabled(false);
            actions.get(ZAP).setEnabled(false);
        } else if (!getRepo().isCache()) {
            actions.get(ZAP).setEnabled(false);
        }
    }

    public boolean hasStatsInfo() {
        return false;
    }
}
