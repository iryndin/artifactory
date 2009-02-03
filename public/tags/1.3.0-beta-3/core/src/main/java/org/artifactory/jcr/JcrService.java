package org.artifactory.jcr;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.schedule.ArtifactoryTimerTask;
import org.artifactory.spring.PostInitializingBean;

import javax.jcr.Node;
import javax.jcr.Repository;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: freds Date: Jun 2, 2008 Time: 4:56:26 PM
 */
public interface JcrService extends PostInitializingBean {

    Repository getRepository();

    JcrSession getManagedSession();

    ObjectContentManager getOcm();

    boolean itemNodeExists(String absPath);

    /**
     * Create an unstructure node under the root node of the jcr repository
     *
     * @param folderName the new or existing folder name
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(String folderName);

    /**
     * Create an unstructure node under the parent node paased
     *
     * @param parent     the parent node where this folder name should be
     * @param folderName the new or existing folder name
     * @return the new or current node for the folder
     */
    Node getOrCreateUnstructuredNode(Node parent, String folderName);

    void destroy();

    void importFile(
            JcrFolder parentFolder, File dirEntry, ImportSettings settings, StatusHolder status);

    void importFileViaWorkingCopy(
            JcrFolder parentFolder, File file, ImportSettings settings, StatusHolder status);

    JcrFile importStream(
            JcrFolder parentFolder, String name, long lastModified, InputStream in);

    boolean delete(String absPath);

    List<String> getChildrenNames(String absPath);

    JcrFsItem getFsItem(String repoRootPath, String relPath);

    List<JcrFsItem> getChildren(JcrFolder folder);

    Node getOrCreateNode(Node parent, String name, String type);

    void commitWorkingCopy(long sleepBetweenFiles, ArtifactoryTimerTask task);

    boolean commitSingleFile(String workingCopyAbsPath);

    JcrFile getJcrFile(LocalRepo repo, String relPath) throws FileExpectedException;

    FileInfo getLockedFileInfo(LocalRepo repo, String path);

    FileInfo getFileInfo(LocalRepo repo, String path);

    void importFolders(LinkedList<JcrFolder> foldersToScan, ImportSettings settings,
            StatusHolder status);

    void importFolder(JcrFolder jcrFolder, ImportSettings settings, StatusHolder status);
}
