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
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.regex.NamedPattern;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.util.RepoLayoutUtils;

import java.util.Calendar;

/**
 * Base class for collecting versions items under a given root node.
 *
 * @author Shay Yaakov
 */
public abstract class VersionsRetriever {

    protected JcrService jcrService;

    protected VersionsRetriever() {
        this.jcrService = ContextHelper.get().beanForType(JcrService.class);
    }

    /**
     * For internal usage inside the collect versions recursion.
     */
    protected TreeMultimap<Calendar, VfsItem> versionsItems = TreeMultimap.create();

    public JcrTreeNode getTreeNode(StoringRepo repo, RepoLayout repoLayout, String itemPath,
            boolean pathHasVersionTokens) {
        RepoPath searchBasePath = getBaseRepoPathFromPartialItemPath(repo.getKey(), itemPath);
        String regEx = RepoLayoutUtils.generateRegExpFromPattern(repoLayout, itemPath, false, pathHasVersionTokens);
        NamedPattern pattern = NamedPattern.compile(regEx);
        JcrTreeNodeFileFilter fileFilter = getFileFilter(repo, pattern);

        return jcrService.getTreeNode(searchBasePath, new MultiStatusHolder(), fileFilter);
    }

    private RepoPath getBaseRepoPathFromPartialItemPath(String repoKey, String itemPath) {
        StringBuilder searchBasePathBuilder = new StringBuilder();
        String[] pathTokens = itemPath.split("/");
        for (String pathToken : pathTokens) {
            if (!pathToken.contains("[") && !pathToken.contains("(") && !pathToken.contains("{")) {
                searchBasePathBuilder.append(pathToken).append("/");
            } else {
                break;
            }
        }
        return InternalRepoPathFactory.create(repoKey, searchBasePathBuilder.toString());
    }

    /**
     * Collects versions items under the given node for the given repo
     *
     * @param repo The repo to search in
     * @param node The root node to collect under
     * @return
     */
    public abstract TreeMultimap<Calendar, VfsItem> collectVersionsItems(StoringRepo repo, JcrTreeNode node);

    public abstract JcrTreeNodeFileFilter getFileFilter(StoringRepo repo, NamedPattern pattern);
}
