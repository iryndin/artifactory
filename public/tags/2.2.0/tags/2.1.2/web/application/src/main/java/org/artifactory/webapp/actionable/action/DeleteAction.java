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

package org.artifactory.webapp.actionable.action;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;

/**
 * @author yoavl
 */
public class DeleteAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "Delete";

    public DeleteAction() {
        super(ACTION_NAME);
    }

    @Override
    public String getConfirmationMessage(ActionableItem actionableItem) {
        return "Are you sure you wish to delete " + actionableItem.getDisplayName() + " ?";
    }

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoPath repoPath = e.getRepoPath();
        getRepoService().undeploy(repoPath);
        removeNodePanel(e);
    }

    private void removeNodePanel(ItemEvent event) {
        WebMarkupContainer nodaPanelContainer = event.getTargetComponents().getNodePanelContainer();
        TreeBrowsePanel browseRepoPanel = (TreeBrowsePanel) nodaPanelContainer.getParent();
        browseRepoPanel.removeNodePanel(event.getTarget());
    }
}