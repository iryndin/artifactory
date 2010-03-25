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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build;

import com.google.common.collect.Lists;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchService;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.common.wicket.component.table.columns.FormattedDateColumn;
import org.artifactory.common.wicket.component.table.masterdetail.MasterDetailEntry;
import org.artifactory.common.wicket.component.table.masterdetail.MasterDetailTable;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable.BuildDependencyActionableItem;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable.BuildTabActionableItem;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;

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
    private CentralConfigService centralConfigService;

    @SpringBean
    protected BuildService buildService;

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
     * Adds the build tables
     */
    private void addBuildTables() {
        // add artifacts border
        FieldSetBorder artifactsBorder = new FieldSetBorder("artifactBorder");
        add(artifactsBorder);

        List<IColumn> buildColumns = Lists.newArrayList();
        buildColumns.add(new ActionsColumn(""));
        buildColumns.add(new PropertyColumn(new Model("Build Name"), "basicBuildInfo.name", "basicBuildInfo.name"));
        buildColumns.add(new PropertyColumn(new Model("Build Number"), "basicBuildInfo.number", "basicBuildInfo.number"));
        buildColumns.add(new FormattedDateColumn(new Model("Build Started"), "basicBuildInfo.startedDate", "basicBuildInfo.started", centralConfigService, Build.STARTED_FORMAT));
        buildColumns.add(new PropertyColumn(new Model("Module ID"), "moduleId"));

        artifactsBorder.add(new SortableTable("artifactBuilds", buildColumns, new ArtifactsDataProvider(getArtifactBuilds()), 10));

        // add dependencies border
        FieldSetBorder dependenciesBorder = new FieldSetBorder("dependencyBorder");
        add(dependenciesBorder);

        List<IColumn> dependencyColumns = Lists.newArrayList();
        dependencyColumns.add(new DependencyActionsColumn());
        dependencyColumns.add(new PropertyColumn(new Model("Build Name"), "master.name", "master.name"));
        dependencyColumns.add(new PropertyColumn(new Model("Build Number"), "master.number", "master.number"));
        dependencyColumns.add(new PropertyColumn(new Model("Module ID"), "detail.moduleId", "detail.moduleId"));
        dependencyColumns.add(new PropertyColumn(new Model("Scope"), "detail.scope", "detail.scope"));

        dependenciesBorder.add(new UsedByTable("dependencyBuilds", dependencyColumns));
    }

    /**
     * Returns the list of artifact basic build info items to display
     *
     * @return Artifact basic build info list
     */
    protected abstract List<BasicBuildInfo> getArtifactBuilds();

    /**
     * Returns the list of dependency basic build info items to display
     *
     * @return Dependency basic build info item list
     */
    protected abstract List<BasicBuildInfo> getDependencyBuilds();

    /**
     * Returns the list of artifact build actionable items to display
     *
     * @param builds Basic build infos to create actionable items from
     * @return Artifact build actionable item list
     */
    protected abstract List<BuildTabActionableItem> getArtifactActionableItems(List<BasicBuildInfo> builds);

    /**
     * Returns the list of dependency build actionable items to display
     *
     * @param basicInfo Basic build info to create actionable items from
     * @return Dependency build actionable item list
     */
    protected abstract List<BuildDependencyActionableItem> getDependencyActionableItems(BasicBuildInfo basicInfo);

    private class UsedByTable extends MasterDetailTable<BasicBuildInfo, BuildDependencyActionableItem> {
        public UsedByTable(String id, List<IColumn> columns) {
            super(id, columns, BaseBuildsTabPanel.this.getDependencyBuilds(), 10);
        }

        @Override
        protected String getMasterLabel(BasicBuildInfo masterObject) {
            return String.format("%s, Build #%s", masterObject.getName(), masterObject.getNumber());
        }

        @Override
        protected List<BuildDependencyActionableItem> getDetails(BasicBuildInfo masterObject) {
            return getDependencyActionableItems(masterObject);
        }
    }

    private static class DependencyActionsColumn extends ActionsColumn {
        public DependencyActionsColumn() {
            super("");
        }

        @SuppressWarnings({"unchecked"})
        @Override
        protected ActionableItem getRowObject(IModel rowModel) {
            return ((MasterDetailEntry<BasicBuildInfo, BuildTabActionableItem>) rowModel.getObject()).getDetail();
        }
    }


    /**
     * The artifacts table data provider
     */
    private class ArtifactsDataProvider extends SortableDataProvider {
        List<BasicBuildInfo> builds;

        public ArtifactsDataProvider(List<BasicBuildInfo> builds) {
            setSort("buildStarted", false);
            this.builds = builds;
        }

        protected List<BuildTabActionableItem> getActionableItems(List<BasicBuildInfo> builds) {
            return getArtifactActionableItems(builds);
        }

        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(builds, getSort());
            List<BuildTabActionableItem> listToReturn = getActionableItems(builds.subList(first, first + count));
            return listToReturn.iterator();
        }

        public int size() {
            return builds.size();
        }

        public IModel model(Object object) {
            return new Model((BuildTabActionableItem) object);
        }
    }

}