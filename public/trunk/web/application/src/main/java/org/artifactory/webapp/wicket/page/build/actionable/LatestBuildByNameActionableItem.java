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

package org.artifactory.webapp.wicket.page.build.actionable;

import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.webapp.actionable.ActionableItemBase;
import org.artifactory.webapp.wicket.page.build.action.DeleteAllBuildsAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.util.Date;

/**
 * Actionable item for the All Builds list
 *
 * @author Noam Y. Tenne
 */
public class LatestBuildByNameActionableItem extends ActionableItemBase {

    private BasicBuildInfo basicBuildInfo;

    /**
     * Main constructor
     *
     * @param basicBuildInfo Basic info of selected builds
     */
    public LatestBuildByNameActionableItem(BasicBuildInfo basicBuildInfo) {
        this.basicBuildInfo = basicBuildInfo;
    }

    public Panel newItemDetailsPanel(String id) {
        return null;
    }

    public String getDisplayName() {
        return basicBuildInfo.getName();
    }

    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    public void filterActions(AuthorizationService authService) {
        if (authService.isAdmin()) {
            getActions().add(new DeleteAllBuildsAction(basicBuildInfo.getName()));
        }
    }

    /**
     * Returns the name of the build
     *
     * @return Selected build name
     */
    public String getName() {
        return basicBuildInfo.getName();
    }

    /**
     * Returns the started time of the build
     *
     * @return Selected build start time
     */
    public String getStarted() {
        return basicBuildInfo.getStarted();
    }

    /**
     * Returns the started time of the build as a date
     *
     * @return Selected build start time as date
     */
    public Date getStartedDate() {
        return basicBuildInfo.getStartedDate();
    }
}