/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.util.SetEnableVisitor;
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
        add(new CssClass("disabled-panel"));

        addArtifactsTable();
        addDependenciesTable();

        setEnabled(false);
        visitChildren(new SetEnableVisitor(false));
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