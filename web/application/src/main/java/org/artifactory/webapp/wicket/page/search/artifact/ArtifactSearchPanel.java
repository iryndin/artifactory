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

package org.artifactory.webapp.wicket.page.search.artifact;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.table.groupable.column.GroupableColumn;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.search.BaseSearchPage;
import org.artifactory.webapp.wicket.page.search.BaseSearchPanel;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableArtifactSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableSearchResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Displays the simple artifact searcher
 *
 * @author Noam Tenne
 */
public class ArtifactSearchPanel extends BaseSearchPanel<ArtifactSearchResult> {

    private ArtifactSearchControls searchControls;

    public ArtifactSearchPanel(Page parent, String id, String query) {
        super(parent, id);

        if (StringUtils.isNotEmpty(query)) {
            searchControls.setQuery(query);
            fetchResults(parent);
        }
    }

    @Override
    protected void addSearchComponents(Form form) {
        searchControls = new ArtifactSearchControls();

        TextField searchControl = new TextField("query", new PropertyModel(searchControls, "query"));
        searchControl.setOutputMarkupId(true);
        form.add(searchControl);

        form.add(new HelpBubble("searchHelp", "Artifact name (wildcards are supported)"));
    }

    @Override
    protected ArtifactSearchControls getSearchControles() {
        return searchControls;
    }

    @Override
    protected Class<? extends BaseSearchPage> getMenuPageClass() {
        return ArtifactSearchPage.class;
    }

    @Override
    protected void onNoResults() {
        String searchQuery = StringEscapeUtils.escapeHtml(searchControls.getQuery());
        if (StringUtils.isEmpty(searchQuery)) {
            searchQuery = "";
        } else {
            searchQuery = " for '" + searchQuery + "'";
        }
        Session.get().warn(String.format("No artifacts found%s.", searchQuery));
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected ActionableSearchResult<ArtifactSearchResult> getActionableResult(ArtifactSearchResult searchResult) {
        return new ActionableArtifactSearchResult(searchResult);
    }

    @Override
    protected boolean isLimitSearchResults() {
        return searchControls.isLimitSearchResults();
    }

    @Override
    public String getSearchExpression() {
        return searchControls.getQuery();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new ActionsColumn(""));
        columns.add(new BaseSearchPanel.ArtifactNameColumn());
        columns.add(new GroupableColumn(new Model("Path"), "searchResult.relDirPath", "searchResult.relDirPath"));
        columns.add(new LastModifiedColumn());
        columns.add(new GroupableColumn(new Model("Repository"), "searchResult.repoKey", "searchResult.repoKey"));
    }

    @Override
    protected SearchResults<ArtifactSearchResult> searchArtifacts() {
        return search(searchControls);
    }

    @Override
    protected SearchResults<ArtifactSearchResult> performLimitlessArtifactSearch() {
        ArtifactSearchControls controlsCopy = new ArtifactSearchControls(searchControls);
        controlsCopy.setLimitSearchResults(false);
        return search(controlsCopy);
    }

    /**
     * Performs the search
     *
     * @param controls Search controls
     * @return List of search results
     */
    private SearchResults<ArtifactSearchResult> search(ArtifactSearchControls controls) {
        return searchService.searchArtifacts(controls);
    }

    private static class LastModifiedColumn extends GroupableColumn implements IChoiceRenderer {
        private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("MMM yyyy");
        private static final SimpleDateFormat ID_FORMAT = new SimpleDateFormat("yyyy-MM");

        private LastModifiedColumn() {
            super(new Model("Modified"), "searchResult.lastModified", "searchResult.lastModifiedString");
        }

        public Object getDisplayValue(Object object) {
            return DISPLAY_FORMAT.format(getDate(object));
        }

        public String getIdValue(Object object, int index) {
            return ID_FORMAT.format(getDate(object));
        }

        @SuppressWarnings({"unchecked"})
        private Date getDate(Object object) {
            ActionableSearchResult<ArtifactSearchResult> searchResult =
                    (ActionableSearchResult<ArtifactSearchResult>) object;
            return new Date(searchResult.getSearchResult().getLastModified());
        }
    }
}
