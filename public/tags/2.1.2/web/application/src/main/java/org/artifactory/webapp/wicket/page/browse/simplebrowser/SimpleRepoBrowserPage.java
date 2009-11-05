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

package org.artifactory.webapp.wicket.page.browse.simplebrowser;

import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.webapp.servlet.RepoFilter;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleRepoBrowserPage extends AuthenticatedPage {
    public static final String PATH = "_repoBrowser";

    @SpringBean
    private RepositoryService repoService;

    public SimpleRepoBrowserPage() {
        //Retrieve the repository path from the request
        WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
        WebRequest request = webRequestCycle.getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        RepoPath repoPath = (RepoPath) httpRequest.getAttribute(RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (repoPath == null) {
            //Happens on refresh after login redirection - return a 404
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }

        String repoKey = repoPath.getRepoKey();
        if (repoService.remoteRepoDescriptorByKey(repoKey) != null) {
            warn("Remote repositories are not directly browsable - browsing the remote repository cache.");
            // switch the repo path and key to the cache repo
            repoKey = repoKey + "-cache";
            repoPath = new RepoPath(repoKey, repoPath.getPath());
        }

        if (repoService.localOrCachedRepoDescriptorByKey(repoKey) != null) {
            add(new LocalRepoBrowserPanel("browseRepoPanel", repoPath));
        } else if (repoService.virtualRepoDescriptorByKey(repoKey) != null) {
            add(new VirtualRepoBrowserPanel("browseRepoPanel", repoPath));
        } else {
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected Class<? extends BasePage> getMenuPageClass() {
        return SimpleBrowserRootPage.class;
    }

    @Override
    public String getPageName() {
        return "Repository Browser";
    }
}