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

package org.artifactory.webapp.wicket.page.build.panel;

import com.google.common.collect.Lists;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.FormattedDateColumn;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.build.BuildBrowserConstants;
import org.artifactory.webapp.wicket.page.build.page.BuildBrowserRootPage;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Locates and displays all the builds in the system
 *
 * @author Noam Y. Tenne
 */
public class AllBuildsPanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(AllBuildsPanel.class);

    @SpringBean
    private SearchService searchService;

    @SpringBean
    private CentralConfigService centralConfigService;

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public AllBuildsPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        try {
            Set<BasicBuildInfo> latestBuildsByName = searchService.getLatestBuildsByName();
            addTable(latestBuildsByName);
        } catch (RepositoryRuntimeException e) {
            String errorMessage = "An error occurred while loading all existing builds";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public String getTitle() {
        return "All Builds";
    }

    /**
     * Adds the build table to the panel
     *
     * @param latestBuildsByName Latest builds by name to display
     */
    private void addTable(Set<BasicBuildInfo> latestBuildsByName) {
        List<IColumn> columns = Lists.newArrayList();
        columns.add(new AbstractColumn(new Model("Build Name"), "name") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                BasicBuildInfo info = (BasicBuildInfo) cellItem.getParent().getParent().getModelObject();
                cellItem.add(getBuildNameLink(componentId, info.getName()));
            }
        });
        columns.add(new FormattedDateColumn(new Model("Last Built"), "startedDate", "started", centralConfigService,
                Build.STARTED_FORMAT));
        BuildsDataProvider dataProvider = new BuildsDataProvider(latestBuildsByName);
        add(new SortableTable("builds", columns, dataProvider, 200));
    }

    /**
     * Returns a link that redirects to the build history panel of the given build name
     *
     * @param componentId ID to assign to the link
     * @param buildName   Build name to to locate
     * @return Link to the build history panel
     */
    private AjaxLink getBuildNameLink(String componentId, final String buildName) {
        AjaxLink link = new AjaxLink(componentId, new Model(buildName)) {

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                replaceComponentTagBody(markupStream, openTag, buildName);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters pageParameters = new PageParameters();
                pageParameters.put(BuildBrowserConstants.BUILD_NAME, buildName);
                setResponsePage(BuildBrowserRootPage.class, pageParameters);
            }
        };
        link.add(new CssClass("item-link"));
        return link;
    }

    /**
     * The build table data provider
     */
    private static class BuildsDataProvider extends SortableDataProvider {

        List<BasicBuildInfo> buildList;

        /**
         * Main constructor
         *
         * @param latestBuildsByName Latest build by name to display
         */
        public BuildsDataProvider(Set<BasicBuildInfo> latestBuildsByName) {
            setSort("name", true);
            this.buildList = Lists.newArrayList(latestBuildsByName);
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(buildList, getSort());
            List<BasicBuildInfo> listToReturn = buildList.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return buildList.size();
        }

        public IModel model(Object object) {
            return new Model((BasicBuildInfo) object);
        }
    }
}