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

package org.artifactory.webapp.actionable.action.metadata;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.actionable.action.ViewAction;
import org.artifactory.webapp.actionable.event.RepoAwareItemEvent;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableMetadataSearchResult;
import org.slf4j.Logger;

/**
 * @author Noam Tenne
 */
public class MetadataViewAction extends ViewAction {

    private static final Logger log = LoggerFactory.getLogger(MetadataViewAction.class);

    @Override
    public void onAction(RepoAwareItemEvent e) {
        RepoAwareActionableItem source = e.getSource();
        isSourceCorrect(source);
        MetadataSearchResult searchResult = getSearchResult(source);
        String metadataName = searchResult.getMetadataName();
        try {
            showHighlightedSourceModal(e, getContent(source.getRepoPath(), searchResult), metadataName, Syntax.xml);
        } catch (RepositoryRuntimeException rre) {
            String logs;
            if (ContextHelper.get().getAuthorizationService().isAdmin()) {
                String systemLogsPage = WicketUtils.absoluteMountPathForPage(SystemLogsPage.class);
                logs = "<a href=\"" + systemLogsPage + "\">log</a>";
            } else {
                logs = "log";
            }
            e.getTarget().getPage().error("Error while retrieving selected metadata. Please review the " + logs +
                    " for further information.");
            log.error("Error while retrieving selected metadata '{}': {}", metadataName, rre.getMessage());
            log.debug("Error while retrieving selected metadata '" + metadataName + "'.", rre);
        }
    }

    private String getContent(RepoPath sourceRepoPath, MetadataSearchResult searchResult) {
        return getRepoService().getXmlMetadata(sourceRepoPath, searchResult.getMetadataName());
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