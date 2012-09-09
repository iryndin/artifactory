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

package org.artifactory.repo.snapshot;

import com.google.common.collect.TreeMultimap;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.regex.NamedPattern;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.VfsItem;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Set;

/**
 * Collects release versions items under a given root node.
 *
 * @author Shay Yaakov
 */
public class ReleaseVersionsRetriever extends VersionsRetriever {
    private static final Logger log = LoggerFactory.getLogger(ReleaseVersionsRetriever.class);

    @Override
    public TreeMultimap<Calendar, VfsItem> collectVersionsItems(StoringRepo repo, JcrTreeNode node) {
        internalCollectVersionsItems(repo, node);
        return versionsItems;
    }

    private void internalCollectVersionsItems(StoringRepo repo, JcrTreeNode node) {
        if (node.isFolder()) {
            Set<JcrTreeNode> children = node.getChildren();
            for (JcrTreeNode child : children) {
                internalCollectVersionsItems(repo, child);
            }
        } else {
            RepoPath itemRepoPath = node.getRepoPath();
            VfsItem fsItem;
            try {
                fsItem = repo.getJcrFsItem(itemRepoPath);
            } catch (RepositoryRuntimeException e) {
                log.warn("Could not get file at '{}' ({}). Skipping.", itemRepoPath, e.getMessage());
                if (log.isDebugEnabled()) {
                    log.error("Error while getting file.", e);
                }
                return;
            }
            versionsItems.put(node.getCreated(), fsItem);
        }
    }

    @Override
    public JcrTreeNodeFileFilter getFileFilter(StoringRepo repo, NamedPattern pattern) {
        return new ReleaseFileFilter(repo, pattern);
    }

    private static class ReleaseFileFilter implements JcrTreeNodeFileFilter {
        private final StoringRepo repo;

        private final NamedPattern pattern;

        public ReleaseFileFilter(StoringRepo repo, NamedPattern pattern) {
            this.repo = repo;
            this.pattern = pattern;
        }

        @Override
        public boolean acceptsFile(RepoPath repoPath) {
            String path = repoPath.getPath();
            if (!pattern.matcher(path).matches()) {
                return false;
            }
            ModuleInfo itemModuleInfo = repo.getItemModuleInfo(path);

            //Make sure this file's module info is valid and is actually a release version
            return itemModuleInfo.isValid() && !itemModuleInfo.isIntegration();
        }
    }
}
