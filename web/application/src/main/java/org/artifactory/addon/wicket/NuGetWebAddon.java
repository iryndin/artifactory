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

package org.artifactory.addon.wicket;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Form;
import org.artifactory.addon.Addon;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;

/**
 * Webapp NuGet functionality interface
 *
 * @author Noam Y. Tenne
 */
public interface NuGetWebAddon extends Addon {

    /**
     * Assemble the repo NuGet configuration section and add it to the given form
     *
     * @param form           Repo configuration form
     * @param repoDescriptor Configured repo descriptor
     */
    void createAndAddRepoConfigNuGetSection(Form form, RepoDescriptor repoDescriptor);

    /**
     * Returns the virtual repository's NuGet configuration tab
     *
     * @param tabTitle       Tab title
     * @param repoDescriptor Descriptor of currently edited/created virtual repository
     * @return Constructed tab
     */
    ITab getVirtualRepoConfigurationTab(String tabTitle, VirtualRepoDescriptor repoDescriptor);

    /**
     * Creates an HTTP method to test the connectivity of the remote repo. Type of method and URL are subject to the
     * repo configuration
     *
     * @param repoUrl Repo URL; always ends with forward slash
     * @param repo    Descriptor of currently configured repo
     * @return HTTP method to execute
     */
    HttpMethodBase getRemoteRepoTestMethod(String repoUrl, HttpRepoDescriptor repo);
}
