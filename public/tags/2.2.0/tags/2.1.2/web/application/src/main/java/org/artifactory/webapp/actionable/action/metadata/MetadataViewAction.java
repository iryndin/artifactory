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

package org.artifactory.webapp.actionable.action.metadata;

import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ViewAction;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableMetadataSearchResult;

/**
 * @author Noam Tenne
 */
public class MetadataViewAction extends ViewAction {

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoAwareActionableItem source = e.getSource();
        displayModalWindow(e, getContent(source), getTitle(source));
    }

    private String getContent(RepoAwareActionableItem source) {
        isSourceCorrect(source);
        MetadataSearchResult searchResult = getSearchResult(source);
        return getRepoService().getXmlMetadata(source.getRepoPath(), searchResult.getMetadataName());
    }

    private String getTitle(RepoAwareActionableItem source) {
        isSourceCorrect(source);
        MetadataSearchResult searchResult = getSearchResult(source);
        return searchResult.getName();
    }

    /**
     * Checks that the given source is an instance of the correct actionable search result If not, throws an illegal
     * argument exception
     *
     * @param source The actionable item
     */
    private void isSourceCorrect(RepoAwareActionableItem source) {
        if (!(source instanceof ActionableMetadataSearchResult)) {
            throw new IllegalArgumentException("Actionable item must be an instance of ActionableMetadataSearchResult");
        }
    }

    /**
     * Returns the search result object from the actionable item
     *
     * @param source The actionable item
     * @return MetadataSearchResult - Result object
     */
    private MetadataSearchResult getSearchResult(RepoAwareActionableItem source) {
        return ((ActionableMetadataSearchResult) source).getSearchResult();
    }

    @Override
    public String getCssClass() {
        return ViewAction.class.getSimpleName();
    }
}