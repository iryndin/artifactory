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

package org.artifactory.webapp.wicket.page.build.actionable;

import org.artifactory.addon.build.diff.BuildsDiffModel;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.RepoAwareActionableItemBase;
import org.artifactory.webapp.actionable.action.DownloadAction;
import org.artifactory.webapp.actionable.action.ShowInTreeAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

/**
 * @author Shay Yaakov
 */
public class BuildDiffModelActionableItem extends RepoAwareActionableItemBase {

    private BuildsDiffModel model;

    public BuildDiffModelActionableItem(BuildsDiffModel model) {
        super(model.getRepoPath());
        this.model = model;
    }

    public BuildsDiffModel getModel() {
        return model;
    }

    @Override
    public String getDisplayName() {
        return model.getSecondItemName();
    }

    @Override
    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    @Override
    public void filterActions(AuthorizationService authService) {
        RepoPath repoPath = getRepoPath();
        if ((repoPath != null) && (authService.canRead(repoPath))) {
            getActions().add(new ShowInTreeAction());
            getActions().add(new DownloadAction());
        }
    }
}
