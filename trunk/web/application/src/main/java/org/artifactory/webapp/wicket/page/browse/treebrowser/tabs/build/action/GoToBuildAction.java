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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.wicket.page.build.BuildBrowserConstants;

/**
 * Redirects to the build history view of the given build name
 *
 * @author Noam Y. Tenne
 */
public class GoToBuildAction extends ItemAction {

    private static String ACTION_NAME = "Go To Build";
    private String buildName;
    private long buildNumber;

    /**
     * Main constructor
     *
     * @param buildName   Name of build to go to
     * @param buildNumber Number of build to go to
     */
    public GoToBuildAction(String buildName, long buildNumber) {
        super(ACTION_NAME);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    @Override
    public void onAction(ItemEvent e) {
        String url = new StringBuilder().append(BuildBrowserConstants.BUILDS).append("/").append(buildName).append("/").
                append(buildNumber).toString();
        RequestCycle.get().setRequestTarget(new RedirectRequestTarget(url));
    }
}