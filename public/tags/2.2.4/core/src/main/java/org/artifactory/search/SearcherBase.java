/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.search;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchControlsBase;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.jcr.fs.FolderInfoProxy;
import org.artifactory.jcr.fs.ItemInfoProxy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.jackrabbit.DataStoreRecordNotFoundException;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.ExceptionUtils;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

/**
 * @author yoavl
 */
public abstract class SearcherBase<C extends SearchControls, R extends SearchResult> implements Searcher<C, R> {
    private static final Logger log = LoggerFactory.getLogger(SearcherBase.class);
    protected static final String FORWARD_SLASH = "/";

    private final JcrService jcrService;
    private final InternalRepositoryService repoService;
    private final AuthorizationService authService;

    //Cache this calculation (and don't make it a static...)
    private final int maxResults = ConstantValues.searchMaxResults.getInt() + 1;

    protected SearcherBase() {
        ArtifactoryContext context = ContextHelper.get();
        jcrService = context.beanForType(JcrService.class);
        repoService = context.beanForType(InternalRepositoryService.class);
        authService = context.beanForType(AuthorizationService.class);
    }

    public final SearchResults<R> search(C controls) {
        long start = System.currentTimeMillis();
        SearchResults<R> results;
        try {
            results = doSearch(controls);
        } catch (Exception e) {
            //Handle bad queries
            Throwable invalidQueryException = ExceptionUtils.getCauseOfTypes(e, InvalidQueryException.class);
            if (invalidQueryException != null) {
                log.debug("Invalid query encountered.", e);
                throw new RepositoryRuntimeException("Invalid query: " + e.getMessage(), e);
            } else {
                log.error("Could not perform search.", e);
                throw new RepositoryRuntimeException("Could not execute search query (" + e.getMessage() + ").", e);
            }
        }
        long time = System.currentTimeMillis() - start;
        results.setTime(time);
        log.debug("Total search time: {} ms", time);
        return results;
    }

    protected StringBuilder getPathQueryBuilder(SearchControlsBase controls) {
        StringBuilder queryBuilder = new StringBuilder("/");
        addRepoToQuery(controls, queryBuilder);
        return queryBuilder;
    }

    //Add specific repositories to search from. If list is empty query will search all repos

    private void addRepoToQuery(SearchControlsBase controls, StringBuilder queryBuilder) {
        if (controls.isSpecificRepoSearch()) {
            queryBuilder.append("jcr:root").append(JcrPath.get().getRepoJcrRootPath()).append(FORWARD_SLASH).
                    append(". [");

            Iterator<String> repoKeys = controls.getSelectedRepoForSearch().iterator();

            while (repoKeys.hasNext()) {
                queryBuilder.append("fn:name() = '").append(repoKeys.next()).append("'");
                if (repoKeys.hasNext()) {
                    queryBuilder.append(" or ");
                }
            }

            queryBuilder.append("]/");
        }
    }

    public abstract SearchResults<R> doSearch(C controls) throws RepositoryException;

    public JcrService getJcrService() {
        return jcrService;
    }

    public InternalRepositoryService getRepoService() {
        return repoService;
    }

    public AuthorizationService getAuthService() {
        return authService;
    }

    //We can use this method to monitor the actions of the Lucene StandardAnalyzer

    @SuppressWarnings({"UnusedDeclaration"})
    private void debugLuceneStandardAnalyzer(String search) {
        TokenStream tokenStream =
                new StandardAnalyzer().tokenStream("dummy", new StringReader(search));
        Token token;
        try {
            while ((token = tokenStream.next()) != null) {
                String text = token.toString();
                System.out.println("text = " + text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the Fragment JDom elements from the highlighting excerpt
     *
     * @param excerpt Highlighting excerpt
     * @return Element[] - fragments
     */
    @SuppressWarnings({"unchecked"})
    protected Element[] getFragmentElements(String excerpt) {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        Document document;
        try {
            document = builder.build(new StringReader(excerpt));
        } catch (Exception e) {
            log.error("An error has occurred while parsing the search result excerpt", e);
            return new Element[]{};
        }
        Element element = document.getRootElement();
        List<Element> fragments = element.getChildren("fragment");
        return fragments.toArray(new Element[fragments.size()]);
    }

    /**
     * Returns the beginning of the path (if any) before a highlighted element in a list of Element contents.
     *
     * @param children   List of Element contents
     * @param path       The path StringBuilder
     * @param childIndex The index of the highlighted child
     */
    protected void getPathBeforeHighlight(List<Content> children, StringBuilder path, int childIndex) {
        //Make sure we are not the first element in the array
        if (childIndex > 0) {
            for (int i = (childIndex - 1); i > -1; i--) {

                //Get element
                Content element = children.get(i);
                String elementValue = element.getValue();

                //Paths are separated by spaces, so we should stop at the last space occurrence
                int lastSpace = elementValue.lastIndexOf(' ');
                if (lastSpace < 0) {
                    path.insert(0, elementValue);
                } else {
                    String pathPiece = elementValue.substring(lastSpace + 1);
                    path.insert(0, pathPiece);
                    break;
                }
            }
        }
    }

    /**
     * Returns the rest of the path (if any) after a highlighted element in a list of Element contents.
     *
     * @param children   List of Element contents
     * @param path       The path StringBuilder
     * @param childIndex The index of the highlighted child
     */
    protected void getPathAfterHighlight(List<Content> children, StringBuilder path, int childIndex) {
        //Make sure we are not at the end of the array
        if (childIndex < children.size()) {
            for (int i = (childIndex + 1); i < children.size(); i++) {

                //Get element
                Content element = children.get(i);
                String elementValue = element.getValue();

                //Paths are separated by spaces, so we should stop at the next space occurrence
                int firstSpace = elementValue.indexOf(' ');
                if (firstSpace < 0) {
                    path.append(elementValue);
                } else {
                    String pathPiece = elementValue.substring(0, firstSpace);
                    path.append(pathPiece);
                    break;
                }
            }
        }
    }

    protected int getMaxResults() {
        return maxResults;
    }

    protected ItemInfo getProxyItemInfo(Node artifactNode) throws RepositoryException {
        ItemInfoProxy itemInfo;
        RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
        if (JcrFile.isFileNode(artifactNode)) {
            itemInfo = new FileInfoProxy(repoPath);
        } else {
            itemInfo = new FolderInfoProxy(repoPath);
        }
        return itemInfo;
    }

    /**
     * Appends the given String to the given StringBuilder, and adds a forward slash to the end
     *
     * @param sb       StringBuilder to add content to
     * @param elements Elements to add
     */
    protected void addElementsToSb(StringBuilder sb, String... elements) {
        for (String element : elements) {
            sb.append(element).append(FORWARD_SLASH);
        }
    }

    /**
     * Indicates whether the given result repo path is valid or not.<br> A repo path will stated as valid if it is
     * originated in a local repository and if it is not a checksum file.
     *
     * @param repoPath Repo path to validate
     * @return True if the repo path is valid
     */
    protected boolean isResultRepoPathValid(RepoPath repoPath) {
        LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
        return isRepoPathValid(repoPath, localRepo);
    }

    /**
     * Indicates whether the given result repo path is valid or not.<br> A repo path will stated as valid if the given
     * origin local repo is not null and if it is not a checksum file.
     *
     * @param repoPath        Repo path to validate
     * @param sourceLocalRepo Source local repo to assert as valid
     * @return True if the repo path is valid
     */
    protected boolean isRepoPathValid(RepoPath repoPath, LocalRepo sourceLocalRepo) {
        return (sourceLocalRepo != null) && (!NamingUtils.isChecksum(repoPath.getPath()));
    }

    /**
     * Indicates whether the given input contains the '*' or the '?' wildcards
     *
     * @param userInput Input taken from the user
     * @return True if the input contains '*' or the '?' wildcards
     */
    protected boolean inputContainsWildCard(String userInput) {
        return !StringUtils.isBlank(userInput) && ((userInput.contains("*") || userInput.contains("?")));
    }

    /**
     * Handles unfound item exceptions
     *
     * @param re Thrown exception
     * @throws RepositoryException Will be rethrown if the given exception isn't of an "unfound" type
     */
    protected void handleNotFoundException(RepositoryException re) throws RepositoryException {
        Throwable notFound = ExceptionUtils.getCauseOfTypes(re, DataStoreRecordNotFoundException.class,
                PathNotFoundException.class, FileNotFoundException.class, ItemNotFoundException.class);
        if (notFound != null) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping unfound archive search result", re);
            } else {
                log.error("Skipping unfound archive search result: {}.", re.getMessage());
            }
        } else {
            throw re;
        }
    }

    /**
     * Performs the given JCR XPath query
     *
     * @param limitSearchResults True if search results should be limited
     * @param queryStr           Query
     * @return Query result
     */
    protected QueryResult performQuery(boolean limitSearchResults, String queryStr) {
        JcrQuerySpec spec = JcrQuerySpec.xpath(queryStr);
        if (!limitSearchResults) {
            spec.noLimit();
        }
        QueryResult queryResult = getJcrService().executeQuery(spec);
        return queryResult;
    }
}