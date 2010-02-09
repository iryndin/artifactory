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

package org.artifactory.webapp.wicket.page.build.actionable;

import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.api.Build;
import org.artifactory.webapp.actionable.ActionableItemBase;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ShowInCiServerAction;
import org.artifactory.webapp.wicket.page.build.action.DeleteBuildAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

/**
 * The build list actionable item
 *
 * @author Noam Y. Tenne
 */
public class BuildActionableItem extends ActionableItemBase {
    private Build build;

    /**
     * Main constructor
     *
     * @param build Build to act up-on
     */
    public BuildActionableItem(Build build) {
        this.build = build;
        getActions().add(new ShowInCiServerAction(build.getUrl()));
    }

    public Panel newItemDetailsPanel(String id) {
        return null;
    }

    public String getDisplayName() {
        return build.getName();
    }

    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    public void filterActions(AuthorizationService authService) {
        if (authService.isAdmin()) {
            getActions().add(new DeleteBuildAction(build));
        }
    }

    /**
     * Returns the selected build object
     *
     * @return Selected build object
     */
    public Build getBuild() {
        return build;
    }

    /**
     * Returns the number of the selected build
     *
     * @return Selected build number
     */
    public Long getBuildNumber() {
        return build.getNumber();
    }
}