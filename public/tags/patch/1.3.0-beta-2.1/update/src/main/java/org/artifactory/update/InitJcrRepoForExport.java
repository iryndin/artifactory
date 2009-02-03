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
package org.artifactory.update;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.artifactory.jcr.InitJcrRepoStrategy;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

/**
 * User: freds Date: Jun 3, 2008 Time: 1:31:07 PM
 */
public class InitJcrRepoForExport extends InitJcrRepoStrategy {
    public InitJcrRepoForExport(JcrWrapper jcrWrapper) {
        super(jcrWrapper);
        jcrWrapper.setReadOnly(true);
    }

    protected void initializeRepoRoot() {
        jcrWrapper.doInSession(new JcrCallback<Node>() {
            public Node doInJcr(JcrSessionWrapper session) throws RepositoryException {
                Node root = session.getRootNode();
                NodeIterator iterator = root.getNodes();
                while (iterator.hasNext()) {
                    Node node = iterator.nextNode();
                    System.out.println(node.getPath());
                }
                return root;
            }
        });

        String path = JcrPath.get().getRepoJcrRootPath();
        if ("/".equals(path)) {
            // Nothing it's the root
        } else {
            // Check that it exists (will throw exception if not we are read only)
            jcrWrapper.getOrCreateUnstructuredNode(path);
        }
    }

    protected void registerTypes(Workspace workspace, NodeTypeDef[] types)
            throws RepositoryException, InvalidNodeTypeDefException {
        // Do nothing since we don't want to have more types

        //Get the NodeTypeManager from the Workspace.
        //Note that it must be cast from the generic JCR NodeTypeManager to the
        //Jackrabbit-specific implementation.
        NodeTypeManagerImpl ntmgr =
                (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        //Acquire the NodeTypeRegistry
        NodeTypeRegistry ntReg = ntmgr.getNodeTypeRegistry();
        //Create or update (reregister) all NodeTypeDefs
        for (NodeTypeDef ntd : types) {
            Name name = ntd.getName();
            if (!ntReg.isRegistered(name)) {
                ntReg.registerNodeType(ntd);
            } else {
                // Do nothing since we don't want to have more types
            }
        }
    }
}
