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
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
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
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.build.actionable.BuildActionableItem;
import org.artifactory.webapp.wicket.page.build.page.BuildBrowserRootPage;
import org.artifactory.webapp.wicket.page.build.panel.compare.BuildForNameListSorter;
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
        columns.add(new BuildNumberColumn());
        columns.add(new BuildDateColumn());

        BuildsDataProvider dataProvider = new BuildsDataProvider(buildsByName);

        add(new SortableTable("builds", columns, dataProvider, 200));
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
            BuildForNameListSorter.sort(buildList, getSort());
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

    private void drillDown(BuildActionableItem build) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.put(BUILD_NAME, buildName);
        pageParameters.put(BUILD_NUMBER, build.getBuildNumber());
        pageParameters.put(BUILD_STARTED, build.getStarted());
        setResponsePage(BuildBrowserRootPage.class, pageParameters);
    }

    private class BuildNumberColumn extends AbstractColumn {
        public BuildNumberColumn() {
            super(new Model("Build Number"), "number");
        }

        public void populateItem(final Item cellItem, String componentId, IModel rowModel) {
            final BuildActionableItem build = (BuildActionableItem) cellItem.getParent().getParent().getDefaultModelObject();
            final String buildNumber = build.getBuildNumber();
            Component link = new Label(componentId, buildNumber);
            link.add(new CssClass("item-link"));
            cellItem.add(link);
            cellItem.add(new AjaxEventBehavior("onclick") {
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    drillDown(build);
                }
            });
        }
    }

    private class BuildDateColumn extends FormattedDateColumn {
        public BuildDateColumn() {
            super(new Model("Time Built"), "startedDate", "started", centralConfigService, Build.STARTED_FORMAT);
        }

        @Override
        public void populateItem(final Item item, String componentId, IModel model) {
            super.populateItem(item, componentId, model);
            item.add(new AjaxEventBehavior("onclick") {
                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    final BuildActionableItem build = (BuildActionableItem) item.getParent().getParent().getDefaultModelObject();
                    drillDown(build);
                }
            });
        }
    }
}