/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.build.tabs;

import com.google.common.collect.Lists;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.webapp.wicket.page.build.page.BuildBrowserRootPage;
import org.artifactory.webapp.wicket.page.build.tabs.compare.ModuleItemListSorter;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;

import java.util.Iterator;
import java.util.List;

import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.*;

/**
 * Displays the build's published modules
 *
 * @author Noam Y. Tenne
 */
public class PublishedModulesTabPanel extends Panel {

    private Build build;

    /**
     * Main constructor
     *
     * @param id    ID to assign to the panel
     * @param build Build to display the modules of
     */
    public PublishedModulesTabPanel(String id, Build build) {
        super(id);
        setOutputMarkupId(true);
        this.build = build;

        addTable();
    }

    /**
     * Adds the published modules table to the panel
     */
    private void addTable() {
        List<IColumn<Module>> columns = Lists.newArrayList();
        columns.add(new AbstractColumn<Module>(Model.of("Module ID"), "id") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                Module module = (Module) cellItem.getParent().getParent().getDefaultModelObject();
                cellItem.add(getModuleNameLink(componentId, module.getId()));
            }
        });
        columns.add(
                new PropertyColumn<Module>(Model.of("Number Of Artifacts"), "artifacts", "artifacts.size"));
        columns.add(new PropertyColumn<Module>(
                Model.of("Number Of Dependencies"), "dependencies", "dependencies.size"));

        ModulesDataProvider dataProvider = new ModulesDataProvider(build.getModules());

        add(new SortableTable<Module>("modules", columns, dataProvider, 50));
    }

    /**
     * Returns a link that redirects to the module info
     *
     * @param componentId ID to assign to the link
     * @param moduleId    ID of module to display
     * @return Module redirection link
     */
    private AjaxLink getModuleNameLink(String componentId, final String moduleId) {
        AjaxLink link = new AjaxLink<String>(componentId, Model.of(moduleId)) {

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                replaceComponentTagBody(markupStream, openTag, moduleId);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters pageParameters = new PageParameters();
                pageParameters.put(BUILD_NAME, build.getName());
                pageParameters.put(BUILD_NUMBER, build.getNumber());
                pageParameters.put(BUILD_STARTED, build.getStarted());
                pageParameters.put(MODULE_ID, moduleId);
                setResponsePage(BuildBrowserRootPage.class, pageParameters);
            }
        };
        link.add(new CssClass("item-link"));
        return link;
    }

    /**
     * The modules table data provider
     */
    private static class ModulesDataProvider extends SortableDataProvider<Module> {

        List<Module> moduleList;

        /**
         * Main constructor
         *
         * @param publishedModules Modules to display
         */
        public ModulesDataProvider(List<Module> publishedModules) {
            setSort("id", true);
            moduleList = (publishedModules != null) ? publishedModules : Lists.<Module>newArrayList();
        }

        public Iterator<Module> iterator(int first, int count) {
            ModuleItemListSorter.sort(moduleList, getSort());
            List<Module> listToReturn = moduleList.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return moduleList.size();
        }

        public IModel<Module> model(Module object) {
            return new Model<Module>(object);
        }
    }
}