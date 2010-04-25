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

import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.wicket.component.confirm.AjaxConfirm;
import org.artifactory.common.wicket.component.confirm.ConfirmDialog;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.action.DeleteAction;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.wicket.page.build.BuildBrowserConstants;
import org.artifactory.webapp.wicket.page.build.page.BuildBrowserRootPage;
import org.slf4j.Logger;

import java.util.Set;

/**
 * Deletes the selected build
 *
 * @author Noam Y. Tenne
 */
public class DeleteBuildAction extends ItemAction {

    private static final Logger log = LoggerFactory.getLogger(DeleteBuildAction.class);

    private static String ACTION_NAME = "Delete";
    private BasicBuildInfo basicBuildInfo;

    /**
     * Main constructor
     *
     * @param basicBuildInfo Basic build info of build to delete
     */
    public DeleteBuildAction(BasicBuildInfo basicBuildInfo) {
        super(ACTION_NAME);
        this.basicBuildInfo = basicBuildInfo;
    }

    @Override
    public void onAction(final ItemEvent e) {
        AjaxConfirm.get().confirm(new ConfirmDialog() {
            public String getMessage() {
                return String.format("Are you sure you wish to delete the build '%s' #%s?",
                        basicBuildInfo.getName(), basicBuildInfo.getNumber());
            }

            public void onConfirm(boolean approved, AjaxRequestTarget target) {
                if (approved) {
                    delete(e);
                }
            }
        });
    }

    @Override
    public String getCssClass() {
        return DeleteAction.class.getSimpleName();
    }

    /**
     * Deletes the build
     *
     * @param e Item event
     */
    private void delete(ItemEvent e) {
        AjaxRequestTarget target = e.getTarget();
        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        String buildName = basicBuildInfo.getName();
        long buildNumber = basicBuildInfo.getNumber();

        try {
            buildService.deleteBuild(basicBuildInfo);
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

        Set<BasicBuildInfo> remainingBuilds = buildService.searchBuildsByName(buildName);
        PageParameters pageParameters = new PageParameters();

        if (!remainingBuilds.isEmpty()) {
            pageParameters.put(BuildBrowserConstants.BUILD_NAME, buildName);
        }

        RequestCycle.get().setResponsePage(BuildBrowserRootPage.class, pageParameters);
    }
}