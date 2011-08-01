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

package org.artifactory.jcr;

import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.tx.SessionResource;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.InputStream;
import java.util.List;

/**
 * @author Frederic Simon
 */
public interface JcrService extends ReloadableBean {

    @Transactional
    Repository getRepository();

    JcrSession getManagedSession();

    ObjectContentManager getOcm();

    /**
     * @param absPath The absolute path of the node (must start with '/')
     * @return True if a node with the absolute path exists.
     */
    @Lock(transactional = true)
    boolean itemNodeExists(String absPath);

    @Transactional
    int delete(String absPath);

    /**
     * Empty the trash. Asynchronous and non-transactional method.
     */
    @Async(transactional = false)
    void emptyTrash();


    /**
     * Empty the trash after the current TX is committed.
     */
    @Async(transactional = false, delayUntilAfterCommit = true)
    void emptyTrashAfterCommit();

    /**
     * Returns the node at the given path
     *
     * @param absPath Absolute JCR path to node
     * @return Node if found. Null if not
     */
    Node getNode(String absPath);

    /**
     * Create an unstructure node under the root node of the jcr repository
     *
     * @param absPath the new or existing node path
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(String absPath);

    /**
     * Returns the node at the given location
     *
     * @param parent  Parent node at given path
     * @param relPath Relative path to node from parent
     * @return Node if found. Null if not
     */
    Node getNode(Node parent, String relPath);

    /**
     * Create an unstructure node under the parent node paased
     *
     * @param parent  the parent node where this folder name should be
     * @param relPath the new or existing node path
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(Node parent, String relPath);

    <T extends SessionResource> T getSessionResource(Class<T> resourceClass);

    /**
     * Returns the number of artifacts currently being served
     *
     * @return ArtifactCount
     * @throws RepositoryException
     */
    @Lock(transactional = true)
    ArtifactCount getArtifactCount();

    /**
     * Returns the number of artifacts currently being served from the specified repository
     *
     * @param repoKey Repository to query
     * @return ArtifactCount
     * @throws RepositoryException
     */
    @Lock(transactional = true)
    ArtifactCount getArtifactCount(String repoKey);

    /**
     * Returns all the JcrFiles that holds a maven plugin pom (poms with packaging of maven-plugin)
     *
     * @param repo Repository to search in @return JcrFiles of plugin poms
     */
    @Lock(transactional = true)
    List<JcrFile> getPluginPomNodes(StoringRepo repo);

    /**
     * Runs the garbage collector
     *
     * @param fixConsistency
     * @return Garbage collector info with collected garbage summary
     */
    GarbageCollectorInfo garbageCollect(boolean fixConsistency);

    /**
     * Returns an unmanaged non-transactional session. You must call logout() on this session after using it, to
     * guarantee the underlying raw session is returned to the session pool.
     *
     * @return
     */
    JcrSession getUnmanagedSession();

    /**
     * Retrieve the raw XML information located in the JCR_CONTENT of the given node path
     *
     * @param nodePath The JCR node with a resource node containing XML data
     * @return The String representation of the whole XML data, null if the node does not exists
     * @throws RepositoryRuntimeException if reading XML fails
     */
    @Transactional
    String getString(String nodePath);

    /**
     * Add a JCR node a the given path (Parent node path should exists), with a resource node. The resource data stream
     * will be the content of the in string param. The xml will be imported with characters entity resolving, so it will
     * add a full JCR tree of the XML nodes. If the node exists, all its children will be deleted and the all content
     * will be renew.
     *
     * @param parentPath The existing JCR node to add the XML node to
     * @param nodeName   The name for the new or existing JCR node to add XML data to
     * @param mimeType   Content mime type
     * @param userId     The current user editing the xml  @throws RepositoryRuntimeException If failed to insert the
     *                   XML stream
     */
    @Transactional
    public void setString(String parentPath, String nodeName, String value, String mimeType, String userId);

    void setString(Node parent, String nodeName, String value, String mimeType, String userId, boolean saveXmlHierarchy
    );

    /**
     * Moves node from one path to another.
     *
     * @param fromAbsPath Absolute path of the node to move
     * @param toAbsPath   Absolute path of the target
     */
    void move(String fromAbsPath, String toAbsPath);

    void copy(String fromAbsPath, String toAbsPath);

    QueryResult executeQuery(JcrQuerySpec spec);

    QueryResult executeQuery(JcrQuerySpec spec, JcrSession session);

    boolean isFile(RepoPath repoPath);

    boolean isFolder(RepoPath repoPath);

    void reCreateJcrRepository() throws Exception;

    void setStream(Node parent, String nodeName, InputStream value, String mimeType, String userId,
            boolean saveXmlHierarchy);

    /**
     * Get an input stream of the binary stored on the specified node.
     * <p/>
     * NOTE: Caller is expected to close the returned stream.
     *
     * @param nodePath
     * @return
     */
    @Transactional
    InputStream getStream(String nodePath);

    @Async(delayUntilAfterCommit = true, transactional = true)
    void saveChecksums(JcrFsItem fsItem, String metadataName, Checksum[] checksums);

    /**
     * Extract in pure JCR the tree of folders and files. This method will recursively populate the whole tree.
     *
     * @param itemPath          The original folder repo path to start from
     * @param multiStatusHolder The status holder containing all error and message during tree building
     * @param fileFilter        File acceptance filter. Can be null
     * @return The fully populated tree node for the above folder or null on error
     */
    @Transactional
    JcrTreeNode getTreeNode(RepoPath itemPath, MultiStatusHolder multiStatusHolder,
            JcrTreeNodeFileFilter fileFilter);

    @Transactional
    InputStream getDataStreamBySha1Checksum(String sha1) throws DataStoreException;
}