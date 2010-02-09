/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.storage.GarbageCollectorInfo;
import org.artifactory.jcr.fs.JcrFile;
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
 * Created by IntelliJ IDEA. User: freds Date: Jun 2, 2008 Time: 4:56:26 PM
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

    @Async(transactional = false, delayUntilAfterCommit = true)
    void emptyTrash();

    @Async(transactional = false)
    void deleteFromTrash(String sessionFolderName);

    /**
     * Create an unstructure node under the root node of the jcr repository
     *
     * @param absPath the new or existing node path
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(String absPath);

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
     * @return Garbage collector info with collected garbage summary
     */
    GarbageCollectorInfo garbageCollect();

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

    void reCreateJcrRepository() throws Exception;

    void setStream(Node parent, String nodeName, InputStream value, String mimeType, String userId,
            boolean saveXmlHierarchy);

    InputStream getStream(String nodePath);
}
