/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.search;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.ConstantsValue;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;
import org.artifactory.webapp.wicket.common.ajax.ImmediateAjaxIndicatorDecorator;
import org.artifactory.webapp.wicket.common.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.webapp.wicket.common.component.SimpleButton;
import org.artifactory.webapp.wicket.common.component.TextContentPanel;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.panel.bordered.BorderedModelPanel;
import org.artifactory.webapp.wicket.common.component.panel.feedback.FeedbackUtils;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.common.component.table.SingleSelectionTable;
import org.artifactory.webapp.wicket.common.component.table.columns.ActionsColumn;
import org.artifactory.webapp.wicket.utils.ListPropertySorter;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ArtifactSearchPanel extends TitledPanel {

    @SpringBean
    private SearchService searchService;

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private CentralConfigService centralConfig;

    private SearchControls searchControls;

    private ModalHandler contentDialog;

    public ArtifactSearchPanel(String id, String query) {
        super(id);
        searchControls = new SearchControls();
        searchControls.setSearch(query);

        contentDialog = new ModalHandler("contentDialog");
        add(contentDialog);

        //Results table
        final SearchResultsDataProvider dataProvider = new SearchResultsDataProvider();
        if (!StringUtils.isEmpty(query)) {
            dataProvider.fetchResults();
        }

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new ActionsColumn(""));

        final String hrefPrefix = WebUtils.getWicketServletContextUrl();
        columns.add(new AbstractColumn(new Model("Artifact"), "searchResult.name") {
            public void populateItem(Item cellItem, String componentId, IModel model) {
                ActionableSearchResult result =
                        (ActionableSearchResult) cellItem.getParent().getParent().getModelObject();
                String href = hrefPrefix + "/" + result.getSearchResult().getRepoKey() + "/" +
                        result.getSearchResult().getPath();
                String name = result.getSearchResult().getName();
                ExternalLink link = new ExternalLink(componentId, href, name);
                link.add(new AttributeAppender("class", new Model("cellItemLink"), " "));
                cellItem.add(link);
            }
        });
        columns.add(new PropertyColumn(new Model("Path"), "searchResult.relDirPath",
                "searchResult.relDirPath"));
        columns.add(new PropertyColumn(
                new Model("Modified"), "searchResult.lastModifiedString",
                "searchResult.lastModifiedString"));
        columns.add(new PropertyColumn(new Model("Repository"), "searchResult.repoKey",
                "searchResult.repoKey"));
        final SingleSelectionTable<ActionableSearchResult> table =
                new SingleSelectionTable<ActionableSearchResult>("results", columns, dataProvider, 20) {
                    public String getSearchExpression() {
                        return searchControls.getSearch();
                    }

                    @Override
                    protected void onRowSelected(ActionableSearchResult selection, AjaxRequestTarget target) {
                        super.onRowSelected(selection, target);
                        FileInfo fileInfo = selection.getSearchResult().getFileInfo();
                        String fileName = fileInfo.getName();
                        if (isPom(fileName)) {
                            String content = repoService.getPomContent(fileInfo);
                            TextContentPanel contentPanel = new TextContentPanel(contentDialog.getContentId());
                            contentPanel.setContent(content);
                            BorderedModelPanel panel = new BorderedModelPanel(contentPanel);
                            panel.setTitle(fileName);
                            contentDialog.setModalPanel(panel);
                            contentDialog.show(target);
                        }
                    }
                };
        add(table);

        //Form
        Form form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        TextField searchControl =
                new TextField("search", new PropertyModel(this, "searchControls.search"));
        searchControl.setOutputMarkupId(true);
        form.add(searchControl);

        form.add(new HelpBubble("searchHelp", "Artifact name (wildcards are supported)"));

        SimpleButton searchButton = new SimpleButton("submit", form, "Search") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                dataProvider.fetchResults();
                table.setCurrentPage(0);    // scroll back to the first page
                target.addComponent(table);
                FeedbackUtils.refreshFeedback(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new ImmediateAjaxIndicatorDecorator();
            }
        };
        form.add(searchButton);

        form.add(new DefaultButtonBehavior(searchButton));
    }

    private boolean isPom(String name) {
        return org.springframework.util.StringUtils.endsWithIgnoreCase(name, ".pom");
    }

    private class SearchResultsDataProvider extends SortableDataProvider {

        private List<ActionableSearchResult> results;

        private SearchResultsDataProvider() {
            results = new ArrayList<ActionableSearchResult>();
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(results, getSort());
            List<ActionableSearchResult> list = results.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return results.size();
        }

        public IModel model(Object object) {
            ActionableSearchResult searchResult = (ActionableSearchResult) object;
            // Format the lsat modified date
            if (searchResult.getSearchResult().getLastModifiedString() == null) {
                long lastModified = searchResult.getSearchResult().getFileInfo().getLastModified();
                searchResult.getSearchResult().setLastModifiedString(centralConfig.format(lastModified));
            }
            return new Model(searchResult);
        }

        private void fetchResults() {
            List<SearchResult> searchResults;
            try {
                searchResults = searchService.searchArtifacts(searchControls);
            } catch (Exception e) {
                Session.get().error("There was an error in the search query.");
                results = Collections.emptyList();
                return;
            }

            if (searchResults.isEmpty()) {
                Session.get().warn(String.format("No Artifacts Found for '%s'.",
                        HtmlUtils.htmlEscape(searchControls.getSearch())));
            }/* else {
                Session.get().info(format("Found %s Artifacts Matching '%s'", searchResults.size(), HtmlUtils.htmlEscape(searchControls.getSearch())));
            }*/
            int maxResults = ConstantsValue.searchMaxResults.getInt();
            if (searchResults.size() > maxResults) {
                Session.get().warn("Displaying first " + maxResults +
                        " results. Please consider refining your search.");
                searchResults = searchResults.subList(0, maxResults);
            }

            results = new ArrayList<ActionableSearchResult>();
            for (SearchResult result : searchResults) {
                ActionableSearchResult searchResult = new ActionableSearchResult(result);
                ItemEventTargetComponents targets =
                        new ItemEventTargetComponents(ArtifactSearchPanel.this, null, contentDialog);
                searchResult.setEventTargetComponents(targets);
                results.add(searchResult);
            }

            //Reset sorting (will default to results order by name)
            setSort(null);
        }
    }
}
