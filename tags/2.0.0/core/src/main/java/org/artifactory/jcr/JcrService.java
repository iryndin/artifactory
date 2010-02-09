package org.artifactory.jcr;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.tx.SessionResource;
import org.springframework.transaction.annotation.Transactional;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: freds Date: Jun 2, 2008 Time: 4:56:26 PM
 */
public interface JcrService extends ReloadableBean {

    @Transactional
    Repository getRepository();

    JcrSession getManagedSession();

    ObjectContentManager getOcm();

    @Lock(transactional = true, readOnly = true)
    boolean itemNodeExists(String absPath);

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
     * @param parent     the parent node where this folder name should be
     * @param folderName the new or existing folder name
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(Node parent, String folderName);

    void commitWorkingCopy(long sleepBetweenFiles, TaskCallback callback);

    /**
     * Extract the working copy file data and insert it in the JCR data store.
     *
     * @param workingCopyAbsPath
     * @return true if file found and work done, false otherwise
     */
    @Lock(transactional = true)
    boolean commitSingleFile(String workingCopyAbsPath);

    <T extends SessionResource> T getSessionResource(Class<T> resourceClass);

    /**
     * Returns the number of artifacts currently being served
     *
     * @return ArtifactCount
     * @throws RepositoryException
     */
    @Lock(transactional = true, readOnly = true)
    ArtifactCount getArtifactCount() throws RepositoryException;

    /**
     * Executes an Xpath query against the artifactory jcr repository.
     *
     * @param xpathQuery The jcr 170 xpath query to execute
     * @return The selected nodes
     * @throws RepositoryException On connection or syntax error
     */
    @Lock(transactional = true, readOnly = true)
    QueryResult executeXpathQuery(String xpathQuery) throws RepositoryException;

    /**
     * Executes an Sql query against the artifactory jcr repository.
     *
     * @param sqlQuery The jcr 170 sql query to execute
     * @return The selected nodes
     * @throws RepositoryException On connection or syntax error
     */
    @Lock(transactional = true, readOnly = true)
    QueryResult executeSqlQuery(String sqlQuery) throws RepositoryException;

    /**
     * Returns all the deployable units under a certain path.
     *
     * @param repoPath The repository path (might be repository root with no sub-path)
     * @return deployable units under a certain path
     * @throws RepositoryException On jcr connection error.
     */
    @Lock(transactional = true, readOnly = true)
    List<DeployableUnit> getDeployableUnitsUnder(RepoPath repoPath) throws RepositoryException;

    @Lock(transactional = true)
    void garbageCollect();

    /**
     * Returns an unmanaged non-transactional session. You must call logout() on this session after using it, to
     * guarantee the underlying raw session is returned to the session pool.
     *
     * @return
     */
    JcrSession getUnmanagedSession();
}
