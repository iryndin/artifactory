/*
 * This file is part of Artifactory.
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

package org.artifactory.webapp.wicket.page.browse.simplebrowser.root;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

import java.util.List;

public class SimpleBrowserRootPage extends AuthenticatedPage {
    @SpringBean
    private RepositoryService repositoryService;

    public SimpleBrowserRootPage() {
        List<VirtualRepoDescriptor> virtualRepos = repositoryService.getVirtualRepoDescriptors();
        add(new RepoListPanel("virtualRepositoriesPanel", virtualRepos));

        List<LocalRepoDescriptor> localRepos = repositoryService.getLocalAndCachedRepoDescriptors();
        add(new RepoListPanel("localRepositoriesPanel", localRepos));
    }

    @Override
    public String getPageName() {
        return "Repository Browser";
    }
}