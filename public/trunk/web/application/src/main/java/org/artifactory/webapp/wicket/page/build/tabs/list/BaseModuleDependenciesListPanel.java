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

package org.artifactory.webapp.wicket.page.build.tabs.list;

import com.google.common.collect.Lists;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.groupable.GroupableTable;
import org.artifactory.common.wicket.component.table.groupable.column.GroupableColumn;
import org.artifactory.common.wicket.component.table.groupable.provider.GroupableDataProvider;
import org.artifactory.common.wicket.util.ListPropertySorter;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleDependencyActionableItem;

import java.util.Iterator;
import java.util.List;

/**
 * The base modules dependencies list panel
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseModuleDependenciesListPanel extends TitledPanel {

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public BaseModuleDependenciesListPanel(String id) {
        super(id);
    }

    @Override
    public String getTitle() {
        return "Dependencies";
    }

    /**
     * Returns a list of unpopulated dependency actionable items
     *
     * @return Unpopulated actionable items
     */
    protected abstract List<ModuleDependencyActionableItem> getDependencies();

    /**
     * Populates dependency actionable items with their corresponding repo paths (if exist)
     *
     * @param dependencies Unpopulated actionable items
     * @return Dependency actionable item list
     */
    protected abstract List<ModuleDependencyActionableItem> populateModuleDependencyActionableItem(
            List<ModuleDependencyActionableItem> dependencies);

    /**
     * Adds the dependencies table
     */
    protected void addTable() {
        List<IColumn> columns = Lists.newArrayList();
        columns.add(new ActionsColumn(""));
        columns.add(new PropertyColumn(new Model("ID"), "dependency.id", "dependency.id"));
        columns.add(new GroupableColumn(new Model("Scopes"), "dependencyScope", "dependencyScope"));
        columns.add(new PropertyColumn(new Model("Type"), "dependency.type", "dependency.type"));
        columns.add(new PropertyColumn(new Model("Repo Path"), "repoPathOrMissingMessage"));

        add(new GroupableTable("dependencies", columns, new ModuleDependenciesDataProvider(), 10));
    }

    /**
     * The published module's dependencies table data provider
     */
    private class ModuleDependenciesDataProvider extends GroupableDataProvider {

        private List<ModuleDependencyActionableItem> dependenciesList;

        /**
         * Default constructor
         */
        public ModuleDependenciesDataProvider() {
            setSort("dependencyScope", true);
            setGroupParam(new SortParam("dependencyScope", true));
            setGroupReneder("dependencyScope", new ChoiceRenderer("dependencyScope", "dependencyScope"));
            this.dependenciesList = getDependencies();
        }

        @Override
        public Iterator iterator(int first, int count) {
            ListPropertySorter.sort(dependenciesList, getGroupParam(), getSort());
            List<ModuleDependencyActionableItem> listToReturn =
                    populateModuleDependencyActionableItem(dependenciesList.subList(first, first + count));
            return listToReturn.iterator();
        }

        @Override
        public int size() {
            return dependenciesList.size();
        }

        @Override
        public IModel model(Object object) {
            ModuleDependencyActionableItem item = (ModuleDependencyActionableItem) object;
            item = new ModuleDependencyActionableItem(item.getRepoPath(), item.getDependency()) {
                public Object getRepoPathOrMissingMessage() {
                    if (super.getRepoPath() == null) {
                        return "Not Found: Artifact may have been deleted or overwritten.";
                    } else {
                        return super.getRepoPath();
                    }
                }
            };
            return new Model(item);
        }
    }
}