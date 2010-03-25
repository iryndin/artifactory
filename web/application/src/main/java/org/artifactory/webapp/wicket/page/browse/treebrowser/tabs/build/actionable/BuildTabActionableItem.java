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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
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

    private BasicBuildInfo basicBuildInfo;
    private String moduleId;

    /**
     * Main constructor
     *
     * @param textContentViewer Modal handler for displaying the build XML
     * @param basicBuildInfo    Basic build info object to handle
     * @param moduleId          ID of module association to specify
     */
    public BuildTabActionableItem(ModalHandler textContentViewer, BasicBuildInfo basicBuildInfo, String moduleId) {
        this.basicBuildInfo = basicBuildInfo;
        this.moduleId = moduleId;

        getActions().add(new GoToBuildAction(basicBuildInfo));
        getActions().add(new ViewBuildJsonAction(textContentViewer, basicBuildInfo));

        try {
            BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
            String ciServerUrl = buildService.getBuildCiServerUrl(basicBuildInfo);
            if (StringUtils.isNotBlank(ciServerUrl)) {
                getActions().add(new ShowInCiServerAction(ciServerUrl));
            }
        } catch (IOException e) {
            String buildName = basicBuildInfo.getName();
            long buildNumber = basicBuildInfo.getNumber();
            String buildStarted = basicBuildInfo.getStarted();
            String message = String.format("Unable to extract CI server URL if build '%s' #%s that started at %s",
                    buildName, buildNumber, buildStarted);
            log.error(message + ": {}", e.getMessage());
            log.debug(message + ".", e);
        }
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