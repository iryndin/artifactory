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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.BuildRun;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.actionable.ActionableItemBase;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.GoToBuildAction;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ShowInCiServerAction;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ViewBuildJsonAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Artifact associated build actionable item
 *
 * @author Noam Y. Tenne
 */
public class BuildTabActionableItem extends ActionableItemBase {

    private static final Logger log = LoggerFactory.getLogger(BuildTabActionableItem.class);

    private String moduleId;
    private BuildRun buildRun;
    private ViewBuildJsonAction viewJsonAction;

    /**
     * Main constructor
     *
     * @param textContentViewer Modal handler for displaying the build XML
     * @param buildRun          Basic build info object to handle
     * @param moduleId          ID of module association to specify
     */
    public BuildTabActionableItem(ModalHandler textContentViewer, BuildRun buildRun, String moduleId) {
        this.buildRun = buildRun;
        this.moduleId = moduleId;

        getActions().add(new GoToBuildAction(buildRun, moduleId));
        viewJsonAction = new ViewBuildJsonAction(textContentViewer, buildRun);
        getActions().add(viewJsonAction);

        try {
            BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
            String ciServerUrl = buildService.getBuildCiServerUrl(buildRun);
            if (StringUtils.isNotBlank(ciServerUrl)) {
                getActions().add(new ShowInCiServerAction(ciServerUrl));
            }
        } catch (IOException e) {
            String buildName = buildRun.getName();
            String buildNumber = buildRun.getNumber();
            String buildStarted = buildRun.getStarted();
            String message = String.format("Unable to extract CI server URL if build '%s' #%s that started at %s",
                    buildName, buildNumber, buildStarted);
            log.error(message + ": {}", e.getMessage());
            log.debug(message + ".", e);
        }
    }

    @Override
    public Panel newItemDetailsPanel(String id) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return buildRun.getName();
    }

    @Override
    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    @Override
    public void filterActions(AuthorizationService authService) {
        if (!authService.canDeployToLocalRepository()) {
            viewJsonAction.setEnabled(false);
        }
    }

    /**
     * Returns the ID of the module to specify association with
     *
     * @return Module ID association to specify
     */
    public String getModuleId() {
        return moduleId;
    }
}