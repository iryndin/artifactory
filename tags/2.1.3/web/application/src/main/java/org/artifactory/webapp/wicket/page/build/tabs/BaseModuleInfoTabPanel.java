package org.artifactory.webapp.wicket.page.build.tabs;

import org.apache.wicket.markup.html.panel.Panel;

/**
 * The base module information panel
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseModuleInfoTabPanel extends Panel {

    /**
     * Main constructor
     *
     * @param id ID to assign to the panel
     */
    public BaseModuleInfoTabPanel(String id) {
        super(id);
    }

    /**
     * Returns the published module artifacts list panel
     *
     * @param id ID to assign to the panel
     * @return Module artifacts list panel
     */
    protected abstract Panel getModuleArtifactsListPanel(String id);

    /**
     * Returns the published module dependency list panel
     *
     * @param id ID to assign to the panel
     * @return Module dependencies list panel
     */
    protected abstract Panel getModuleDependenciesListPanel(String id);

    /**
     * Adds the module artifacts table
     */
    protected void addArtifactsTable() {
        add(getModuleArtifactsListPanel("artifactsPanel"));
    }

    /**
     * Adds the module dependencies table
     */
    protected void addDependenciesTable() {
        add(getModuleDependenciesListPanel("dependenciesPanel"));
    }
}