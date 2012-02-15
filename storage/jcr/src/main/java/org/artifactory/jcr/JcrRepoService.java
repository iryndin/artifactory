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

package org.artifactory.jcr;

import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.Lock;
import org.artifactory.sapi.fs.VfsItem;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This interface should be used by JcrRepoBase only. It contains all the methods Tx or not, that are needed to manage
 * JcrFsItem
 *
 * @author freds
 * @date Oct 24, 2008
 */
public interface JcrRepoService {
    void saveXmlHierarchy(Node xmlNode, InputStream in) throws RepositoryException, IOException;

    @Lock(transactional = true)
    JcrFile importFile(JcrFolder parentFolder, File file, ImportSettings settings) throws RepoRejectException;

    @Lock(transactional = true)
    RepoPath importFolder(JcrFsItemFactory repo, RepoPath jcrFolder, ImportSettings settings);

    /**
     * Returns jcr file or folder given a repo path and the storing repository to look in. Added @Transactional
     * annotation because full system import fails (RTFACT-725).
     *
     * @param repoPath The item repository path
     * @param repo     Repository to search for the item
     * @return Jcr item or null if not found
     */
    @Lock(transactional = true)
    JcrFsItem getFsItem(RepoPath repoPath, JcrFsItemFactory repo);

    JcrFsItem getFsItem(Node node, JcrFsItemFactory repo);

    /**
     * Create the children list of file system items for the given folder. If withWriteLock is true, all children will
     * be create with write lock acquired, If false the read lock will be acquired.
     *
     * @param folder        the JcrFolder parent of all resulting children
     * @param withWriteLock true to acquire write lock, false for read lock
     * @return The list of children
     */
    @Lock(transactional = true)
    @Nonnull
    List<JcrFsItem> getChildren(JcrFolder folder, boolean withWriteLock);

    boolean delete(JcrFsItem fsItem);

    /**
     * Copied from JcrService. TODO: Check how to remove the duplication
     *
     * @return
     */
    JcrSession getManagedSession();

    /**
     * Create an unstructure node under the parent node paased Copied from JcrService. TODO: Check how to remove the
     * duplication
     *
     * @param parent  the parent node where this folder name should be
     * @param relPath the new or existing folder name
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(Node parent, String relPath);

    Node getOrCreateNode(Node parent, String relPath, String type, String... mixins);

    @Lock(transactional = true)
    void exportFile(JcrFile jcrFile, ExportSettings settings);

    @Lock(transactional = true)
    List<String> getChildrenNames(String absPath);

    void trash(List<VfsItem> items);

    String getNodeTypeName(RepoPath repoPath);

    @Lock(transactional = true)
    void writeMetadataEntries(JcrFsItem fsItem, MutableStatusHolder status, File folder, boolean incremental);
}
