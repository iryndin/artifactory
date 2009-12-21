package org.artifactory.webapp.wicket.page.build;

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
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchService;
import org.artifactory.build.api.Build;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.build.compare.BuildItemListSorter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Locates and displays all the builds in the system
 *
 * @author Noam Y. Tenne
 */
public class AllBuildsPanel extends TitledPanel {

    private static final Logger log = LoggerFactory.getLogger(AllBuildsPanel.class);

    @SpringBean
    private SearchService searchService;

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public AllBuildsPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        try {
            List<Build> latestBuilds = searchService.getLatestBuildsByName();
            addTable(latestBuilds);
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
     * @param buildsToAdd Builds to display
     */
    private void addTable(List<Build> buildsToAdd) {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new AbstractColumn(new Model("Build Name"), "name") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                Build build = (Build) cellItem.getParent().getParent().getModelObject();
                cellItem.add(getBuildNameLink(componentId, build.getName()));
            }
        });
        columns.add(new PropertyColumn(new Model("Last Built"), "startedDate", "started"));

        BuildsDataProvider dataProvider = new BuildsDataProvider(buildsToAdd);

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
                String url = new StringBuilder().append(BuildBrowserConstants.BUILDS).append("/").append(buildName).
                        toString();
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
         * @param buildList Builds to display
         */
        public BuildsDataProvider(List<Build> buildList) {
            setSort("name", true);
            this.buildList = buildList;
        }

        public Iterator iterator(int first, int count) {
            /**
             * We use a custom sorter here since we need to sort the date objects that aren't directly accessible
             * through the build bean
             */
            BuildItemListSorter.sort(buildList, getSort());
            List<Build> listToReturn = buildList.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return buildList.size();
        }

        public IModel model(Object object) {
            return new Model((Build) object);
        }
    }
}