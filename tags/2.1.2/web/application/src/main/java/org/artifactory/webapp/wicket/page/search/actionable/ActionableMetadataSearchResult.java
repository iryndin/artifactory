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

package org.artifactory.webapp.wicket.page.search.actionable;

import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ShowInTreeAction;
import org.artifactory.webapp.actionable.action.metadata.MetadataDownloadAction;
import org.artifactory.webapp.actionable.action.metadata.MetadataViewAction;

import java.util.Set;

/**
 * @author Noam Tenne
 */
public class ActionableMetadataSearchResult<T extends MetadataSearchResult> extends ActionableSearchResult<T> {

    public ActionableMetadataSearchResult(T searchResult) {
        super(searchResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addActions(Set<ItemAction> actions) {
        actions.add(new MetadataViewAction());
        actions.add(new MetadataDownloadAction());
        actions.add(new ShowInTreeAction());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filterActions(AuthorizationService authService) {
    }

    @Override
    public T getSearchResult() {
        return super.getSearchResult();
    }
}
