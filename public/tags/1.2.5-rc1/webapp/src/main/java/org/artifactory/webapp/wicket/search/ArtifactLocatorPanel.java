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

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.search.SearchControls;
import org.artifactory.search.SearchHelper;
import org.artifactory.search.SearchResult;
import org.artifactory.webapp.wicket.components.ContentDialogPanel;
import org.artifactory.webapp.wicket.components.panel.TitlePanel;
import org.artifactory.webapp.wicket.help.HelpBubble;
import org.artifactory.webapp.wicket.utils.ComparablePropertySorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class ArtifactLocatorPanel extends TitlePanel {

    private SearchControls searchControls;

    public ArtifactLocatorPanel(String string) {
        super(string);
        searchControls = new SearchControls();

        final ContentDialogPanel pomDialogPanel = new ContentDialogPanel("pomDialog");
        add(pomDialogPanel);
        add(new HelpBubble("searchHelp", "Artifact name (% and _ wildcards are supported)"));

        //Results table
        final SearchResultsDataProvider dataProvider = new SearchResultsDataProvider();
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new Model("Artifact"), "name", "name"));
        columns.add(new PropertyColumn(new Model("Path"), "relDirPath", "relDirPath"));
        columns.add(new PropertyColumn(
                new Model("Modified"), "lastModifiedString", "lastModifiedString"));
        columns.add(new PropertyColumn(new Model("Repository"), "repoKey", "repoKey"));
        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("results", columns, dataProvider, 10) {
                    //Handle row selection
                    @Override
                    protected Item newCellItem(final String id, int index, final IModel model) {
                        Item item = super.newCellItem(id, index, model);
                        item.add(new AjaxEventBehavior("onMouseUp") {
                            protected void onEvent(final AjaxRequestTarget target) {
                                //Show the popup panel
                                SearchResult result =
                                        (SearchResult) getComponent().getParent().getParent()
                                                .getModelObject();
                                ArtifactResource artifact = result.getArtifact();
                                String content = getArtifactMetadataContent(artifact);
                                pomDialogPanel.ajaxUpdate(content, target);
                            }
                        });
                        return item;
                    }
                };
        add(table);

        final TextField searchControl =
                new TextField("search", new PropertyModel(this, "searchControls.search"));
        searchControl.setOutputMarkupId(true);
        searchControl.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onUpdate(AjaxRequestTarget target) {
                List<SearchResult> results = dataProvider.fetchResults();
                target.addComponent(table);
                String tableId = table.getMarkupId();
                //Some visual effect to indicate the receipt of results
                target.appendJavascript(
                        "dojo.lfx.html.fadeOut('" + tableId + "', 200).play();");
                target.appendJavascript(
                        "dojo.html.setOpacity('" + tableId + "', 0);");
                target.appendJavascript(
                        "dojo.lfx.html.fadeIn('" + tableId + "', 500).play();");
            }
        }.setThrottleDelay(Duration.milliseconds(1000)));
        add(searchControl);
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
            return new Model((SearchResult) object);
        }

        @SuppressWarnings({"UnnecessaryLocalVariable"})
        private List<SearchResult> fetchResults() {
            List<SearchResult> resources = SearchHelper.searchArtifacts(searchControls);
            results = resources;
            //Reset sorting (will default to results order by score)
            setSort(null);
            return results;
        }
    }
}
