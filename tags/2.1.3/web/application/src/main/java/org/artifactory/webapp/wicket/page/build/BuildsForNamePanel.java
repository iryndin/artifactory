package org.artifactory.webapp.wicket.page.build;

import com.google.common.collect.Lists;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.search.SearchService;
import org.artifactory.build.api.Build;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.build.actionable.BuildActionableItem;
import org.artifactory.webapp.wicket.page.build.compare.BuildItemListSorter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Displays all the builds of a given name
 *
 * @author Noam Y. Tenne
 */
public class BuildsForNamePanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(BuildsForNamePanel.class);

    @SpringBean
    private SearchService searchService;

    private String buildName;

    /**
     * Main constructor
     *
     * @param id              ID to assign to the panel
     * @param buildName       The name of the builds to display
     * @param buildsToDisplay List of builds to display
     */
    public BuildsForNamePanel(String id, String buildName, List<Build> buildsToDisplay) {
        super(id);
        setOutputMarkupId(true);
        this.buildName = buildName;

        try {
            addTable(buildsToDisplay);
        } catch (Exception e) {
            String errorMessage = "An error occurred while loading the builds with the name '" + buildName + "'";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public String getTitle() {
        return "History For Build '" + buildName + "'";
    }

    /**
     * Adds the build table to the panel
     *
     * @param buildsToAdd Builds to display
     */
    private void addTable(List<Build> buildsToAdd) {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new ActionsColumn(""));
        columns.add(new AbstractColumn(new Model("Build Number"), "number") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                BuildActionableItem build = (BuildActionableItem) cellItem.getParent().getParent().getModelObject();
                cellItem.add(getBuildNumberLink(componentId, build.getBuildNumber()));
            }
        });
        columns.add(new PropertyColumn(new Model("Time Built"), "startedDate", "build.started"));

        BuildsDataProvider dataProvider = new BuildsDataProvider(buildsToAdd);

        add(new SortableTable("builds", columns, dataProvider, 200));
    }

    /**
     * Returns a link that redirects to the build info panel of the given build object
     *
     * @param componentId ID to assign to the link
     * @param buildNumber Number of build to display
     * @return Link to the build info panel
     */
    private AjaxLink getBuildNumberLink(String componentId, final long buildNumber) {
        AjaxLink link = new AjaxLink(componentId, new Model(buildNumber)) {

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                replaceComponentTagBody(markupStream, openTag, Long.toString(buildNumber));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                String url = new StringBuilder().append(BuildBrowserConstants.BUILDS).append("/").append(buildName).
                        append("/").append(buildNumber).toString();
                RequestCycle.get().setRequestTarget(new RedirectRequestTarget(url));
            }
        };
        link.add(new CssClass("item-link"));
        return link;
    }

    /**
     * The build table data provider
     */
    private static class BuildsDataProvider extends SortableDataProvider {

        List<Build> buildList;

        /**
         * Main constructor
         *
         * @param builds Builds to display
         */
        public BuildsDataProvider(List<Build> builds) {
            setSort("number", false);
            this.buildList = builds;
        }

        public Iterator iterator(int first, int count) {
            /**
             * We use a custom sorter here since we need to sort the date objects that aren't directly accessible
             * through the build bean
             */
            BuildItemListSorter.sort(buildList, getSort());
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
        private List<BuildActionableItem> getActionableItems(List<Build> builds) {
            List<BuildActionableItem> actionableItems = Lists.newArrayList();

            for (Build build : builds) {
                actionableItems.add(new BuildActionableItem(build));
            }

            return actionableItems;
        }
    }
}