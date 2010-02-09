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
package org.artifactory.webapp.wicket.search;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.webapp.wicket.component.NotifingFeedbackPanel;
import org.artifactory.webapp.wicket.component.SimpleButton;
import org.artifactory.webapp.wicket.component.TextContentPanel;
import org.artifactory.webapp.wicket.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.component.table.SingleSelectionTable;
import org.artifactory.webapp.wicket.help.HelpBubble;
import org.artifactory.webapp.wicket.utils.ComparablePropertySorter;
import org.artifactory.webapp.wicket.utils.WebUtils;

import java.util.ArrayList;
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

    private FeedbackPanel feedback;

    public ArtifactSearchPanel(String string) {
        super(string);
        searchControls = new SearchControls();

        final ModalWindow textContentViewer = new ModalWindow("contentDialog");
        add(textContentViewer);

        feedback = new NotifingFeedbackPanel("feedback");
        add(feedback);

        //Results table
        final SearchResultsDataProvider dataProvider = new SearchResultsDataProvider();
        List<IColumn> columns = new ArrayList<IColumn>();
        final String hrefPrefix = WebUtils.getWicketServletContextUrl();
        columns.add(new AbstractColumn(new Model("Artifact"), "name") {
            public void populateItem(Item cellItem, String componentId, IModel model) {
                SearchResult result =
                        (SearchResult) cellItem.getParent().getParent().getModelObject();
                String href = hrefPrefix + "/" + result.getRepoKey() + "/" + result.getPath();
                String name = result.getName();
                ExternalLink link = new ExternalLink(componentId, href, name);
                link.add(new AttributeAppender("class", new Model("cellItemLink"), " "));
                cellItem.add(link);
            }
        });
        columns.add(new PropertyColumn(new Model("Path"), "relDirPath", "relDirPath"));
        columns.add(new PropertyColumn(
                new Model("Modified"), "lastModifiedString", "lastModifiedString"));
        columns.add(new PropertyColumn(new Model("Repository"), "repoKey", "repoKey"));
        final SingleSelectionTable<SearchResult> table =
                new SingleSelectionTable<SearchResult>("results", columns, dataProvider, 20) {
                    @Override
                    protected void onRowSelected(SearchResult selection, AjaxRequestTarget target) {
                        super.onRowSelected(selection, target);
                        String content = repoService.getContent(selection.getFileInfo());
                        TextContentPanel contentPanel =
                                new TextContentPanel(textContentViewer.getContentId());
                        contentPanel.setContent(content);
                        textContentViewer.setContent(contentPanel);
                        textContentViewer.show(target);
                    }
                };
        add(table);

        //Form
        Form form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        final TextField searchControl =
                new TextField("search", new PropertyModel(this, "searchControls.search"));
        searchControl.setOutputMarkupId(true);
        form.add(searchControl);

        form.add(new HelpBubble("searchHelp", "Artifact name (% and _ wildcards are supported)"));

        SimpleButton submit = new SimpleButton("submit", form, "Go!") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                dataProvider.fetchResults();
                target.addComponent(feedback);
                target.addComponent(table);
            }
        };
        form.add(submit);
    }

    private class SearchResultsDataProvider extends SortableDataProvider {

        private List<SearchResult> results;

        public SearchResultsDataProvider() {
            //Load the results
            results = new ArrayList<SearchResult>();
        }

        public Iterator iterator(int first, int count) {
            ComparablePropertySorter<SearchResult> sorter =
                    new ComparablePropertySorter<SearchResult>(SearchResult.class);
            SortParam sp = getSort();
            if (sp != null) {
                sorter.sort(results, sp);
            }
            List<SearchResult> list = results.subList(first, first + count);
            return list.iterator();
        }

        public int size() {
            return results.size();
        }

        public IModel model(Object object) {
            SearchResult searchResult = (SearchResult) object;
            // Format the lsat modified date
            if (searchResult.getLastModifiedString() == null) {
                long lastModified = searchResult.getFileInfo().getLastModified();
                searchResult.setLastModifiedString(centralConfig.format(lastModified));
            }
            return new Model(searchResult);
        }

        private void fetchResults() {
            List<SearchResult> resources = searchService.searchArtifacts(searchControls);
            int maxResults = ArtifactoryConstants.searchMaxResults;
            if (resources.size() > maxResults) {
                info("Displaying first " + maxResults +
                        " results. Please consider refining your search.");
                List<SearchResult> sublistResults = resources.subList(0, maxResults);
                //Need to copy to a new list in order to be serializable
                results = new ArrayList<SearchResult>();
                results.addAll(sublistResults);
            } else {
                results = resources;
            }
            //Reset sorting (will default to results order by score)
            setSort(null);
        }
    }
}
