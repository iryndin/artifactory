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
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.maven.MavenUtils;
import org.artifactory.request.ArtifactoryRequest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class StrayDataCleaner implements JcrCallback<Object> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(StrayDataCleaner.class);

    private final String queryString;

    public StrayDataCleaner(ArtifactoryRequest request) {
        String dir = request.getDir();
        /*
        //Encode numbers in the dir path, according to sec 6.4.3 of the jcr spec
        //See also the discussion at http://issues.apache.org/jira/browse/JCR-579
        String encodedDir = dir != null ? ISO9075.encode(dir) : "*";
        this.queryString = "/jcr:root*/
        /*//*" + encodedDir +
        "//element(*, " + ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE + ")" +
        "[@" + ArtifactoryJcrConstants.PROP_ARTIFACTORY_NAME + "='maven-metadata.xml']";
        */
        this.queryString = "SELECT * FROM " + JcrFile.NT_ARTIFACTORY_FILE +
                " WHERE jcr:path LIKE '/%/" + dir + "/%'" +
                " AND " + JcrFsItem.PROP_ARTIFACTORY_NAME + "='maven-metadata.xml'";
    }

    public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
        Set<Node> results = new HashSet<Node>();
        Workspace workSpace = session.getWorkspace();
        QueryManager queryManager = workSpace.getQueryManager();
        //Find all the metadata.ext files and check that their parent has real data
        Query query = queryManager.createQuery(queryString, Query.SQL);
        // Lucene is locking during creation of it's query cache so do this
        // out of the create session
        QueryResult queryResult = query.execute();
        NodeIterator nodes = queryResult.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            //We are dirty if we are inside an edge folder node and have no no-metada siblings
            Node parent = node.getParent();
            if (parent.isNodeType(JcrFolder.NT_ARTIFACTORY_FOLDER)) {
                //Are we an edge folder and there are no real artifacts around the metadata file
                boolean fits = true;
                NodeIterator children = parent.getNodes();
                Collection<Node> childrenList = new ArrayList<Node>();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    childrenList.add(child);
                    String childNodeName = child.getName();
                    NodeType childNodeType = child.getPrimaryNodeType();
                    if (childNodeType.isNodeType(JcrFolder.NT_ARTIFACTORY_FOLDER) ||
                            !MavenUtils.isMetadata(childNodeName)) {
                        fits = false;
                        break;
                    }
                }
                if (fits) {
                    results.addAll(childrenList);
                }
            }
            //Delete all the found artifacts
            for (Node result : results) {
                Node resultParent = result.getParent();
                NodeLock.lock(resultParent);
                result.remove();
            }
        }
        return null;
    }
}
