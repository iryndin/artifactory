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

package org.artifactory.webapp.wicket.page.search;

import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.persistence.IValuePersister;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.wicket.SearchAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.wicket.ajax.ImmediateAjaxIndicatorDecorator;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.navigation.NavigationToolbarWithDropDown;
import org.artifactory.common.wicket.component.table.groupable.GroupableTable;
import org.artifactory.common.wicket.component.table.groupable.column.GroupableColumn;
import org.artifactory.common.wicket.component.table.groupable.provider.GroupableDataProvider;
import org.artifactory.common.wicket.persister.EscapeCookieValuePersister;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.page.search.actionable.ActionableSearchResult;
import org.artifactory.webapp.wicket.panel.advanced.AdvancedSearchPanel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basic search panel for the different search types to extend
 *
 * @author Noam Tenne
 */
public abstract class BaseSearchPanel<T extends SearchResult> extends Panel implements LimitlessCapableSearcher<T> {

    @SpringBean
    protected SearchService searchService;

    @SpringBean
    protected RepositoryService repoService;

    @SpringBean
    protected CentralConfigService centralConfig;

    @SpringBean
    private AddonsManager addons;

    private SearchResults<T> searchResults;
    private GroupableDataProvider<ActionableSearchResult<T>> dataProvider;
    private WebMarkupContainer searchBorder;

    public BaseSearchPanel(final Page parent, String id) {
        super(id);
        searchBorder = new WebMarkupContainer("searchBorder");
        searchBorder.setOutputMarkupId(true);
        add(searchBorder);

        // Results table
        dataProvider = new GroupableDataProvider<ActionableSearchResult<T>>();

        List<IColumn> columns = new ArrayList<IColumn>();
        addColumns(columns);

        final GroupableTable table = new GroupableTable("results", columns, dataProvider, 20) {
            public String getSearchExpression() {
                return BaseSearchPanel.this.getSearchExpression();
            }

            public String getSearchCount() {
                int maxResults = ConstantValues.searchMaxResults.getInt();
                long fullResultCount = searchResults.getFullResultsCount();
                String searchExpression = BaseSearchPanel.this.getSearchExpression();

                StringBuilder msg = new StringBuilder();
                //Return this only if we limit the search results and don't return the full number of results found
                if (isLimitSearchResults() && (fullResultCount > maxResults)) {
                    msg.append(getRowCount()).append(" out of ").append(fullResultCount).append(" matches found for '").
                            append(searchExpression).append("'");
                } else if (searchExpression == null) {
                    msg.append(getRowCount()).append(" matches found ");
                } else {
                    msg.append(getRowCount()).append(" matches found for '").append(searchExpression).append("'");
                }
                String timeStr = NumberFormat.getNumberInstance().format(searchResults.getTime());
                msg.append(" (").append(timeStr).append(" ms)");
                return msg.toString();
            }

            @Override
            protected NavigationToolbarWithDropDown getDropDownNavToolbar() {
                return new NavigationToolbarWithDropDown(this, 0);
            }
        };
        searchBorder.add(table);

        //Form
        Form form = new Form("form") {
            @Override
            protected IValuePersister getValuePersister() {
                return new EscapeCookieValuePersister();
            }
        };
        form.setOutputMarkupId(true);
        searchBorder.add(form);

        addSearchComponents(form);

        //selected repo for search
        CompoundPropertyModel advancedModel = new CompoundPropertyModel(getSearchControles());
        AdvancedSearchPanel advancedPanel = new AdvancedSearchPanel("advancedPanel", advancedModel);
        form.add(advancedPanel);

        SearchAddon searchAddon = addons.addonByType(SearchAddon.class);
        SaveSearchResultsPanel saveSearchResultsPanel = searchAddon.getSaveSearchResultsPanel("saveResultsPanel",
                new PropertyModel(this, "searchResults.results"), this);
        searchBorder.add(saveSearchResultsPanel);

        TitledAjaxSubmitLink searchButton = new TitledAjaxSubmitLink("submit", "Search", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                onSearch();
                fetchResults(parent);
                table.setCurrentPage(0);    // scroll back to the first page
                target.addComponent(table);
                target.addComponent(searchBorder);
                AjaxUtils.refreshFeedback(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ImmediateAjaxIndicatorDecorator();
            }
        };
        addSearchButton(form, searchButton);

        form.add(new DefaultButtonBehavior(searchButton));
    }

    protected void onSearch() {
    }

    protected void addSearchButton(Form form, TitledAjaxSubmitLink searchButton) {
        form.add(searchButton);
    }

    protected abstract void addSearchComponents(Form form);

    protected abstract Object getSearchControles();

    protected abstract Class<? extends BaseSearchPage> getMenuPageClass();

    protected abstract void addColumns(List<IColumn> columns);

    public abstract String getSearchExpression();

    /**
     * Performs the search
     *
     * @return List of results limited by the system spec
     */
    protected abstract SearchResults<T> searchArtifacts();

    /**
     * Performs a limitless result search
     *
     * @return List of results unlimited by the system spec
     */
    protected abstract SearchResults<T> performLimitlessArtifactSearch();

    protected abstract void onNoResults();

    protected abstract ActionableSearchResult<T> getActionableResult(T searchResult);

    /**
     * Indicates if the search results should be limited as in the system spec
     *
     * @return True if the search results should be limited
     */
    protected abstract boolean isLimitSearchResults();

    @SuppressWarnings({"unchecked"})
    protected void fetchResults(Page parent) {
        List<T> searchResultList;
        try {
            searchResults = searchArtifacts();
            searchResultList = searchResults.getResults();
        } catch (Exception e) {
            dataProvider.setData((List<ActionableSearchResult<T>>) Collections.EMPTY_LIST);
            Session.get().error("There was an error while searching: " + e.getMessage());
            getSaveSearchResultsPanel().updateState();
            return;
        }

        if (searchResultList.isEmpty()) {
            onNoResults();
        } else {
            //Session.get().info(format("Found %s Artifacts Matching '%s'", searchResults.size(),
            // HtmlUtils.htmlEscape(searchControls.getSearch())));
        }

        getSaveSearchResultsPanel().updateState();

        int maxResults = ConstantValues.searchMaxResults.getInt();

        //Display this only if we limit the search results and don't return the full number of results found
        if (isLimitSearchResults() && (searchResultList.size() > maxResults)) {
            Session.get().warn("Displaying first " + maxResults + " out of " + searchResults.getFullResultsCount()
                    + " results. Please consider refining your search.");
            searchResultList = searchResultList.subList(0, maxResults);
        }

        List<ActionableSearchResult<T>> actionableSearchResults = new ArrayList<ActionableSearchResult<T>>();
        for (T result : searchResultList) {
            ActionableSearchResult<T> searchResult = getActionableResult(result);
            ModalWindow contentDialog = ModalHandler.getInstanceFor(parent);
            ItemEventTargetComponents targets =
                    new ItemEventTargetComponents(this, null, contentDialog);
            searchResult.setEventTargetComponents(targets);
            actionableSearchResults.add(searchResult);
        }

        //Reset sorting (will default to results order by name)
        dataProvider.setData(actionableSearchResults);
        dataProvider.setSort(null);
    }

    public SaveSearchResultsPanel getSaveSearchResultsPanel() {
        return (SaveSearchResultsPanel) searchBorder.get("saveResultsPanel");
    }

    protected class ArtifactNameColumn extends GroupableColumn implements IChoiceRenderer {
        public ArtifactNameColumn() {
            this("Artifact", "searchResult.name");
        }

        public ArtifactNameColumn(String columnName, String propertyExpression) {
            this(new Model(columnName), propertyExpression);
        }

        public ArtifactNameColumn(IModel displayModel, String propertyExpression) {
            super(displayModel, propertyExpression);
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void populateItem(Item cellItem, String componentId, IModel model) {
            final String hrefPrefix = RequestUtils.getWicketServletContextUrl();
            ActionableSearchResult<SearchResult> result =
                    (ActionableSearchResult<SearchResult>) cellItem.getParent().getParent().getModelObject();
            String href = hrefPrefix + "/" + result.getSearchResult().getRepoKey() + "/" +
                    result.getSearchResult().getRelativePath();
            String name = result.getSearchResult().getName();
            ExternalLink link = new ExternalLink(componentId, href, name);
            link.add(new CssClass("item-link"));
            cellItem.add(link);
        }

        public Object getDisplayValue(Object object) {
            return getArtifactName(object);
        }

        @SuppressWarnings({"unchecked"})
        private String getArtifactName(Object object) {
            ActionableSearchResult<SearchResult> result = (ActionableSearchResult<SearchResult>) object;
            return FilenameUtils.getBaseName(result.getSearchResult().getName());
        }

        public String getIdValue(Object object, int index) {
            return getArtifactName(object);
        }
    }

    public GroupableDataProvider<ActionableSearchResult<T>> getDataProvider() {
        return dataProvider;
    }

    public List<T> searchLimitlessArtifacts() {
        SearchResults<T> results = performLimitlessArtifactSearch();
        return results.getResults();
    }
}