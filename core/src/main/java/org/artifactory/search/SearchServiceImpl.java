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

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ConstantsValue;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: freds Date: Jul 27, 2008 Time: 6:04:39 PM
 */
@Service
public class SearchServiceImpl implements SearchService {
    private final static Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private AuthorizationService authService;

    public List<SearchResult> searchArtifacts(SearchControls controls) {
        long start = System.currentTimeMillis();
        String search = controls.getSearch();
        if (search == null) {
            return Collections.emptyList();
        }
        try {
            String exp = stringToJcrSearchExp(search);

            // select all the elements of type artifactory:file with element artifactory:name
            // that contains the search term. we use jackrabbit specific function fn:lower-case to allow jcr:like
            // queries that are not case sensitive.
            String queryStr = "/jcr:root/repositories//element(*, " + JcrFile.NT_ARTIFACTORY_FILE + ") " +
                    "[jcr:like(fn:lower-case(@" + JcrFsItem.PROP_ARTIFACTORY_NAME + ")," + exp.toLowerCase() + ")] " +
                    "order by @" + JcrFsItem.PROP_ARTIFACTORY_NAME + " ascending";

            QueryResult queryResult = jcrService.executeXpathQuery(queryStr);
            List<SearchResult> results = new ArrayList<SearchResult>();
            NodeIterator nodes = queryResult.getNodes();
            //Filter the results and stop when reached more than max results + 1
            while (nodes.hasNext() && (results.size() < ConstantsValue.searchMaxResults.getInt() + 1)) {
                Node artifactNode = nodes.nextNode();
                RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
                LocalRepo localRepo = repoService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
                if (localRepo == null) {
                    // Some left over in JCR of non configured repo
                    continue;
                }

                if (NamingUtils.isChecksum(repoPath.getPath())) {
                    // don't show checksum files
                    continue;
                }

                JcrFile jcrFile = localRepo.getJcrFile(repoPath);
                ArtifactResource artifact = new ArtifactResource(jcrFile.getInfo());
                boolean canRead = authService.canRead(jcrFile.getRepoPath());
                MavenArtifactInfo mavenInfo = artifact.getMavenInfo();
                if (canRead && mavenInfo.isValid()) {
                    SearchResult result = new SearchResult(jcrFile.getInfo(), mavenInfo);
                    results.add(result);
                }
            }

            log.debug("Total search time: {} ms", System.currentTimeMillis() - start);
            return results;
        } catch (RepositoryException e) {
            log.error("Failed to execute search query: {}", e.getMessage());
            throw new RepositoryRuntimeException("Search failed.", e);
        }

    }

    private static String stringToJcrSearchExp(String expression) {
        String escapedEpression = escapeToJcrLikeString(expression);
        // now wrap with % to allow searching all artifacts that contains the expression
        String result = "'%" + escapedEpression + "%'";
        return result;
    }

    /**
     * Convert a string to a JCR search expression literal, suitable for use in jcr:like() (inside XPath queries).
     * The characters -, ', " and \ have special meaning, and must be escaped with a backslash to obtain their literal
     * value. This method doesn't escape the characters '%' and '_' to allow users to use wildcards.
     * The characters '*' and '?' are converted to '%' or '_' respectively to allow using both types of wildcards.
     * See JSR-170 spec v1.0, Sec. 6.6.5.1.
     *
     * @param expression A string to escape.
     * @return A valid XPath 2.0 string literal suitable for use in jcr:like(), including enclosing '%'.
     */
    private static String escapeToJcrLikeString(String expression) {
        StringBuffer sb = new StringBuffer();
        char[] chars = expression.toCharArray();
        for (char c : chars) {
            if (c == '\'') {
                sb.append('\'');
            } else if (c == '\"' || c == '\\') {
                sb.append('\\');
            } else if (c == '*') {
                c = '%'; // treat '*' as % character (match any number of characters)
            } else if (c == '?') {
                c = '_'; // treat '?' as '_' (match any single character)
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static String escapeToJcrContainsString(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-' || c == '\'' || c == '\"' || c == '\\') {
                sb.append('\\');
            }
            if (c != '^') { // see RTFACT-1024
                sb.append(c);
            }
        }
        return sb.toString();
    }

    //We can use this method to monitor the actions of the Lucene StandardAnalyzer
    @SuppressWarnings({"UnusedDeclaration"})
    private void debugLuceneStandardAnalyzer(String search) {
        TokenStream tokenStream =
                new StandardAnalyzer().tokenStream("dummy", new StringReader(search));
        Token token;
        try {
            while ((token = tokenStream.next()) != null) {
                String text = token.termText();
                System.out.println("text = " + text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
