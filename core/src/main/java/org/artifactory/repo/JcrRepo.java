package org.artifactory.repo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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
import org.artifactory.utils.IoUtil;

import javax.jcr.Node;
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
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger
            .getLogger(JcrRepo.class);

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlElement(defaultValue = "false", required = false)
    private boolean useSnapshotUniqueVersions;

    @XmlTransient
    private String tempFileRepoUrl;

    private transient JcrHelper jcr;

    @XmlTransient
    public String getUrl() {
        return tempFileRepoUrl;
    }

    public void init(CentralConfig cc) {
        String localRepositoriesDir = cc.getLocalRepositoriesDir();
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
     * Given a relative path, returns the file node in the repository
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFsItem getFsItem(final String relPath, Session session) throws RepositoryException {
        Node repoNode = getRepoJcrNode(session);
        Node node;
        if (relPath.length() > 0) {
            node = repoNode.getNode(relPath);
        } else {
            node = repoNode;
        }
        String typeName = node.getPrimaryNodeType().getName();
        if (typeName.equals(NT_ARTIFACTORY_FOLDER)) {
            return new JcrFolder(node);
        } else if (typeName.equals(NT_ARTIFACTORY_FILE)) {
            return new JcrFile(node);
        } else {
            throw new RuntimeException("Did not find a file system item at '" + relPath + "'.");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource retrieveInfo(final String relPath) {
        //Check if node exists
        boolean fileNodeExists = fileNodeExists(relPath);
        if (fileNodeExists) {
            //Create the cache repo node if it doesn't exist
            JcrFile file = (JcrFile) jcr.doInSession(
                    new JcrCallback<JcrFile>() {
                        public JcrFile doInJcr(JcrSessionWrapper session)
                                throws RepositoryException {
                            return (JcrFile) getFsItem(relPath, session);
                        }
                    });
            RepoResource localRes = new SimpleRepoResource(file);
            return localRes;
        }
        return new NotFoundRepoResource(relPath, this);
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public RepoResource getInfo(String path) {
        //Skip if in blackout or not accepting
        if (isBlackedOut() || !accept(path)) {
            return new NotFoundRepoResource(path, this);
        }
        //No need to access any cache
        RepoResource artifact = retrieveInfo(path);
        return artifact;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        final String absPath = res.getAbsPath();
        LOGGER.info("Transferring " + absPath + " directly to user from " + this);
        final InputStream is = (InputStream) jcr.doInSession(new JcrCallback<InputStream>() {
            public InputStream doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //If resource does not exist return null and throw an IOException
                if (!jcr.fileNodeExists(absPath)) {
                    return null;
                }
                String relPath = res.getRelPath();
                JcrFile file = (JcrFile) getFsItem(relPath, session);
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
                IoUtil.close(is);
            }
        };
        return handle;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public void undeploy(final String relPath) {
        //TODO: [by yl] Replace with real undeploy
        /**
         * Undeploy rules:
         * jar - remove pom, all jar classifiers and update metadata
         * pom - if packaing is jar remove pom and jar, else remove pom and update metadata
         * metadata - remove pom, jar and classifiers
         * version dir - update versions metadata in containing dir
         * plugin pom - update plugins in maven-metadata.xml in the directory above 
         */
        jcr.doInSession(new JcrCallback() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                JcrFsItem fsItem = getFsItem(relPath, session);
                if (fsItem.isDirectory()) {
                    fsItem.remove();
                } else {
                    //remove artifact
                    fsItem.remove();
                    //then remove all sibling artifacts by removing its parent folder
                    JcrFolder parent = fsItem.getParent();
                    parent.remove();
                }
                return null;
            }
        });
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Model getModel(ArtifactResource pa) {
        String pom = getPomContent(pa);
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
            throw new RuntimeException("Failed to get local repository node.", e);
        }
    }


    //TODO: [by yl] Clean up this method
    public void export(final File targetDir) {
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

    public void importFolder(final File dir) {
        jcr.doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node repoNode = getRepoJcrNode(session);
                try {
                    JcrHelper.importFolder(repoNode, repoNode, dir);
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
    }

    /**
     * Create the resource in the local repository
     * @param res
     * @param in
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void saveResource(final RepoResource res, final InputStream in) {
        Node resNode = (Node) jcr.doInSession(new JcrCallback<Node>() {
            @SuppressWarnings({"UnnecessaryLocalVariable"})
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                String repoKey = getKey();
                //Create the resource path
                Node parentNode = session.getRootNode();
                String resRelPath = res.getRelPath();
                int idx = resRelPath.lastIndexOf("/");
                String resDirAbsPath = "/" + repoKey + "/" + resRelPath.substring(0, idx);
                String resName = resRelPath.substring(idx + 1);
                Node resDirNode = jcr.createPath(parentNode, resDirAbsPath, repoKey);
                long lastModified = res.getLastModifiedTime();
                BufferedInputStream bis = new BufferedInputStream(in);
                try {
                    JcrHelper.importStream(resDirNode, resName, repoKey, lastModified, bis);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to retrieve resource '" + res + "'.", e);
                }
                Node resNode = resDirNode.getNode(resName);
                return resNode;
            }
        });
    }

    public String getPomContent(ArtifactResource res) {
        String relativeDirPath = res.getRelDirPath();
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
                ArtifactResource pomRes = new ArtifactResource(relativePath, this);
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

    protected JcrHelper getJcr() {
        return jcr;
    }

    protected boolean fileNodeExists(String relativePath) {
        return jcr.fileNodeExists("/" + getKey() + "/" + relativePath);
    }
}