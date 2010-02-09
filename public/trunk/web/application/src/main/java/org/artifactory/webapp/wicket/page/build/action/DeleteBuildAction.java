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

package org.artifactory.webapp.wicket.page.build.action;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.build.api.Build;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.slf4j.Logger;

import java.util.List;

import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.BUILDS;

/**
 * Deletes the selected build
 *
 * @author Noam Y. Tenne
 */
public class DeleteBuildAction extends ItemAction {

    private static final Logger log = LoggerFactory.getLogger(DeleteBuildAction.class);

    private static String ACTION_NAME = "Delete";
    private Build build;

    /**
     * Main constructor
     *
     * @param build Build to delete
     */
    public DeleteBuildAction(Build build) {
        super(ACTION_NAME);
        this.build = build;
    }

    @Override
    public void onAction(ItemEvent e) {
        AjaxRequestTarget target = e.getTarget();
        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        String buildName = build.getName();
        long buildNumber = build.getNumber();

        try {
            buildService.deleteBuild(build);
            String info = String.format("Successfully deleted build '%s' #%s.", buildName, buildNumber);
            Session.get().info(info);
            AjaxUtils.refreshFeedback(target);
        } catch (Exception exception) {
            String error = String.format("Exception occurred while deleting build '%s' #%s", buildName, buildNumber);
            log.error(error, e);
            Session.get().error(error);
            AjaxUtils.refreshFeedback(target);
            return;
        }

        List<Build> remainingBuilds = buildService.searchBuildsByName(buildName);

        if (remainingBuilds.isEmpty()) {
            RequestCycle.get().setRequestTarget(new RedirectRequestTarget(BUILDS));
        } else {
            String buildUrl = new StringBuilder().append(BUILDS).append("/").append(buildName).toString();
            RequestCycle.get().setRequestTarget(new RedirectRequestTarget(buildUrl));
        }
    }

    @Override
    public String getCssClass() {
        return DeleteAction.class.getSimpleName();
    }
}