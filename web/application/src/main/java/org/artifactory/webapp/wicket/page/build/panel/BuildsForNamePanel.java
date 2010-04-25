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
import org.artifactory.api.search.SearchService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.FormattedDateColumn;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.build.actionable.BuildActionableItem;
import org.artifactory.webapp.wicket.page.build.page.BuildBrowserRootPage;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.*;

/**
 * Displays all the builds of a given name
 *
 * @author Noam Y. Tenne
 */
public class BuildsForNamePanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(BuildsForNamePanel.class);

    @SpringBean
    private SearchService searchService;

    @SpringBean
    private CentralConfigService centralConfigService;

    private String buildName;

    /**
     * Main constructor
     *
     * @param id           ID to assign to the panel
     * @param buildName    The name of the builds to display
     * @param buildsByName Set of builds to display
     */
    public BuildsForNamePanel(String id, String buildName, Set<BasicBuildInfo> buildsByName) {
        super(id);
        setOutputMarkupId(true);
        this.buildName = buildName;

        try {
            addTable(buildsByName);
        } catch (Exception e) {
            String errorMessage = "An error occurred while loading the builds with the name '" + buildName + "'";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public String getTitle() {
        return "History for Build '" + buildName + "'";
    }

    /**
     * Adds the build table to the panel
     *
     * @param buildsByName Builds to display
     */
    private void addTable(Set<BasicBuildInfo> buildsByName) {
        List<IColumn> columns = Lists.newArrayList();

        columns.add(new ActionsColumn(""));
        columns.add(new AbstractColumn(new Model("Build Number"), "number") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                BuildActionableItem build = (BuildActionableItem) cellItem.getParent().getParent().getModelObject();
                String buildNumberAsString = Long.toString(build.getBuildNumber());
                cellItem.add(getBuildNumberLink(componentId, buildNumberAsString, build.getStarted()));
            }
        });
        columns.add(new FormattedDateColumn(new Model("Time Built"), "startedDate", "started", centralConfigService,
                Build.STARTED_FORMAT));

        BuildsDataProvider dataProvider = new BuildsDataProvider(buildsByName);

        add(new SortableTable("builds", columns, dataProvider, 200));
    }

    /**
     * Returns a link that redirects to the build info panel of the given build object
     *
     * @param componentId  ID to assign to the link
     * @param buildNumber  Number of build to display
     * @param buildStarted Start time of build to display
     * @return Link to the build info panel
     */
    private AjaxLink getBuildNumberLink(String componentId, final String buildNumber, final String buildStarted) {
        AjaxLink link = new AjaxLink(componentId, new Model(buildNumber)) {

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                replaceComponentTagBody(markupStream, openTag, buildNumber);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters pageParameters = new PageParameters();
                pageParameters.put(BUILD_NAME, buildName);
                pageParameters.put(BUILD_NUMBER, buildNumber);
                pageParameters.put(BUILD_STARTED, buildStarted);
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
         * @param buildsByName Builds to display
         */
        public BuildsDataProvider(Set<BasicBuildInfo> buildsByName) {
            setSort("number", false);
            this.buildList = Lists.newArrayList(buildsByName);
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(buildList, getSort());
            List<BuildActionableItem> listToReturn = getActionableItems(buildList.subList(first, first + count));
            return listToReturn.iterator();
        }

        public int size() {
            return buildList.size();
        }

        public IModel model(Object object) {
            return new Model((BuildActionableItem) object);
        }

        /**
         * Returns a list of actionable items for the given builds
         *
         * @param builds Builds to display
         * @return Actionable item list
         */
        private List<BuildActionableItem> getActionableItems(List<BasicBuildInfo> builds) {
            List<BuildActionableItem> actionableItems = Lists.newArrayList();

            for (BasicBuildInfo build : builds) {
                actionableItems.add(new BuildActionableItem(build));
            }

            return actionableItems;
        }
    }
}