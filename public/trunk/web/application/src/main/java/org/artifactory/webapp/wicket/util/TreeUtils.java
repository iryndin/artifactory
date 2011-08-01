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

package org.artifactory.webapp.wicket.util;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.util.SerializablePair;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.webapp.wicket.page.browse.treebrowser.BrowseRepoPage;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.maven.MetadataPanel;
import org.artifactory.webapp.wicket.panel.tabbed.PersistentTabbedPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Noam Y. Tenne
 */
public abstract class TreeUtils {
    private static final Logger log = LoggerFactory.getLogger(TreeUtils.class);

    /**
     * Returns an HTTP link that points to the deployed item within the browser tree.
     *
     * @param repoKey      Repo key of item to point to
     * @param artifactPath Relative path of item to point to
     * @return HTTP link if permitted and valid, null if not
     */
    public static String getRepoPathUrl(LocalRepoDescriptor repo, String artifactPath) {
        if (!shouldProvideTreeLink(repo, artifactPath)) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder();
        if (NamingUtils.isChecksum(artifactPath)) {
            // if a checksum file is deployed, link to the target file
            artifactPath = MavenNaming.getChecksumTargetFile(artifactPath);
        }

        String metadataName = null;
        if (NamingUtils.isMetadata(artifactPath)) {
            SerializablePair<String, String> nameAndParent = NamingUtils.getMetadataNameAndParent(artifactPath);
            metadataName = nameAndParent.getFirst();
            artifactPath = nameAndParent.getSecond();
        }
        String repoPathId = new RepoPathImpl(repo.getKey(), artifactPath).getId();

        String encodedPathId;
        try {
            encodedPathId = URLEncoder.encode(repoPathId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode deployed artifact ID '{}': {}.", repoPathId, e.getMessage());
            return null;
        }

        //Using request parameters instead of wicket's page parameters. See RTFACT-2843
        urlBuilder.append(WicketUtils.absoluteMountPathForPage(BrowseRepoPage.class)).append("?").
                append(BrowseRepoPage.PATH_ID_PARAM).append("=").append(encodedPathId);
        if (StringUtils.isNotBlank(metadataName)) {
            urlBuilder.append("&").append(PersistentTabbedPanel.SELECT_TAB_PARAM).append("=Metadata");

            try {
                urlBuilder.append("&").append(MetadataPanel.SELECT_METADATA_PARAM).append("=").
                        append(URLEncoder.encode(metadataName, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                log.warn("Unable to link to tree item metadata '" + metadataName + "'.", e);
            }
        }
        return urlBuilder.toString();
    }

    /**
     * Indicates whether a link to the tree item of the deployed artifact should be provided. Links should be
     * provided if deploying a snapshot file to repository with different snapshot version policy.
     *
     * @param repo
     * @param artifactPath The artifact deploy path
     * @return True if should provide the link
     */
    private static boolean shouldProvideTreeLink(LocalRepoDescriptor repo, String artifactPath) {
        SnapshotVersionBehavior repoSnapshotBehavior = repo.getSnapshotVersionBehavior();

        boolean uniqueToNonUnique = MavenNaming.isUniqueSnapshot(artifactPath)
                && SnapshotVersionBehavior.NONUNIQUE.equals(repoSnapshotBehavior);

        boolean nonUniqueToNonUnique = MavenNaming.isNonUniqueSnapshot(artifactPath)
                && SnapshotVersionBehavior.UNIQUE.equals(repoSnapshotBehavior);

        return !uniqueToNonUnique && !nonUniqueToNonUnique;
    }
}
