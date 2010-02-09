package org.artifactory.webapp.wicket.page.build.tabs;

import org.apache.wicket.RequestCycle;
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
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.Module;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.table.SortableTable;
import org.artifactory.webapp.wicket.page.build.BuildBrowserConstants;
import org.artifactory.webapp.wicket.page.build.tabs.compare.ModuleItemListSorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new AbstractColumn(new Model("Module ID"), "id") {
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                Module module = (Module) cellItem.getParent().getParent().getModelObject();
                cellItem.add(getModuleNameLink(componentId, module.getId()));
            }
        });
        columns.add(new PropertyColumn(new Model("Number Of Artifacts"), "artifacts", "artifacts.size"));
        columns.add(new PropertyColumn(new Model("Number Of Dependencies"), "dependencies", "dependencies.size"));

        ModulesDataProvider dataProvider = new ModulesDataProvider(build.getModules());

        add(new SortableTable("modules", columns, dataProvider, 10));
    }

    /**
     * Returns a link that redirects to the module info
     *
     * @param componentId ID to assign to the link
     * @param moduleId    ID of module to display
     * @return Module redirection link
     */
    private AjaxLink getModuleNameLink(String componentId, final String moduleId) {
        AjaxLink link = new AjaxLink(componentId, new Model(moduleId)) {

            @Override
            protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                replaceComponentTagBody(markupStream, openTag, moduleId);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                String url = new StringBuilder().append(BuildBrowserConstants.BUILDS).append("/").
                        append(build.getName()).append("/").append(build.getNumber()).append("/").append(moduleId).
                        toString();
                RequestCycle.get().setRequestTarget(new RedirectRequestTarget(url));
            }
        };
        link.add(new CssClass("item-link"));
        return link;
    }

    /**
     * The modules table data provider
     */
    private static class ModulesDataProvider extends SortableDataProvider {

        List<Module> moduleList;

        /**
         * Main constructor
         *
         * @param publishedModules Modules to display
         */
        public ModulesDataProvider(List<Module> publishedModules) {
            setSort("id", true);
            moduleList = publishedModules;
        }

        public Iterator iterator(int first, int count) {
            ModuleItemListSorter.sort(moduleList, getSort());
            List<Module> listToReturn = moduleList.subList(first, first + count);
            return listToReturn.iterator();
        }

        public int size() {
            return moduleList.size();
        }

        public IModel model(Object object) {
            return new Model((Module) object);
        }
    }
}