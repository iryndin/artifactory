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

package org.artifactory.webapp.actionable.action.metadata;

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.action.DownloadAction;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableMetadataSearchResult;

/**
 * @author Noam Tenne
 */
public class MetadataDownloadAction extends DownloadAction {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDownloadPath(ActionableItem actionableItem) {
        if (!(actionableItem instanceof ActionableMetadataSearchResult)) {
            throw new IllegalArgumentException("Actionable item must be an instance of ActionableMetadataSearchResult");
        }
        ActionableMetadataSearchResult metadataResult = (ActionableMetadataSearchResult) actionableItem;
        RepoPath repoPath = metadataResult.getRepoPath();
        MetadataSearchResult result = metadataResult.getSearchResult();
        String metadataName = result.getMetadataName();
        String path =
                RequestUtils.getWicketServletContextUrl() + "/" + repoPath.getRepoKey() + "/" + repoPath.getPath();
        return NamingUtils.getMetadataPath(path, metadataName);
    }

    @Override
    public String getCssClass() {
        return DownloadAction.class.getSimpleName();
    }
}
