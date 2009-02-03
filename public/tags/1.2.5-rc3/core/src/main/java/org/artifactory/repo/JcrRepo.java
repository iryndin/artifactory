/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.repo;

import org.acegisecurity.Authentication;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.ArtifactoryConstants;
import org.artifactory.config.CentralConfig;
import org.artifactory.engine.ResourceStreamHandle;
import org.artifactory.fs.FsItemMetadata;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FOLDER;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.RepoPath;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@XmlType(name = "LocalRepoType")
public class JcrRepo extends RepoBase implements LocalRepo {
    private static final Logger LOGGER = Logger.getLogger(JcrRepo.class);

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlElement(defaultValue = "nonunique", required = false)
    private SnapshotVersionBehavior snapshotVersionBehavior = SnapshotVersionBehavior.nonunique;

    @XmlTransient
    private boolean anonDownloadsAllowed;

    @XmlTransient
    private String tempFileRepoUrl;

    @XmlTransient
    private String repoPath;

    private int maxUniqueSnapshots;

    @XmlTransient
    private LocalRepoInterceptor localRepoInterceptor;

    private transient JcrHelper jcr;

    @XmlTransient
    public String getUrl() {
        return tempFileRepoUrl;
    }

    public void init(CentralConfig cc) {
        super.init(cc);
        anonDownloadsAllowed = cc.isAnonDownloadsAllowed();
        String localRepositoriesDir = CentralConfig.DATA_DIR;
        final String key = getKey();
        String basePath = localRepositoriesDir + "/tmp/" + key;
        tempFileRepoUrl = "file:///" + basePath;
        //Purge and recreate the (temp) repo dir
        File file = new File(basePath);
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                //Ignore
            }
        }
        boolean result = file.mkdirs();
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository directory: " + file.getAbsolutePath());
        }
        localRepoInterceptor = cc.getLocalRepoInterceptor();
        jcr = cc.getJcr();
        //Create the repo node if it doesn't exist
        jcr.doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node root = session.getRootNode();
                if (root.hasNode(repoPath)) {
                    return root.getNode(repoPath);
                } else {
                    Node node = jcr.createPath(root, repoPath, key);
                    session.save();
                    return node;
                }
            }
        });
    }

    @Override
    public void setKey(String key) {
        super.setKey(key);
        repoPath = REPO_ROOT + "/" + getKey();
    }

    public String getRepoPath() {
        return repoPath;
    }

    public SnapshotVersionBehavior getSnapshotVersionBehavior() {
        return snapshotVersionBehavior;
    }

    public int getMaxUniqueSnapshots() {
        return maxUniqueSnapshots;
    }

    public void setMaxUniqueSnapshots(int maxUniqueSnapshots) {
        this.maxUniqueSnapshots = maxUniqueSnapshots;
    }

    public boolean isAnonDownloadsAllowed() {
        return anonDownloadsAllowed;
    }

    /**
     * Given a relative path, returns the file node in the repository.
     *
     * @return null if the item's path does not exist
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFsItem getFsItem(final String path) {
        Node repoNode = jcr.doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                return getRepoJcrNode(session);
            }
        });
        Node node;
        if (path.length() > 0) {
            try {
                node = repoNode.getNode(path);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to get node at '" + path + "'.", e);
            }
        } else {
            node = repoNode;
        }
        String typeName;
        try {
            typeName = node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            throw new RuntimeException(
                    "Failed to get the primary type for node at '" + path + "'.", e);
        }
        if (typeName.equals(NT_ARTIFACTORY_FOLDER)) {
            return new JcrFolder(node);
        } else if (typeName.equals(NT_ARTIFACTORY_FILE)) {
            return new JcrFile(node);
        } else {
            throw new RuntimeException("Did not find a file system item at '" + path + "'.");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource retrieveInfo(final String path) {
        //Check if node exists
        boolean fileNodeExists = fileNodeExists(path);
        if (fileNodeExists) {
            //Create the cache repo node if it doesn't exist
            JcrFile file = (JcrFile) getFsItem(path);
            RepoResource localRes = new SimpleRepoResource(file);
            return localRes;
        }
        return new NotFoundRepoResource(path, this);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource getInfo(String path) {
        //Skip if in blackout or not accepting or cannot download
        if (isBlackedOut() || !accepts(path) || !allowsDownload(path)) {
            return new NotFoundRepoResource(path, this);
        }
        //No need to access any cache
        RepoResource artifact = retrieveInfo(path);
        return artifact;
    }

    public boolean allowsDownload(String path) {
        //Check download permissions
        if (anonDownloadsAllowed) {
            return true;
        }
        String parentPath;
        int parentPathEndIdx = path.lastIndexOf('/');
        if (parentPathEndIdx > 1) {
            parentPath = path.substring(0, parentPathEndIdx);
        } else {
            LOGGER.warn("Cannot determine parent path from request path '" + path + "'.");
            parentPath = path;
        }
        ArtifactoryContext context = ContextHelper.get();
        SecurityHelper security = context.getSecurity();
        RepoPath repoPath = new RepoPath(getKey(), parentPath);
        boolean canRead = security.canRead(repoPath);
        if (!canRead) {
            Authentication authentication = SecurityHelper.getAuthentication();
            LOGGER.warn("Download request for repo:path '" + repoPath +
                    "' is forbidden for user '" + authentication.getName() + "'.");
            AccessLogger.downloadDenied(repoPath);
        }
        return canRead;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        String relPath = res.getPath();
        String absPath = "/" + repoPath + "/" + relPath;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transferring " + absPath + " directly to user from " + this);
        }
        //If resource does not exist return null and throw an IOException
        if (!fileNodeExists(relPath)) {
            return null;
        }
        JcrFile file = (JcrFile) getFsItem(relPath);
        final InputStream is = file.getStreamForDownload();
        if (is == null) {
            throw new IOException(
                    "Could not get resource stream. Path not found: " + absPath + ".");
        }
        ResourceStreamHandle handle = new ResourceStreamHandle() {
            public InputStream getInputStream() {
                return is;
            }

            public void close() {
                IOUtils.closeQuietly(is);
            }
        };
        return handle;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public void undeploy(final String path) {
        //TODO: [by yl] Replace with real undeploy
        /**
         * Undeploy rules:
         * jar - remove pom, all jar classifiers and update metadata
         * pom - if packaing is jar remove pom and jar, else remove pom and update metadata
         * metadata - remove pom, jar and classifiers
         * version dir - update versions metadata in containing dir
         * plugin pom - update plugins in maven-metadata.xml in the directory above
         */
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                delete(path, true);
                return null;
            }
        });
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Model getModel(ArtifactResource res) {
        String pom = getPomContent(res);
        if (pom == null) {
            return null;
        }
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new StringReader(pom));
            return model;
        } catch (Exception e) {
            LOGGER.warn("Failed to read pom from '" + pom + "'.", e);
            return null;
        }
    }

    public Node getRepoJcrNode(Session session) {
        try {
            Node root = session.getRootNode();
            return root.getNode(repoPath);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get local repository node: " + getKey(), e);
        }
    }

    public void exportToDir(final File targetDir) {
        exportToDir(targetDir, false);
    }

    public void exportToDir(final File targetDir, final boolean includeMetadata) {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create export directory '" + targetDir + "'.");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting repository '" + getKey() + "' to '" + targetDir + "'.");
        }
        jcr.doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node repoNode = getRepoJcrNode(session);
                JcrFolder folder = new JcrFolder(repoNode);
                folder.export(targetDir, includeMetadata);
                return repoNode;
            }
        });
    }

    public void importFromDir(
            final File sourceDir, boolean singleTransaction, boolean ignoreMissingDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) {
            String message = "Cannot import null, non existent folder or non directory file '" +
                    sourceDir + "'.";
            if (ignoreMissingDir) {
                LOGGER.warn(message);
            } else {
                throw new RuntimeException(message);
            }
        }
        final String basePath = sourceDir.getPath();
        if (singleTransaction && !ArtifactoryConstants.forceAtomicTransacions) {
            jcr.doInSession(new JcrCallback<Node>() {
                public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                    Node repoNode = getRepoJcrNode(session);
                    try {
                        importFolder(repoNode, sourceDir, basePath);
                    } catch (IOException e) {
                        throw new RepositoryException(
                                "Failed to import folder '" + sourceDir.getPath() + "'.", e);
                    }
                    return repoNode;
                }
            });
        } else {
            String parentPath = jcr.doInSession(new JcrCallback<String>() {
                public String doInJcr(JcrSessionWrapper session) throws RepositoryException {
                    Node repoNode = getRepoJcrNode(session);
                    return repoNode.getPath();
                }
            });
            importFolderFileByFile(parentPath, sourceDir, basePath);
        }
    }

    /**
     * Create the resource in the local repository
     *
     * @param res
     * @param in
     */
    public void saveResource(final RepoResource res, final InputStream in,
            final boolean createChecksum) throws IOException {
        try {
            Node resNode = jcr.doInSession(new JcrCallback<Node>() {
                @SuppressWarnings({"UnnecessaryLocalVariable"})
                public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                    //Create the resource path
                    Node rootNode = session.getRootNode();
                    String resPath = res.getPath();
                    int idx = resPath.lastIndexOf("/");
                    String resDirAbsPath =
                            "/" + repoPath + (idx > 0 ? "/" + resPath.substring(0, idx) : "");
                    String resName = idx > 0 ? resPath.substring(idx + 1) : resPath;
                    String repoKey = getKey();
                    Node resDirNode = jcr.createPath(rootNode, resDirAbsPath, repoKey);
                    long lastModified = res.getLastModifiedTime();
                    BufferedInputStream bis = new BufferedInputStream(in);
                    try {
                        JcrHelper.importStream(
                                resDirNode, resName, repoKey, lastModified, bis, createChecksum);
                        AccessLogger.deployed(res.getRepoPath());
                    } catch (Exception e) {
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            if (cause instanceof IOException) {
                                throw new RepositoryException(cause);
                            }
                        }
                        throw new RepositoryException(
                                "Failed to import resource '" + res + "'", e);
                    }
                    Node resNode = resDirNode.getNode(resName);
                    return resNode;
                }
            });
            //If the resource has no size specified, update the size
            //(this can happen if we established the resource based on a HEAD request that failed to
            //return the content-length).
            JcrFile jcrFile = new JcrFile(resNode);
            if (!res.hasSize()) {
                long size = jcrFile.size();
                ((SimpleRepoResource) res).setSize(size);
            }
            localRepoInterceptor.afterResourceSave(res, this);
        } catch (Exception e) {
            //Unwrap any IOException and throw it
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                cause = cause.getCause();
            }
            throw new RuntimeException("Failed to save resource '" + res + "'.", e);
        }
    }

    public void createPath(String path) {
        Node repoNode = jcr.doInSession(new JcrCallback<javax.jcr.Node>() {
            public javax.jcr.Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                return getRepoJcrNode(session);
            }
        });
        //Create the path
        String repoKey = getKey();
        try {
            jcr.createPath(repoNode, path, repoKey);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to create '" + new RepoPath(repoKey, path) + "'.");
        }
    }

    public String getPomContent(ArtifactResource res) {
        String relativeDirPath = res.getDirPath();
        String relativePath = null;
        String name = res.getName();
        if (name.endsWith(".pom")) {
            relativePath = relativeDirPath + "/" + name;
        } else if (name.endsWith(".jar")) {
            relativePath = relativeDirPath + "/" + name.substring(0, name.length() - 4) + ".pom";
        }
        if (relativePath != null && fileNodeExists(relativePath)) {
            ResourceStreamHandle handle = null;
            try {
                ArtifactResource pomRes = new ArtifactResource(this, relativePath);
                handle = getResourceStreamHandle(pomRes);
                InputStream is = handle.getInputStream();
                return IOUtils.toString(is, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read pom from '" + relativePath + "'.", e);
            } finally {
                if (handle != null) {
                    handle.close();
                }
            }
        }
        return null;
    }

    public boolean fileNodeExists(String relPath) {
        return itemExists(relPath) && !getFsItem(relPath).isFolder();
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean itemExists(String relPath) {
        if (relPath.length() > 0) {
            return jcr.itemNodeExists("/" + repoPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    public boolean isCache() {
        return false;
    }

    public void exportTo(String basePath, StatusHolder status) {
        String key = getKey();
        status.setStatus("Exporting repository " + key + "...");
        File targetDir = new File(basePath, key);
        exportToDir(targetDir, true);
    }

    public void importFrom(String basePath, StatusHolder status) {
        String key = getKey();
        status.setStatus("Importing repository " + key + "...");
        File sourceDir = new File(basePath, key);
        importFromDir(sourceDir, false, false);
    }

    public void delete() {
        delete("", false);
    }

    /**
     * Delete a relative path
     *
     * @param path        relative path to delete
     * @param excludeRoot never remove the repository root folder itself
     */
    protected void delete(final String path, final boolean excludeRoot) {
        JcrFsItem fsItem = getFsItem(path);
        if (fsItem.isFolder()) {
            JcrFolder folder = (JcrFolder) fsItem;
            if (path.length() == 0 && excludeRoot) {
                //Do not delete the repo folder itself
                folder.deleteChildren(false);
            } else {
                folder.delete(false);
            }
        } else {
            fsItem.delete();
        }
    }

    protected JcrHelper getJcr() {
        return jcr;
    }

    private void importFolder(Node parentNode, File directory, String basePath)
            throws RepositoryException, IOException {
        File[] dirEntries = directory.listFiles();
        for (File dirEntry : dirEntries) {
            String fileName = dirEntry.getName();
            String key = getKey();
            if (dirEntry.isDirectory()) {
                Node childNode = JcrHelper.getOrCreateFolderNode(parentNode, fileName, key);
                LOGGER.info("Importing folder '" + fileName + "' into '" + key + "'...");
                //Update the metadata
                JcrFolder jcrFolder = new JcrFolder(childNode);
                jcrFolder.importFrom(basePath, null);
                //Import recursively
                importFolder(childNode, dirEntry, basePath);
            } else if (!fileName.endsWith(FsItemMetadata.SUFFIX)) {
                //Do not import artifactory metadata descriptor files
                LOGGER.info("Importing file '" + fileName + "' into '" + key + "'...");
                importFile(parentNode, dirEntry, basePath);
            }
        }
    }

    private void importFolderFileByFile(
            final String parentPath, File folder, final String basePath) {
        final String key = getKey();
        File[] dirEntries = folder.listFiles();
        for (final File dirEntry : dirEntries) {
            final String fileName = dirEntry.getName();
            if (dirEntry.isDirectory()) {
                String childNodePath = jcr.doInSession(new JcrCallback<String>() {
                    public String doInJcr(JcrSessionWrapper session) throws RepositoryException {
                        Node parentNode = (Node) session.getItem(parentPath);
                        Node childNode = JcrHelper.getOrCreateFolderNode(parentNode, fileName, key);
                        //Update the metadata
                        JcrFolder jcrFolder = new JcrFolder(childNode);
                        jcrFolder.importFrom(basePath, null);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "Importing directory '" + fileName + "' into '" + key + "'...");
                        }
                        return childNode.getPath();
                    }
                });
                importFolderFileByFile(childNodePath, dirEntry, basePath);
            } else if (!fileName.endsWith(FsItemMetadata.SUFFIX)) {
                //Do not import artifactory metadata descriptor files
                jcr.doInSession(new JcrCallback<String>() {
                    public String doInJcr(JcrSessionWrapper session) throws RepositoryException {
                        Node parentNode = (Node) session.getItem(parentPath);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Importing file '" + fileName + "' into '" + key + "'...");
                        }
                        try {
                            importFile(parentNode, dirEntry, basePath);
                            session.save();
                        } catch (IOException e) {
                            throw new RuntimeException(
                                    "Failed to import file '" + dirEntry + "'.", e);
                        }
                        return null;
                    }
                });

            }
        }
    }

    private void importFile(Node parentNode, File dirEntry, String basePath)
            throws RepositoryException, IOException {
        String key = getKey();
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(dirEntry));
        Node childNode = JcrHelper.importStream(
                parentNode, dirEntry.getName(), key, dirEntry.lastModified(), bis, false);
        //Update the metadata
        JcrFile jcrFile = new JcrFile(childNode);
        jcrFile.importFrom(basePath, null);
    }
}