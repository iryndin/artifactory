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

package org.artifactory.repo.service;

import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a chain of compactable folders beyond a given one
 *
 * @author Noam Tenne
 */
@Component
public class FolderCompactor {

    @Autowired
    private AuthorizationService authorizationService;

    /**
     * Returns A list cotaining the given folder, and any folder beyond it that can be compacted with it.
     *
     * @param folder Folder to check beyond.
     * @return List of the of folders that can be compacted
     */
    public List<JcrFolder> getFolderWithCompactedChildren(JcrFolder folder) {
        List<JcrFolder> result = new ArrayList<JcrFolder>();
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

        List<String> metadataNames = folder.getXmlMetadataNames();
        int namesListSize = metadataNames.size();
        boolean containsMavenMetadata = metadataNames.contains(MavenNaming.MAVEN_METADATA_NAME);

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

        List<JcrFsItem> children = folder.getItems();
        //Make copy of children list so we can remove while iterating
        List<JcrFsItem> childrenCopy = new ArrayList<JcrFsItem>(children);

        //Iterate on the copied list
        for (JcrFsItem child : childrenCopy) {
            RepoPath childRepoPath = child.getRepoPath();

            //Check if the user has read permissions on the current child
            boolean canReadChild = authorizationService.canImplicitlyReadParentPath(childRepoPath);

            /**
             * Remove the child from the list. There is no need to consider it while checking children to compact, since
             * The user has no read permissions on it
             */
            if (!canReadChild) {
                children.remove(child);
            }
        }

        //If we have no children or more than 1 children stop here
        if (children.size() != 1) {
            return null;
        }

        //If we have 1 child stop at it if it's a folder
        JcrFsItem next = children.get(0);
        if (next.isDirectory()) {
            return (JcrFolder) next;
        }

        return null;
    }
}
