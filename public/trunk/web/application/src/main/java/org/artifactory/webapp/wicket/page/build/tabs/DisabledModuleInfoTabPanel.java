package org.artifactory.webapp.wicket.page.build.tabs;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.webapp.wicket.page.build.tabs.list.DisabledModuleArtifactsListPanel;
import org.artifactory.webapp.wicket.page.build.tabs.list.DisabledModuleDependenciesListPanel;

/**
 * The disabled base module information panel
 *
 * @author Noam Y. Tenne
 */
public class DisabledModuleInfoTabPanel extends BaseModuleInfoTabPanel {

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public DisabledModuleInfoTabPanel(String id) {
        super(id);
        add(new CssClass("disabled-tab-panel"));

        addArtifactsTable();
        addDependenciesTable();

        setEnabled(false);
        visitChildren(new IVisitor() {
            public Object component(Component component) {
                component.setEnabled(false);
                return CONTINUE_TRAVERSAL;
            }
        });
    }

    @Override
    protected Panel getModuleArtifactsListPanel(String id) {
        return new DisabledModuleArtifactsListPanel(id);
    }

    @Override
    protected Panel getModuleDependenciesListPanel(String id) {
        return new DisabledModuleDependenciesListPanel(id);
    }
}