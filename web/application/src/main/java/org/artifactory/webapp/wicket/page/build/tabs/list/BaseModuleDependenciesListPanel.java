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

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.build.api.Dependency;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.webapp.wicket.actionable.column.ActionsColumn;
import org.artifactory.webapp.wicket.page.build.actionable.ModuleDependencyActionableItem;
import org.artifactory.webapp.wicket.page.build.tabs.list.compare.DependencyItemListSorter;

import java.util.ArrayList;
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
     * Returns a list of dependencies to display
     *
     * @return Dependency list to display
     */
    protected abstract List<Dependency> getDependencies();

    /**
     * Returns a list of dependency actionable items
     *
     * @param dependencies Dependencies to create actionable items from
     * @return Dependency actionable item list
     */
    protected abstract List<ModuleDependencyActionableItem> getModuleDependencyActionableItem(
            List<Dependency> dependencies);

    /**
     * Adds the dependencies table
     */
    protected void addTable() {
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new ActionsColumn(""));
        columns.add(new PropertyColumn(new Model("ID"), "id", "dependency.id"));
        columns.add(new PropertyColumn(new Model("Scopes"), "scopes", "dependencyScopes"));
        columns.add(new PropertyColumn(new Model("Type"), "type", "dependency.type"));
        columns.add(new PropertyColumn(new Model("Repo Path"), "repoPathOrMissingMessage"));
        //columns.add(new PropertyColumn(new Model("Required By"), "requiredBy", "dependencyRequiredBy"));

        add(new SortableTable("dependencies", columns, new ModuleDependenciesDataProvider(), 10));
    }

    /**
     * The published module's dependencies table data provider
     */
    private class ModuleDependenciesDataProvider extends SortableDataProvider {

        private List<Dependency> dependenciesList;

        /**
         * Default constructor
         */
        public ModuleDependenciesDataProvider() {
            setSort("dependencyId", true);
            this.dependenciesList = getDependencies();
        }

        public Iterator iterator(int first, int count) {
            /**
             * We use a custom sorter here since we need to sort the scopes and required-by lists that aren't directly
             * comparable
             */
            DependencyItemListSorter.sort(dependenciesList, getSort());
            List<ModuleDependencyActionableItem> listToReturn =
                    getModuleDependencyActionableItem(dependenciesList.subList(first, first + count));
            return listToReturn.iterator();
        }

        public int size() {
            return dependenciesList.size();
        }

        public IModel model(Object object) {
            ModuleDependencyActionableItem item = (ModuleDependencyActionableItem) object;
            item = new ModuleDependencyActionableItem(item.getRepoPath(), item.getDependency()) {
                ;

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