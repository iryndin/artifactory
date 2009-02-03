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
package org.artifactory.search;

import org.apache.log4j.Logger;
import org.artifactory.jcr.ArtifactoryJcrConstants;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrHelper;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SearchHelper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SearchHelper.class);

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static List<SearchResult> searchArtifacts(SearchControls controls) {
        String search = controls.getSearch();
        ArtifactoryContext context = ContextUtils.getArtifactoryContext();
        SearchCallback callback = new SearchCallback(search);
        JcrHelper jcrHelper = context.getCentralConfig().getJcr();
        List<SearchResult> results = jcrHelper.doInSession(callback);
        return results;
    }

    private static class SearchCallback implements JcrCallback<List<SearchResult>> {
        private String search;

        public SearchCallback(String search) {
            this.search = search;
        }

        public List<SearchResult> doInJcr(JcrSessionWrapper session) throws RepositoryException {
            List<SearchResult> results = new ArrayList<SearchResult>();
            if (search == null) {
                return results;
            }
            Workspace workSpace = session.getWorkspace();
            QueryManager queryManager = workSpace.getQueryManager();
            String wildcard = "*" + search + "*";
            String queryStr =
                    "/jcr:root//element(*, " + ArtifactoryJcrConstants.NT_ARTIFACTORY_FILE +
                            ")[jcr:contains(@" + ArtifactoryJcrConstants.PROP_ARTIFACTORY_NAME +
                            ", '" + wildcard + "')]";
            /*String queryStr = "/jcr:root//project//jcr:xmltext"
                    + "[jcr:contains(., '" + wildcard + "')]";*/
            Query query = queryManager.createQuery(queryStr, Query.XPATH);
            QueryResult queryResult = query.execute();
            NodeIterator nodes = queryResult.getNodes();
            //Filer the results
            ArtifactoryContext context = ContextUtils.getArtifactoryContext();
            SecurityHelper security = context.getSecurity();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String absPath = node.getPath();
                int relPathEnd = absPath.indexOf("/artifactory:xml/");
                if (relPathEnd == -1) {
                    relPathEnd = absPath.length();
                }
                String relPath = absPath.substring(1, relPathEnd);
                Node artifactNode = session.getRootNode().getNode(relPath);
                JcrFile jcrFile = new JcrFile(artifactNode);
                ArtifactResource artifact = new ArtifactResource(jcrFile);
                boolean canRead = security.canRead(artifact);
                if (canRead && artifact.isStandardPackaging()) {
                    SearchResult result = new SearchResult(artifact);
                    results.add(result);
                }
            }
            return results;
        }
    }
}
