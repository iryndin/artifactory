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
package org.artifactory.jcr;

import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.fs.FsItemMetadata;
import static org.artifactory.jcr.ArtifactoryJcrConstants.PROP_ARTIFACTORY_MODIFIED_BY;
import static org.artifactory.jcr.ArtifactoryJcrConstants.PROP_ARTIFACTORY_REPO_KEY;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.RepoPath;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class JcrFsItem implements Comparable<JcrFsItem>, ExportableConfig {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFsItem.class);

    protected final Node node;
    private final String relPath;

    public JcrFsItem(Node fileNode) {
        this.node = fileNode;
        String repoKey = repoKey();
        String absPath = absPath();
        int relPathBegin = absPath.indexOf(repoKey) + repoKey.length() + 1;
        if (relPathBegin == absPath.length() + 1) {
            //The folder node of the repo itself
            relPath = "";
        } else {
            relPath = absPath.substring(relPathBegin);
        }
    }

    public String getName() {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's name.", e);
        }
    }

    /**
     * Get the absolute path of the item
     */
    public String absPath() {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's absolute path.", e);
        }
    }

    /**
     * Get the relative path of the item
     */
    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public String relPath() {
        return relPath;
    }

    public Date created() {
        try {
            //This property is auto-populated on node creation
            Property prop = node.getProperty(JCR_CREATED);
            return prop.getDate().getTime();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve node's created.", e);
        }
    }

    public long createdTime() {
        Date date = created();
        return date != null ? date.getTime() : 0;
    }

    public RepoPath getRepoPath() {
        return new RepoPath(repoKey(), relPath());
    }

    public String repoKey() {
        try {
            return node.getProperty(PROP_ARTIFACTORY_REPO_KEY).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve repository key.", e);
        }
    }

    public String modifiedBy() {
        try {
            return node.getProperty(PROP_ARTIFACTORY_MODIFIED_BY).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve modified-by.", e);
        }
    }

    public void delete() {
        NodeLock.lock(node);
        try {
            AccessLogger.deleted(getRepoPath());
            node.remove();
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to remove node.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public JcrFolder getParent() {
        try {
            Node parent = node.getParent();
            JcrFolder parentFolder = new JcrFolder(parent);
            return parentFolder;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to get node's parent folder.", e);
        }
    }

    public int compareTo(JcrFsItem item) {
        return getName().compareTo(item.getName());
    }

    @Override
    public String toString() {
        return repoKey() + ":" + relPath();
    }

    public abstract boolean isFolder();

    public abstract FsItemMetadata getMetadata();
}
