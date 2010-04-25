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

package org.artifactory.webapp.wicket.page.search.gavc;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.table.groupable.column.GroupableColumn;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.search.BaseSearchPage;
import org.artifactory.webapp.wicket.page.search.BaseSearchPanel;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableGavcSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableSearchResult;

import java.util.List;

/**
 * Displays the GAVC searcher
 *
 * @author Noam Tenne
 */
public class GavcSearchPanel extends BaseSearchPanel<GavcSearchResult> {

    private GavcSearchControls searchControls;

    public GavcSearchPanel(final Page parent, String id) {
        super(parent, id);
    }

    @Override
    protected void addSearchComponents(Form form) {
        add(new CssClass("gavc-panel"));
        searchControls = new GavcSearchControls();

        TextField groupIdField = new TextField("groupIdField", new PropertyModel(searchControls, "groupId"));
        groupIdField.setOutputMarkupId(true);
        groupIdField.setPersistent(true);
        form.add(groupIdField);
        form.add(new HelpBubble("groupIdHelp", "The artifact's group id.<br/>* and ? are accepted."));

        TextField artifactIdField = new TextField("artifactIdField", new PropertyModel(searchControls, "artifactId"));
        artifactIdField.setOutputMarkupId(true);
        artifactIdField.setPersistent(true);
        form.add(artifactIdField);
        form.add(new HelpBubble("artifactIdHelp", "The artifact's id.<br/>* and ? are accepted."));

        TextField versionField = new TextField("versionField", new PropertyModel(searchControls, "version"));
        versionField.setPersistent(true);
        versionField.setOutputMarkupId(true);
        form.add(versionField);
        form.add(new HelpBubble("versionHelp", "The artifact's version.<br/>* and ? are accepted."));

        TextField classifierField = new TextField("classifierField", new PropertyModel(searchControls, "classifier"));
        classifierField.setOutputMarkupId(true);
        classifierField.setPersistent(true);
        form.add(classifierField);
        form.add(new HelpBubble("classifierHelp", "The artifact's classifier.<br/>* and ? are accepted."));
    }

    @Override
    protected GavcSearchControls getSearchControls() {
        return searchControls;
    }

    @Override
    protected Class<? extends BaseSearchPage> getMenuPageClass() {
        return GavcSearchPage.class;
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new ActionsColumn(""));

        columns.add(new BaseSearchPanel.ArtifactNameColumn());
        columns.add(new GroupableColumn(new Model("Group ID"), "searchResult.groupId",
                "searchResult.groupId"));
        columns.add(
                new GroupableColumn(new Model("Artifact ID"), "searchResult.artifactId", "searchResult.artifactId"));
        columns.add(new GroupableColumn(new Model("Version"), "searchResult.version", "searchResult.version"));
        columns.add(new GroupableColumn(new Model("Classifier"), "searchResult.classifier", "searchResult.classifier"));
        columns.add(new PropertyColumn(new Model("Repository"), "searchResult.repoKey", "searchResult.repoKey"));
    }

    @Override
    public String getSearchExpression() {
        return searchControls.getSearchExpression();
    }

    @Override
    protected SearchResults<GavcSearchResult> searchArtifacts() {
        return search(searchControls);
    }

    @Override
    protected SearchResults<GavcSearchResult> performLimitlessArtifactSearch() {
        GavcSearchControls controlsCopy = new GavcSearchControls(searchControls);
        controlsCopy.setLimitSearchResults(false);
        return search(controlsCopy);
    }

    @Override
    protected void onNoResults() {
        warnNoArtifacts(searchControls.getSearchExpression());
    }

    @Override
    protected ActionableSearchResult<GavcSearchResult> getActionableResult(GavcSearchResult searchResult) {
        return new ActionableGavcSearchResult(searchResult);
    }

    @Override
    protected boolean isLimitSearchResults() {
        return searchControls.isLimitSearchResults();
    }

    /**
     * Performs the search
     *
     * @param controls Search controls
     * @return List of search results
     */
    private SearchResults<GavcSearchResult> search(GavcSearchControls controls) {
        return searchService.searchGavc(controls);
    }
}