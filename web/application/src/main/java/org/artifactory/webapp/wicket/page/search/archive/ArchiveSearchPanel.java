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

package org.artifactory.webapp.wicket.page.search.archive;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.table.columns.TitlePropertyColumn;
import org.artifactory.common.wicket.component.table.groupable.column.GroupableColumn;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.page.search.BaseSearchPage;
import org.artifactory.webapp.wicket.page.search.BaseSearchPanel;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableArchiveSearchResult;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableSearchResult;

import java.util.List;

/**
 * Displays the archive content searcher
 *
 * @author Noam Tenne
 */
public class ArchiveSearchPanel extends BaseSearchPanel<ArchiveSearchResult> {

    private ArchiveSearchControls searchControls;

    @WicketProperty
    private boolean excludeInnerClasses;

    @WicketProperty
    private boolean searchAllTypes;

    public ArchiveSearchPanel(Page parent, String id) {
        super(parent, id);
    }

    @Override
    protected void validateSearchControls() {
        if (searchControls.isEmpty()) {
            throw new IllegalArgumentException("The search term cannot be empty.");
        }
    }

    @Override
    protected void addSearchComponents(Form form) {
        searchControls = new ArchiveSearchControls();
        getDataProvider().setGroupParam(new SortParam("searchResult.entry", true));

        TextField searchControl = new TextField<String>("query", new PropertyModel<String>(searchControls, "query"));
        searchControl.setOutputMarkupId(true);
        form.add(searchControl);

        form.add(new StyledCheckbox("excludeInnerClasses", new PropertyModel<Boolean>(this, "excludeInnerClasses")));
        form.add(new HelpBubble("excludeInnerClassesHelp", "Exclude inner classes from the list of results."));

        form.add(new StyledCheckbox("allFileTypes", new PropertyModel<Boolean>(this, "searchAllTypes")));
        form.add(new HelpBubble("allFileTypesHelp", "Search through all types of files (including package names).\n" +
                "Use the full file name including extension, e.g.: 'Driver.properties' or use wildcards.\n" +
                "Please note: providing a search term which is too frequent in an archive, may yield\n" +
                "too many results, which will not be displayed."));

        form.add(new HelpBubble("searchHelp", "Archive entry name.<br/>* and ? are accepted."));

        //Group entry names which are similar but have different character cases
        getDataProvider().setGroupRenderer("searchResult.entry",
                new ChoiceRenderer<ActionableSearchResult<ArchiveSearchResult>>("searchResult.entry",
                        "searchResult.lowerCaseEntry"));
    }

    @Override
    protected ArchiveSearchControls getSearchControls() {
        return searchControls;
    }

    @Override
    protected Class<? extends BaseSearchPage> getMenuPageClass() {
        return ArchiveSearchPage.class;
    }

    @Override
    protected void onNoResults() {
        warnNoArtifacts(searchControls.getQuery());
    }

    @Override
    protected ActionableSearchResult<ArchiveSearchResult> getActionableResult(
            ArchiveSearchResult searchResult) {
        return new ActionableArchiveSearchResult(searchResult);
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
    protected void addColumns(List<IColumn<ActionableSearchResult<ArchiveSearchResult>>> columns) {
        columns.add(new ActionsColumn<ActionableSearchResult<ArchiveSearchResult>>(""));
        columns.add(new GroupableColumn<ActionableSearchResult<ArchiveSearchResult>>(
                "Entry Name", "searchResult.entry", "searchResult.entryPath"));
        columns.add(new BaseSearchPanel.ArtifactNameColumn());
        columns.add(new ArtifactPathColumn());
        //columns.add(new TitlePropertyColumn<ActionableSearchResult<ArchiveSearchResult>>(
        //        "Artifact Path", "searchResult.relDirPath", "searchResult.relDirPath"));
        columns.add(new TitlePropertyColumn<ActionableSearchResult<ArchiveSearchResult>>(
                "Repository", "searchResult.repoKey", "searchResult.repoKey"));
    }

    @Override
    protected ItemSearchResults<ArchiveSearchResult> searchArtifacts() {
        return search(false);
    }

    @Override
    protected ItemSearchResults<ArchiveSearchResult> performLimitlessArtifactSearch() {
        return search(true);
    }

    /**
     * Performs the search
     *
     * @param limitlessSearch True if should perform a limitless search
     * @return List of search results
     */
    private ItemSearchResults<ArchiveSearchResult> search(boolean limitlessSearch) {
        ArchiveSearchControls controlsCopy = new ArchiveSearchControls(searchControls);
        String exp = controlsCopy.getQuery();
        if (!searchAllTypes && !exp.endsWith(".class")) {
            exp += ".class";
            controlsCopy.setQuery(exp);
        }
        if (limitlessSearch) {
            controlsCopy.setLimitSearchResults(false);
            controlsCopy.setShouldCalcEntries(false);
        }
        controlsCopy.setExcludeInnerClasses(excludeInnerClasses);
        return searchService.searchArchiveContent(controlsCopy);
    }

    protected static class ArtifactPathColumn extends GroupableColumn {
        public ArtifactPathColumn() {
            super("Artifact Path", "searchResult.relDirPath", "searchResult.relDirPath");
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void populateItem(final Item cellItem, String componentId, IModel model) {
            final ActionableArchiveSearchResult result =
                    (ActionableArchiveSearchResult) cellItem.getParent().getParent().getDefaultModelObject();
            final String relDirPath = result.getSearchResult().getRelDirPath();
            Link linkToTreeView = new Link<String>(componentId, Model.of(relDirPath)) {
                @Override
                public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                    replaceComponentTagBody(markupStream, openTag, relDirPath);
                }

                @Override
                public void onClick() {
                    RequestCycle.get().setResponsePage(new BrowseRepoPage(result.getArchiveRepoPath()));
                }
            };
            linkToTreeView.add(new CssClass("item-link"));
            cellItem.add(linkToTreeView);
        }

        @Override
        public String getGroupProperty() {
            return "baseName";
        }
    }


}