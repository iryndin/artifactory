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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.config.CentralConfig;
import org.artifactory.engine.ResourceStreamHandle;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FOLDER;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@XmlType(name = "LocalRepoType")
public class JcrRepo extends RepoBase implements LocalRepo {
    private static final Logger LOGGER = Logger.getLogger(JcrRepo.class);

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlElement(defaultValue = "false")
    private boolean useSnapshotUniqueVersions;

    @XmlTransient
    private String tempFileRepoUrl;

    private transient JcrHelper jcr;

    @XmlTransient
    public String getUrl() {
        return tempFileRepoUrl;
    }

    public void init(CentralConfig cc) {
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
        jcr = cc.getJcr();
        //Create the cache repo node if it doesn't exist
        jcr.doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node root = session.getRootNode();
                if (root.hasNode(key)) {
                    return root.getNode(key);
                } else {
                    Node node = JcrHelper.createFolder(root, key, key);
                    session.save();
                    return node;
                }
            }
        });
    }

    public boolean isUseSnapshotUniqueVersions() {
        return useSnapshotUniqueVersions;
    }

    /**
     * Given a relative path, returns the file node in the repository.
     *
     * @return null if the item's path does not exist
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFsItem getFsItem(final String path, Session session) {
        Node repoNode = getRepoJcrNode(session);
        Node node;
        if (path.length() > 0) {
            try {
                node = repoNode.getNode(path);
            } catch (PathNotFoundException e) {
                return null;
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
            JcrFile file = jcr.doInSession(
                    new JcrCallback<JcrFile>() {
                        public JcrFile doInJcr(JcrSessionWrapper session)
                                throws RepositoryException {
                            return (JcrFile) getFsItem(path, session);
                        }
                    });
            RepoResource localRes = new SimpleRepoResource(file);
            return localRes;
        }
        return new NotFoundRepoResource(path, this);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    public boolean isCache() {
        return false;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource getInfo(String path) {
        //Skip if in blackout or not accepting
        if (isBlackedOut() || !accepts(path)) {
            return new NotFoundRepoResource(path, this);
        }
        //No need to access any cache
        RepoResource artifact = retrieveInfo(path);
        return artifact;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        final String absPath = res.getAbsPath();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transferring " + absPath + " directly to user from " + this);
        }
        final InputStream is = jcr.doInSession(new JcrCallback<InputStream>() {
            public InputStream doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //If resource does not exist return null and throw an IOException
                if (!jcr.fileNodeExists(absPath)) {
                    return null;
                }
                String path = res.getPath();
                JcrFile file = (JcrFile) getFsItem(path, session);
                return file.getStream();
            }
        });
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
                JcrFsItem fsItem = getFsItem(path, session);
                if (fsItem.isDirectory()) {
                    fsItem.remove();
                } else {
                    //remove artifact
                    fsItem.remove();
                }
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
            String key = getKey();
            return root.getNode(key);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get local repository node: " + getKey(), e);
        }
    }

    public void exportToDir(final File targetDir) {
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
                folder.export(targetDir);
                if (LOGGER.isDebugEnabled()) {
                    JcrHelper.dump(repoNode);
                }
                return repoNode;
            }
        });
    }

    public void importFromDir(final File dir, boolean singleStep) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException(
                    "Cannot import null, non existent folder or non directory file '" + dir + "'.");
        }
        if (singleStep) {
            jcr.doInSession(new JcrCallback<Node>() {
                public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                    Node repoNode = getRepoJcrNode(session);
                    String key = getKey();
                    try {
                        JcrHelper.importFolder(key, repoNode, dir);
                    } catch (IOException e) {
                        throw new RepositoryException(
                                "Failed to import folder '" + dir.getPath() + "'.", e);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        JcrHelper.dump(repoNode);
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
            importFolderFileByFile(parentPath, dir);
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
                    String repoKey = getKey();
                    //Create the resource path
                    Node parentNode = session.getRootNode();
                    String resPath = res.getPath();
                    int idx = resPath.lastIndexOf("/");
                    String resDirAbsPath = "/" + repoKey + "/" + resPath.substring(0, idx);
                    String resName = resPath.substring(idx + 1);
                    Node resDirNode = jcr.createPath(parentNode, resDirAbsPath, repoKey);
                    long lastModified = res.getLastModifiedTime();
                    BufferedInputStream bis = new BufferedInputStream(in);
                    try {
                        JcrHelper.importStream(resDirNode, resName, repoKey, lastModified, bis,
                                createChecksum);
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
            if (!res.hasSize()) {
                JcrFile jcrFile = new JcrFile(resNode);
                long size = jcrFile.size();
                ((SimpleRepoResource) res).setSize(size);
            }
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

    public boolean fileNodeExists(String path) {
        return jcr.fileNodeExists("/" + getKey() + "/" + path);
    }

    protected JcrHelper getJcr() {
        return jcr;
    }

    private void importFolderFileByFile(final String parentPath, File folder) {
        final String key = getKey();
        File[] dirEntries = folder.listFiles();
        for (final File dirEntry : dirEntries) {
            final String fileName = dirEntry.getName();
            if (dirEntry.isDirectory()) {
                String childNodePath = jcr.doInSession(new JcrCallback<String>() {
                    public String doInJcr(JcrSessionWrapper session) throws RepositoryException {
                        Node parentNode = (Node) session.getItem(parentPath);
                        Node childNode = JcrHelper.getOrCreateFolderNode(parentNode, fileName, key);
                        LOGGER.info("Importing directory '" + fileName + "' into '" + key + "'...");
                        return childNode.getPath();
                    }
                });
                importFolderFileByFile(childNodePath, dirEntry);
            } else {
                jcr.doInSession(new JcrCallback<String>() {
                    public String doInJcr(JcrSessionWrapper session) throws RepositoryException {
                        Node parentNode = (Node) session.getItem(parentPath);
                        LOGGER.info("Importing file '" + fileName + "' into '" + key + "'...");
                        try {
                            JcrHelper.importFile(key, parentNode, dirEntry);
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
}