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

package org.artifactory.addon.wicket;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.artifactory.addon.Addon;
import org.artifactory.common.wicket.component.LabeledValue;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.modal.panel.EditValueButtonRefreshBehavior;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.repo.RepoPath;
import org.jfrog.build.api.Build;

/**
 * @author Tomer Cohen
 */
public interface LicensesWebAddon extends Addon {

    /**
     * Get the licenses menu node.
     *
     * @param nodeName
     * @return The licenses menu node.
     */
    MenuNode getLicensesMenuNode(String nodeName);

    /**
     * Get the licenses info tab that is displayed in the {@code builds} page
     *
     * @param title            The title of the tab.
     * @param build
     * @param hasDeployOnLocal
     * @return The tab.
     */
    ITab getLicensesInfoTab(String title, Build build, boolean hasDeployOnLocal);

    /**
     * Get the licenses label for the "General info" panel.
     *
     * @param id       The wicket id
     * @param repoPath The {@link RepoPath} where the property is.
     * @return The licenses label
     */
    LabeledValue getLicenseLabel(String id, RepoPath repoPath);

    AbstractLink getEditLicenseLink(String id, RepoPath path, String currentValues, LabeledValue licensesLabel);

    AbstractLink getAddLicenseLink(String id, RepoPath path, String currentValues, LabeledValue licensesLabel);

    AbstractLink getDeleteLink(String id, RepoPath path, String currentValues, FieldSetBorder border);

    /**
     * Get the the panel to change the panel on an artifact according to its current values, and all licenses
     * defined in Artifactory
     *
     * @param refreshBehavior The behavior to refresh a panel after the update has taken place.
     * @param path            The {@link RepoPath} of the artifact that should have its license updated.
     * @param currentValues   The current license names that are attached to the artifact now (if any)
     * @return A Drag and drop panel to change the current license on the artifact.
     */
    BaseModalPanel getChangeLicensePanel(EditValueButtonRefreshBehavior refreshBehavior, RepoPath path,
            String currentValues);
}
