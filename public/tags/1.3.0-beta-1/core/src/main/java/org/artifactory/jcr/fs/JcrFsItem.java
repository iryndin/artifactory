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
package org.artifactory.jcr.fs;

import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import org.apache.jackrabbit.value.StringValue;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.fs.FsItemMetadata;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.jcr.NodeLock;
import org.artifactory.repo.RepoPath;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ContextHelper;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class JcrFsItem extends File implements Comparable<File>, ExportableConfig {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFsItem.class);

    public static final String PROP_ARTIFACTORY_NAME = "artifactory:name";
    public static final String PROP_ARTIFACTORY_REPO_KEY = "artifactory:repoKey";
    public static final String PROP_ARTIFACTORY_MODIFIED_BY = "artifactory:modifiedBy";

    private String absPath;
    private String relPath;
    private String name;

    public JcrFsItem(String absPath) {
        super(removeTrailingSlashes(absPath));
        this.absPath = super.getPath().replace('\\', '/');
        setRelativePathFromRepositoryAbsPath(this.absPath, false);
    }

    public JcrFsItem(Node parentNode, String name) {
        this(parentNode, null, name);
    }

    public JcrFsItem(String parent, String child) {
        this(parent + "/" + child);
    }

    public JcrFsItem(File parent, String child) {
        this(parent.getAbsolutePath() + "/" + child);
    }

    public JcrFsItem(Node node) {
        this(getAbsPath(node));
    }

    /**
     * Used for creating a temp repo resource file that will eventually be moved under a repo
     *
     * @param parentNode
     * @param targetAbsPath
     */
    protected JcrFsItem(Node parentNode, String targetAbsPath, String name) {
        super(getAbsPath(parentNode) + "/" + name);
        absPath = super.getPath().replace('\\', '/');
        if (targetAbsPath == null) {
            targetAbsPath = absPath;
        }
        try {
            //Create the file node
            if (isDirectory()) {
                parentNode.addNode(name, JcrFolder.NT_ARTIFACTORY_FOLDER);
            } else {
                parentNode.addNode(name, JcrFile.NT_ARTIFACTORY_FILE);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(
                    "Failed to create temp node '" + targetAbsPath + "' at '" + absPath + "'.", e);
        }
        setRelativePathFromRepositoryAbsPath(targetAbsPath, true);
        setArtifactoryName(name);
        String userId = ArtifactorySecurityManager.getUsername();
        setModifiedBy(userId);
    }

    public void save() {
        try {
            getNode().save();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to save file'" + absPath + "'.", e);
        }
    }

    public String getName() {
        return name;
    }

    /**
     * An alternative name not tied to jcr node naming constraints
     *
     * @return
     */
    public String getArtifactoryName() {
        try {
            return getPropValue(PROP_ARTIFACTORY_NAME).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's name.", e);
        }
    }

    public void setArtifactoryName(String name) {
        StringValue value = new StringValue(name);
        setPropValue(PROP_ARTIFACTORY_NAME, value);
    }

    /**
     * Get the absolute path of the item
     */
    public String getAbsolutePath() {
        return absPath;
    }

    /**
     * Get the relative path of the item
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public String getRelativePath() {
        return relPath;
    }

    public long getCreated() {
        try {
            //This property is auto-populated on node creation
            return getPropValue(JCR_CREATED).getDate().getTimeInMillis();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's created.", e);
        }
    }

    public RepoPath getRepoPath() {
        return new RepoPath(getRepoKey(), getRelativePath());
    }

    public String getRepoKey() {
        try {
            return getPropValue(PROP_ARTIFACTORY_REPO_KEY).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve repository key.", e);
        }
    }

    public void setRepoKey(String repoKey) {
        StringValue value = new StringValue(repoKey);
        try {
            setPropValue(PROP_ARTIFACTORY_REPO_KEY, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getModifiedBy() {
        try {
            return getPropValue(PROP_ARTIFACTORY_MODIFIED_BY).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve modified-by.", e);
        }
    }

    public void setModifiedBy(String modifiedBy) {
        StringValue value = new StringValue(modifiedBy);
        setPropValue(PROP_ARTIFACTORY_MODIFIED_BY, value);
    }

    protected Value getPropValue(String prop) {
        try {
            return getNode().getProperty(prop).getValue();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve value for property '" + prop + "'.", e);
        }
    }

    protected boolean hasProp(String prop) {
        try {
            return getNode().hasProperty(prop);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to check existence for property '" + prop + "'.", e);
        }
    }

    public void setPropValue(String prop, Value value) {
        try {
            getNode().setProperty(prop, value);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to set value for property '" + prop + "'.", e);
        }
    }

    public boolean delete() {
        return delete(true);
    }

    public boolean delete(boolean lockBeforeDeletion) {
        if (!exists()) {
            return false;
        }
        Node node = getNode();
        if (lockBeforeDeletion) {
            NodeLock.lock(node);
        }
        try {
            RepoPath repoPath = getRepoPath();
            node.remove();
            AccessLogger.deleted(repoPath);
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to remove node.", e);
        }
        return true;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFolder getParentFolder() {
        try {
            Node parent = getNode().getParent();
            JcrFolder parentFolder = new JcrFolder(parent);
            return parentFolder;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get node's parent folder.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public boolean renameTo(final String destAbsPath) {
        JcrWrapper jcr = ContextHelper.get().getJcr();
        boolean result = jcr.doInSession(new JcrCallback<Boolean>() {
            public Boolean doInJcr(JcrSessionWrapper session) throws RepositoryException {
                String srcAbsPath = getAbsolutePath();
                session.move(srcAbsPath, destAbsPath);
                JcrFsItem.this.absPath = destAbsPath;
                setRelativePathFromRepositoryAbsPath(absPath, true);
                return true;
            }
        });
        return result;
    }

    /**
     * OVERIDDEN FROM FILE BEGIN
     */
    @Override
    public String getParent() {
        return getParentFolder().getAbsolutePath();
    }

    @Override
    public File getParentFile() {
        return getParentFolder();
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public File getAbsoluteFile() {
        return this;
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return getAbsolutePath();
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return this;
    }

    @Override
    public boolean canRead() {
        ArtifactorySecurityManager security = ContextHelper.get().getSecurity();
        RepoPath repoPath = getRepoPath();
        return security.canRead(repoPath);
    }

    @Override
    public boolean canWrite() {
        ArtifactorySecurityManager security = ContextHelper.get().getSecurity();
        RepoPath repoPath = getRepoPath();
        return security.canDeploy(repoPath);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    @Override
    public boolean exists() {
        JcrWrapper jcr = ContextHelper.get().getJcr();
        boolean result = jcr.doInSession(new JcrCallback<Boolean>() {
            public Boolean doInJcr(JcrSessionWrapper session) throws RepositoryException {
                String absPath = getAbsolutePath();
                return session.itemExists(absPath);
            }
        });
        return result;
    }

    @Override
    public boolean isFile() {
        return !isDirectory();
    }

    @Override
    public boolean isHidden() {
        return getName().startsWith(".");
    }

    @Override
    public boolean createNewFile() throws IOException {
        return false;
    }

    @Override
    public void deleteOnExit() {
        throw new UnsupportedOperationException("deleteOnExit() is not supported for jcr.");
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    @Override
    public boolean renameTo(File dest) {
        String destAbsPath = dest.getAbsolutePath();
        return renameTo(destAbsPath);
    }

    @Override
    public boolean setReadOnly() {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    public boolean setWritable(boolean writable) {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    public boolean setReadable(boolean readable) {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    public boolean setExecutable(boolean executable) {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    public boolean canExecute() {
        return false;
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    public long getTotalSpace() {
        throw new UnsupportedOperationException("getTotalSpace() is not supported for jcr.");
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    public long getFreeSpace() {
        throw new UnsupportedOperationException("getFreeSpace() is not supported for jcr.");
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    public long getUsableSpace() {
        throw new UnsupportedOperationException("getUsableSpace() is not supported for jcr.");
    }

    @Override
    public int compareTo(File item) {
        return getName().compareTo(item.getName());
    }

    @Override
    public String toString() {
        return getRepoKey() + ":" + getRelativePath();
    }

    @Override
    public String getPath() {
        return absPath;
    }

    @SuppressWarnings({"deprecation"})
    @Override
    @Deprecated
    public URL toURL() throws MalformedURLException {
        return new URL("jcr", "", absPath);
    }

    @Override
    public URI toURI() {
        try {
            return new URI("jcr", null, absPath, null);
        } catch (URISyntaxException x) {
            throw new Error(x);// Can't happen
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JcrFsItem)) {
            return false;
        }
        JcrFsItem item = (JcrFsItem) o;
        return absPath.equals(item.absPath);
    }

    public int hashCode() {
        return absPath.hashCode();
    }

    @Override
    public abstract long lastModified();

    @Override
    public abstract long length();

    @Override
    public abstract String[] list();

    @Override
    public abstract String[] list(FilenameFilter filter);

    @Override
    public abstract File[] listFiles();

    @Override
    public abstract File[] listFiles(FilenameFilter filter);

    @Override
    public abstract File[] listFiles(FileFilter filter);

    @Override
    public abstract boolean mkdir();

    @Override
    public abstract boolean mkdirs();

    @Override
    public abstract boolean setLastModified(long time);

    /**
     * OVERIDDEN FROM FILE END
     */

    public abstract boolean isDirectory();

    public abstract FsItemMetadata getMetadata();

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    protected Node getNode() {
        JcrWrapper jcr = ContextHelper.get().getJcr();
        Item node = jcr.doInSession(new JcrCallback<Item>() {
            public Item doInJcr(JcrSessionWrapper session) throws RepositoryException {
                String absPath = getAbsolutePath();
                return session.getItem(absPath);
            }
        });
        return (Node) node;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    protected static String repoKeyFromPath(String absPath) {
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
        int idx = absPath.indexOf(repoJcrRootPath);
        if (idx == -1) {
            throw new IllegalArgumentException("Path '" + absPath + "' is not a repository path.");
        }
        int repoKeyEnd = absPath.indexOf("/", repoJcrRootPath.length() + 1);
        int repoKeyBegin = repoJcrRootPath.length() + 1;
        String repoKey = repoKeyEnd > 0 ? absPath.substring(repoKeyBegin, repoKeyEnd) :
                absPath.substring(repoKeyBegin);
        return repoKey;
    }

    protected static String getAbsPath(Node node) {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's absolute path:" + node, e);
        }
    }

    private static String removeTrailingSlashes(String absPath) {
        if (absPath.endsWith("/")) {
            String modifiedPath = absPath.substring(0, absPath.length() - 1);
            return removeTrailingSlashes(modifiedPath);
        }
        return absPath;
    }

    private static String removeFrontSlashes(String relPath) {
        if (relPath.startsWith("/")) {
            String modifiedPath = relPath.substring(1);
            return removeFrontSlashes(modifiedPath);
        }
        return relPath;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private void setRelativePathFromRepositoryAbsPath(String absPath, boolean updateRepoKey) {
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
        if ("/".equals(repoJcrRootPath)) {
            this.relPath = absPath.substring(1);
        } else {
            String repoKey = repoKeyFromPath(absPath);
            if (updateRepoKey) {
                boolean repoKeyChanged = true;
                if (hasProp(PROP_ARTIFACTORY_REPO_KEY)) {
                    String oldRepoKey = getRepoKey();
                    repoKeyChanged = !oldRepoKey.equals(repoKey);
                }
                if (repoKeyChanged) {
                    setRepoKey(repoKey);
                }
            }
            String relPath = removeFrontSlashes(
                    absPath.substring(repoJcrRootPath.length() + repoKey.length() + 1));
            this.relPath = relPath;
        }
        int nameStart = relPath.lastIndexOf('/');
        //If there is no name it is the repository root
        this.name = nameStart > 0 ? relPath.substring(nameStart + 1) : relPath;
    }
}
