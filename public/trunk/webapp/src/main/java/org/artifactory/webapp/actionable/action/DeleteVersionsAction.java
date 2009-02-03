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

package org.artifactory.webapp.actionable.action;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.common.component.modal.panel.bordered.BorderedModelPanel;
import org.artifactory.webapp.wicket.page.browse.action.DeleteVersionsPanel;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class DeleteVersionsAction extends RepoAwareItemAction {
    public static final String ACTION_NAME = "Delete Versions ...";

    public DeleteVersionsAction() {
        super(ACTION_NAME, null);
    }

    @Override
    public void onAction(RepoAwareItemEvent event) {
        RepoAwareActionableItem source = event.getSource();
        ItemInfo info = source.getItemInfo();
        RepositoryService repositoryService = getRepoService();
        List<DeployableUnit> deployableUnits =
                repositoryService.getDeployableUnitsUnder(info.getRepoPath());
        Component componentToRefresh = event.getTargetComponents().getRefreshableComponent();

        ItemEventTargetComponents eventTargetComponents = event.getTargetComponents();
        ModalWindow modalWindow = eventTargetComponents.getModalWindow();
        DeleteVersionsPanel panel = new DeleteVersionsPanel(
                modalWindow.getContentId(), deployableUnits, componentToRefresh);
        BorderedModelPanel modelPanel = new BorderedModelPanel(panel);
        modelPanel.setTitle("Delete Versions");
        modalWindow.setContent(modelPanel);
        AjaxRequestTarget target = event.getTarget();
        modalWindow.show(target);
    }

}