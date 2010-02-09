/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket.page.browse.simplebrowser;

import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.webapp.servlet.RepoFilter;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.base.BasePage;
import org.artifactory.webapp.wicket.page.browse.simplebrowser.root.SimpleBrowserRootPage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleRepoBrowserPage extends AuthenticatedPage {
    public static final String PATH = "_repoBrowser";

    @SpringBean
    private RepositoryService repositoryService;

    public SimpleRepoBrowserPage() {
        //Retrieve the repository path from the request
        WebRequestCycle webRequestCycle = (WebRequestCycle) getRequestCycle();
        WebRequest request = webRequestCycle.getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        RepoPath repoPath = (RepoPath) httpRequest.getAttribute(
                RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (repoPath == null) {
            //Happens on refresh after login redirection - return a 404
            throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
        }

        String repoKey = repoPath.getRepoKey();
        LocalRepoDescriptor repo =
                repositoryService.localOrCachedRepoDescriptorByKey(repoKey);
        if (repo != null) {
            add(new LocalRepoBrowserPanel("browseRepoPanel", repoPath));
        } else {
            //Try to get a virtual repo
            VirtualRepoDescriptor virtualRepo =
                    repositoryService.virtualRepoDescriptorByKey(repoKey);
            if (virtualRepo == null) {
                //Return a 404
                throw new AbortWithWebErrorCodeException(HttpServletResponse.SC_NOT_FOUND);
            } else {
                add(new VirtualRepoBrowserPanel("browseRepoPanel", repoPath));
            }
        }
    }

    @Override
    protected Class<? extends BasePage> getMenuPageClass() {
        return SimpleBrowserRootPage.class;
    }

    @Override
    protected String getPageName() {
        return "Repository Browser";
    }
}