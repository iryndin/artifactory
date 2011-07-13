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

package org.artifactory.webapp.wicket.page.browse.treebrowser;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.actionable.CannonicalEnabledActionableFolder;
import org.artifactory.webapp.actionable.RepoAwareActionableItem;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.browse.home.RememberPageBehavior;
import org.slf4j.Logger;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BrowseRepoPage extends AuthenticatedPage implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BrowseRepoPage.class);

    private String lastTabName;
    public static final String PATH_ID_PARAM = "pathId";

    public BrowseRepoPage() {
        add(new RememberPageBehavior());

        //Using request parameters instead of wicket's page parameters. See RTFACT-2843
        RepoPath repoPath = null;
        String pathId = getRequest().getParameter(PATH_ID_PARAM);
        if (StringUtils.isNotBlank(pathId)) {
            try {
                repoPath = new RepoPathImpl(pathId);
            } catch (Exception e) {
                error("Unable to find path " + pathId);
            }
        }
        displayBrowser(repoPath);
    }

    public BrowseRepoPage(RepoPath repoPath) {
        displayBrowser(repoPath);
    }

    @Override
    public String getPageName() {
        return "Repository Browser";
    }

    public String getLastTabName() {
        return lastTabName;
    }

    public void setLastTabName(String lastTabName) {
        this.lastTabName = lastTabName;
    }

    private void displayBrowser(RepoPath repoPath) {
        add(new BrowseRepoPanel("browseRepoPanel", new DefaultRepoTreeSelection(repoPath)));

        WebMarkupContainer scrollScript = new WebMarkupContainer("scrollScript");
        scrollScript.setVisible(repoPath != null);
        add(scrollScript);
    }

    /**
     * Builds an external url that points to resource inside the tree. Once used, the tree browser will open and select
     * the node represented by the repository item.
     *
     * @param repoItem The repository item
     * @return External URL linking directly to the item in the tree browser
     */
    public static String getRepoPathUrl(RepoAwareActionableItem repoItem) {
        String artifactPath;
        if (repoItem instanceof CannonicalEnabledActionableFolder) {
            artifactPath = ((CannonicalEnabledActionableFolder) repoItem).getCanonicalPath().getPath();
        } else {
            artifactPath = repoItem.getRepoPath().getPath();
        }

        StringBuilder urlBuilder = new StringBuilder();
        if (NamingUtils.isChecksum(artifactPath)) {
            // checksums are not displayed in the tree, link to the target file
            artifactPath = MavenNaming.getChecksumTargetFile(artifactPath);
        }
        String repoPathId = new RepoPathImpl(repoItem.getRepo().getKey(), artifactPath).getId();

        String encodedPathId;
        try {
            encodedPathId = URLEncoder.encode(repoPathId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode deployed artifact ID '{}': {}.", repoPathId, e.getMessage());
            return null;
        }

        //Using request parameters instead of wicket's page parameters. See RTFACT-2843
        urlBuilder.append(WicketUtils.absoluteMountPathForPage(BrowseRepoPage.class)).append("?").
                append(PATH_ID_PARAM).append("=").append(encodedPathId);
        return urlBuilder.toString();
    }
}