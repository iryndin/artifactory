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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryConstants;
import org.artifactory.fs.FolderMetadata;
import org.artifactory.fs.FsItemMetadata;
import static org.artifactory.jcr.ArtifactoryJcrConstants.*;
import org.artifactory.process.StatusHolder;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class JcrFolder extends JcrFsItem {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrFolder.class);


    public JcrFolder(Node node) {
        super(node);
    }

    public List<JcrFsItem> getItems() {
        List<JcrFsItem> items = new ArrayList<JcrFsItem>();
        try {
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String typeName = node.getPrimaryNodeType().getName();
                if (typeName.equals(NT_ARTIFACTORY_FOLDER)) {
                    items.add(new JcrFolder(node));
                } else if (typeName.equals(NT_ARTIFACTORY_FILE)) {
                    items.add(new JcrFile(node));
                }
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to retrieve folder node items.", e);
        }
        return items;
    }

    public void export(File targetDir, boolean includeMetadata) {
        try {
            List<JcrFsItem> list = getItems();
            for (JcrFsItem item : list) {
                String relPath = item.relPath();
                File targetFile = new File(targetDir, relPath);
                if (item.isFolder()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Exporting directory '" + relPath + "'...");
                    }
                    boolean res = targetFile.exists() || targetFile.mkdirs();
                    if (res) {
                        JcrFolder jcrFolder = ((JcrFolder) item);
                        jcrFolder.export(targetDir, includeMetadata);
                        if (includeMetadata) {
                            FolderMetadata metadata = getMetadata();
                            File parentFile = targetFile.getParentFile();
                            File metadataFile =
                                    new File(parentFile,
                                            targetFile.getName() + FsItemMetadata.SUFFIX);
                            //Reuse the output stream
                            FileOutputStream os = null;
                            try {
                                os = new FileOutputStream(metadataFile);
                                XStream xstream = new XStream();
                                Annotations.configureAliases(xstream, FolderMetadata.class);
                                xstream.toXML(metadata, os);
                            } finally {
                                IOUtils.closeQuietly(os);
                            }
                        }
                    } else {
                        throw new IOException(
                                "Failed to create directory '" + targetFile.getPath() + "'.");
                    }
                } else {
                    JcrFile jcrFile = ((JcrFile) item);
                    jcrFile.export(targetFile, includeMetadata);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to export to dir '" + targetDir.getPath() + "'.", e);
        }
    }

    public void exportTo(String basePath, StatusHolder status) {
        File targetFile = new File(basePath, relPath());
        export(targetFile, true);
    }

    public void importFrom(String basePath, StatusHolder status) {
        FileInputStream is = null;
        try {
            //Read metadata into the node
            File file = new File(basePath, relPath());
            File parentFile = file.getParentFile();
            File metadataFile = new File(parentFile, file.getName() + FsItemMetadata.SUFFIX);
            if (metadataFile.exists()) {
                LOGGER.debug("Importing metadata from '" + metadataFile.getPath() + "'.");
                //Reuse the input stream
                IOUtils.closeQuietly(is);
                is = new FileInputStream(metadataFile);
                XStream xstream = new XStream();
                Annotations.configureAliases(xstream, FolderMetadata.class);
                FolderMetadata metadata = (FolderMetadata) xstream.fromXML(is);
                if (!node.hasProperty(PROP_ARTIFACTORY_REPO_KEY)) {
                    //Do not override the repo key (when importing to a repo with a different key)
                    node.setProperty(PROP_ARTIFACTORY_REPO_KEY, metadata.getRepoKey());
                }
                node.setProperty(PROP_ARTIFACTORY_MODIFIED_BY, metadata.getModifiedBy());
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No metadata found for '" + file.getPath() + "'.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to file import into '" + relPath() + "'.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public boolean isFolder() {
        return true;
    }

    @Override
    public void delete() {
        delete(true);
    }

    public void delete(boolean singleTransaction) {
        if (!singleTransaction && !ArtifactoryConstants.forceAtomicTransacions) {
            deleteChildren(singleTransaction);
        }
        super.delete();
    }

    public void deleteChildren(boolean singleTransaction) {
        //Lock the parent to avoid possible pending changes exception on save
        NodeLock.lock(node);
        List<JcrFsItem> children = getItems();
        for (JcrFsItem child : children) {
            if (child.isFolder()) {
                ((JcrFolder) child).delete(singleTransaction);
            } else {
                child.delete();
            }
            if (!singleTransaction && !ArtifactoryConstants.forceAtomicTransacions) {
                try {
                    Session session = node.getSession();
                    session.save();
                } catch (RepositoryException e) {
                    throw new RuntimeException("Failed to save jcr session.", e);
                }
            }
        }
    }

    public List<JcrFolder> withEmptyChildren() {
        JcrFolder parent = this;
        List<JcrFolder> result = new ArrayList<JcrFolder>();
        while (true) {
            List<JcrFsItem> children = parent.getItems();
            result.add(parent);
            if (children.size() == 1 && children.get(0).isFolder()) {
                parent = (JcrFolder) children.get(0);
            } else {
                break;
            }
        }
        return result;
    }

    public FolderMetadata getMetadata() {
        return new FolderMetadata(repoKey(), relPath(),
                created() != null ? created().getTime() : -1, modifiedBy());
    }
}
