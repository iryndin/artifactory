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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.BuildRun;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.ActionableItemBase;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ShowInCiServerAction;
import org.artifactory.webapp.wicket.page.build.action.DeleteBuildAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;

/**
 * The build list actionable item
 *
 * @author Noam Y. Tenne
 */
public class BuildActionableItem extends ActionableItemBase {

    private static final Logger log = LoggerFactory.getLogger(BuildActionableItem.class);

    private BuildRun buildRun;

    /**
     * Main constructor
     *
     * @param buildRun Basic build info to act up-on
     */
    public BuildActionableItem(BuildRun buildRun) {
        this.buildRun = buildRun;
        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        try {
            String ciServerUrl = buildService.getBuildCiServerUrl(buildRun);
            if (StringUtils.isNotBlank(ciServerUrl)) {
                getActions().add(new ShowInCiServerAction(ciServerUrl));
            }
        } catch (IOException e) {
            String message = String.format("Unable to extract CI server URL if build '%s' #%s that started at %s",
                    buildRun.getName(), buildRun.getNumber(), buildRun.getStarted());
            log.error(message + ": {}", e.getMessage());
            log.debug(message + ".", e);
        }
    }

    public Panel newItemDetailsPanel(String id) {
        return null;
    }

    public String getDisplayName() {
        return buildRun.getName();
    }

    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    public void filterActions(AuthorizationService authService) {
        if (authService.isAdmin()) {
            getActions().add(new DeleteBuildAction(buildRun));
        }
    }

    /**
     * Returns the started time of the build
     *
     * @return Selected build start time
     */
    public String getStarted() {
        return buildRun.getStarted();
    }

    /**
     * Returns the started time of the build as a date
     *
     * @return Selected build start time as date
     */
    public Date getStartedDate() {
        return buildRun.getStartedDate();
    }

    /**
     * Returns the number of the selected build
     *
     * @return Selected build number
     */
    public String getBuildNumber() {
        return buildRun.getNumber();
    }

    /**
     * Returns the latest release status of the build
     *
     * @return Build latest release status if exists, null if not
     */
    public String getLastReleaseStatus() {
        return buildRun.getReleaseStatus();
    }
}
