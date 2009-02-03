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

import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.resource.ArtifactResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * User: freds Date: Jul 27, 2008 Time: 6:04:39 PM
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private JcrService jcr;

    @Autowired
    private AuthorizationService authService;

    @Transactional(readOnly = true)
    public List<SearchResult> searchArtifacts(SearchControls controls) {
        String search = controls.getSearch();

        List<SearchResult> results = new ArrayList<SearchResult>();
        if (search == null) {
            return results;
        }
        JcrSession session = jcr.getManagedSession();
        Workspace workSpace = session.getWorkspace();
        try {
            QueryManager queryManager = workSpace.getQueryManager();
            String exp = stringToJcrSearchExp(search);
            String queryStr = "//*[jcr:contains(@" + JcrFsItem.PROP_ARTIFACTORY_NAME + "," + exp +
                    ")] order by jcr:score() descending";
            /*String queryStr = "//relPath/element(jcr:xmltext, nt:unstructured)" +
                    "[jcr:like(fn:lower-case(@jcr:xmlcharacters), " + exp +
                    ")] order by jcr:score() descending";*/
            /*
            String queryStr = "/jcr:root//" + FileInfo.ROOT + "/relPath/jcr:xmltext" +
                            "[jcr:contains(@jcr:xmlcharacters, " + exp +
                            ")] order by jcr:score() descending";
            */
            //queryStr = "//jcr:xmltext[jcr:contains(@jcr:xmlcharacters,'c')]";
            Query query = queryManager.createQuery(queryStr, Query.XPATH);
            QueryResult queryResult = query.execute();
            NodeIterator nodes = queryResult.getNodes();
            //Filter the results
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String absPath = node.getPath();
                int relPathEnd = absPath.indexOf("/" + JcrFsItem.NODE_ARTIFACTORY_METADATA + "/");
                if (relPathEnd == -1) {
                    relPathEnd = absPath.length();
                }
                String relPathFromRootNode = absPath.substring(1, relPathEnd);
                Node artifactNode = session.getRootNode().getNode(relPathFromRootNode);
                String artifactNodeTypeName = artifactNode.getPrimaryNodeType().getName();
                if (JcrFile.NT_ARTIFACTORY_FILE.equals(artifactNodeTypeName)) {
                    JcrFile jcrFile = new JcrFile(artifactNode);
                    ArtifactResource artifact = new ArtifactResource(jcrFile.getInfo());
                    boolean canRead = authService.canRead(jcrFile.getRepoPath());
                    if (canRead && artifact.isValid()) {
                        SearchResult result = new SearchResult(
                                jcrFile.getInfo(),
                                artifact.getMavenArtifactInfo());
                        results.add(result);
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Search failed.", e);
        }
        return results;
    }

    /**
     * Convert a string to a JCR search expression literal, suitable for use in jcr:contains()
     * (inside XPath queries). The characters - and " have special meaning, and may be escaped with
     * a backslash to obtain their literal value. See JSR-170 spec v1.0, Sec. 6.6.5.2.
     *
     * @param str Any string.
     * @return A valid XPath 2.0 string literal suitable for use in jcr:contains(), including
     *         enclosing quotes.
     */
    private static String stringToJcrSearchExp(String str) {
        String exp = "'*" + escapeQueryChars(str.toLowerCase()).replaceAll("'", "''") + "*'";
        return exp;
    }

    private static String escapeQueryChars(String str) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' ||
                    c == '~' || c == '*' || c == '?') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
