package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable.BuildTabActionableItem;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The repo item build association tab base panel
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildsTabPanel extends Panel {

    private static final Logger log = LoggerFactory.getLogger(BaseBuildsTabPanel.class);

    @SpringBean
    protected SearchService searchService;

    protected String sha1;
    protected String md5;
    protected ModalHandler textContentViewer;

    /**
     * Main constructor
     *
     * @param id   ID to assign to the panel
     * @param item Selected repo item
     */
    public BaseBuildsTabPanel(String id, RepoAwareActionableItem item) {
        super(id);

        sha1 = ((FileInfo) item.getItemInfo()).getSha1();
        md5 = ((FileInfo) item.getItemInfo()).getMd5();
        textContentViewer = new ModalHandler("contentDialog");
        add(textContentViewer);

        try {
            addBuildTables();
        } catch (RepositoryRuntimeException rre) {
            String errorMessage = "An error occurred while loading the build associations of '" +
                    item.getRepoPath().getId() + "'";
            log.error(errorMessage, rre);
            error(errorMessage);
        }
    }

    /**
     * Returns the list of artifact build actionable items to display
     *
     * @return Artifact build actionable item list
     */
    protected abstract List<BuildTabActionableItem> getArtifactActionableItems();

    /**
     * Returns the list of dependency build actionable items to display
     *
     * @return Dependency build actionable item list
     */
    protected abstract List<BuildTabActionableItem> getDependencyActionableItems();

    /**
     * Adds the build tables
     */
    private void addBuildTables() {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new ActionsColumn(""));
        columns.add(new PropertyColumn(new Model("Build Name"), "build.name", "build.name"));
        columns.add(new PropertyColumn(new Model("Build Number"), "build.number", "build.number"));
        columns.add(new PropertyColumn(new Model("Build Started"), "buildStartedDate", "build.started"));
        columns.add(new PropertyColumn(new Model("Module ID"), "moduleId", "moduleId"));

        FieldSetBorder artifactsBorder = new FieldSetBorder("artifactBorder");
        FieldSetBorder dependenciesBorder = new FieldSetBorder("dependencyBorder");

        add(artifactsBorder);
        add(dependenciesBorder);

        artifactsBorder.add(new SortableTable("artifactBuilds", columns,
                new BuildTabDataProvider(getArtifactActionableItems()), 10));
        dependenciesBorder.add(new SortableTable("dependencyBuilds", columns,
                new BuildTabDataProvider(getDependencyActionableItems()), 10));
    }

    /**
     * The repo item's build tab table data provider
     */
    private static class BuildTabDataProvider extends SortableDataProvider {

        List<BuildTabActionableItem> buildsToDisplay;

        /**
         * Main constructor
         *
         * @param buildsToDisplay Builds to display
         */
        public BuildTabDataProvider(List<BuildTabActionableItem> buildsToDisplay) {
            setSort("buildStarted", false);
            this.buildsToDisplay = buildsToDisplay;
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(buildsToDisplay, getSort());
            List<BuildTabActionableItem> listToReturn = buildsToDisplay.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return buildsToDisplay.size();
        }

        public IModel model(Object object) {
            return new Model((BuildTabActionableItem) object);
        }
    }
}