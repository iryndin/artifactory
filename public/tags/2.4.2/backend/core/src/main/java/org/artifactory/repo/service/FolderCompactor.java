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

package org.artifactory.repo.service;

import com.google.common.collect.Lists;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Builds a chain of compactable folders beyond a given one
 *
 * @author Noam Tenne
 */
@Component
public class FolderCompactor {

    private static final Logger log = LoggerFactory.getLogger(FolderCompactor.class);

    @Autowired
    private InternalRepositoryService repositoryService;

    /**
     * Returns A list containing the given folder, and any folder beyond it that can be compacted with it.
     *
     * @param folder Folder to check beyond.
     * @return List of the of folders that can be compacted
     */
    public List<JcrFolder> getFolderWithCompactedChildren(JcrFolder folder) {
        List<JcrFolder> result = Lists.newArrayList();
        JcrFolder current = folder;
        result.add(current);
        while (true) {
            JcrFolder next = getNextCompactedFolder(current);
            if (next == null) {
                return result;
            }

            current = next;
            result.add(current);
        }
    }

    /**
     * Returns the next folder that can be compacted after the given one.
     *
     * @param folder Folder to check beyond.
     * @return JcrFolder if another folder to compact is found. Null if no folder is found.
     */
    private JcrFolder getNextCompactedFolder(JcrFolder folder) {
        if (folder == null) {
            return null;
        }

        int namesListSize = 0;
        boolean containsMavenMetadata = false;
        try {
            Set<MetadataDefinition<?, ?>> metadataDefs = VfsItemFactory.getExistingMetadata(folder, false);
            namesListSize = metadataDefs.size();
            containsMavenMetadata = folder.hasMetadata(MavenNaming.MAVEN_METADATA_NAME);
        } catch (RepositoryRuntimeException re) {
            String folderPath = folder.getAbsolutePath();
            log.error("Could not determine existing metadata existence for '{}': {}", folderPath, re.getMessage());
            log.debug("Could not determine existing metadata existence for '" + folderPath + "'.", re);
            return null;
        }

        /**
         * A node should not be compacted if it has any metadata attached to it, except for maven metadata.
         * If the node has only maven metadata attached, we ignore it and let it be compacted
         * So we don't compact if:
         * - If there is only one metadata attached and it is not maven
         * - OR -
         * - If there are multiple types of metadata attached
         */
        if (((namesListSize == 1) && !containsMavenMetadata) || (namesListSize > 1)) {
            return null;
        }

        //Get only names to avoid (read) locking
        RepoPath parentRepoPath = folder.getRepoPath();
        List<String> children = repositoryService.getChildrenNames(parentRepoPath);

        //If we have no children or more than 1 children stop here
        if (children.size() != 1) {
            return null;
        }

        //If we have 1 child stop at it if it's a folder
        LocalRepo repository = repositoryService.getLocalRepository(parentRepoPath);
        RepoPath childRepoPath = InternalRepoPathFactory.childRepoPath(parentRepoPath, children.get(0));
        JcrFsItem next = repository.getJcrFsItem(childRepoPath);

        if (next.isDirectory()) {
            return (JcrFolder) next;
        }

        return null;
    }
}
