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

import org.apache.log4j.Logger;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE;
import static org.artifactory.jcr.ArtifactoryJcrConstants.NT_ARTIFACTORY_FOLDER;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
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

    public void export(final File targetDir) {
        try {
            List<JcrFsItem> list = getItems();
            for (JcrFsItem item : list) {
                String relPath = item.relPath();
                File targetFile = new File(targetDir, relPath);
                if (item.isDirectory()) {
                    LOGGER.info("Exporting directory '" + relPath + "'...");
                    boolean res = targetFile.exists() || targetFile.mkdirs();
                    if (res) {
                        JcrFolder jcrFolder = ((JcrFolder) item);
                        jcrFolder.export(targetDir);
                    } else {
                        throw new IOException(
                                "Failed to create directory '" + targetFile.getPath() + "'.");
                    }
                } else {
                    JcrFile jcrFile = ((JcrFile) item);
                    jcrFile.export(targetFile);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to export to dir '" + targetDir.getPath() + "'.", e);
        }
    }

    public boolean isDirectory() {
        return true;
    }

    @Override
    public void remove() {
        //Do not remove self
        String path = relPath();
        if (path.length() == 0) {
            List<JcrFsItem> children = getItems();
            for (JcrFsItem child : children) {
                child.remove();
            }
        } else {
            super.remove();
        }
    }
}
