/*
 * This file is part of Artifactory.
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

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
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
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.ExceptionUtils;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import java.io.IOException;
import java.io.StringReader;
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
                log.debug("Invalid query encountered: \n{}", e.getMessage());
                throw new RepositoryRuntimeException("Invalid query.");
            } else {
                log.error("Could not perform search.", e);
                throw new RepositoryRuntimeException("Could not execute search query (" + e.getMessage() + ").");
            }
        }
        long time = System.currentTimeMillis() - start;
        results.setTime(time);
        log.debug("Total search time: {} ms", time);
        return results;
    }

    protected StringBuilder getPathQueryBuilder(SearchControlsBase controls) {
        StringBuilder queryBuilder = new StringBuilder("/jcr:root");
        queryBuilder.append(JcrPath.get().getRepoJcrRootPath()).append(FORWARD_SLASH);
        addRepoToQuery(controls, queryBuilder);
        return queryBuilder;
    }

    //Add specific repositories to search from. If list is empty query will search all repos
    private void addRepoToQuery(SearchControlsBase controls, StringBuilder queryBuilder) {
        if (controls.isSpecificRepoSearch()) {
            List<String> repoKeys = controls.getSelectedRepoForSearch();
            queryBuilder.append(".[");
            for (String repoKey : repoKeys) {
                queryBuilder.append("fn:name() ='");
                queryBuilder.append(repoKey);
                queryBuilder.append("' or ");
            }
            int builderCurentLength = queryBuilder.length();
            queryBuilder.replace(builderCurentLength - 4, builderCurentLength, "]/");
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

    protected static String stringToJcrSearchExp(String expression) {
        String escapedEpression = escapeToJcrLikeString(expression);
        // now wrap with % to allow searching all artifacts that contains the expression
        String result = "'%" + escapedEpression + "%'";
        return result;
    }

    /**
     * Convert a string to a JCR search expression literal, suitable for use in jcr:like() (inside XPath queries). The
     * characters -, ', " and \ have special meaning, and must be escaped with a backslash to obtain their literal
     * value. This method doesn't escape the characters '%' and '_' to allow users to use wildcards. The characters '*'
     * and '?' are converted to '%' or '_' respectively to allow using both types of wildcards. See JSR-170 spec v1.0,
     * Sec. 6.6.5.1.
     *
     * @param expression A string to escape.
     * @return A valid XPath 2.0 string literal suitable for use in jcr:like(), including enclosing '%'.
     */
    protected static String escapeToJcrLikeString(String expression) {
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

    /**
     * Escapes expressions meant for the JCR contains function. Differes in some ways from the escaping of the JCR like
     * function, for example: JCR contains' wildcard is '*', while JCR like's wildcard is '%'
     *
     * @param s Expression to escape
     * @return String - Escaped expression
     */
    protected static String escapeToJcrContainsString(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-' || c == '\'' || c == '\"' || c == '\\') {
                sb.append('\\');
            }
            if (c == '%') {
                c = '*';
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
     * @return Element[] - fragemets
     */
    @SuppressWarnings({"unchecked"})
    protected Element[] getFragmentElements(String excerpt) {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        Document document = null;
        try {
            document = builder.build(new StringReader(excerpt));
        } catch (Exception e) {
            log.error("An error has occured while parsing the search result excerpt", e);
        }
        Element element = null;
        if (document != null) {
            element = document.getRootElement();
        }
        List<Element> fragments = element.getChildren("fragment");
        return fragments.toArray(new Element[fragments.size()]);
    }

    /**
     * Returns the begining of the path (if any) before a highlighted element in a list of Element contents.
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
                String elmentValue = element.getValue();

                //Paths are seperated by spaces, so we should stop at the last space occurance
                int lastSpace = elmentValue.lastIndexOf(" ");
                if (lastSpace < 0) {
                    path.insert(0, elmentValue);
                } else {
                    String pathPiece = elmentValue.substring(lastSpace + 1);
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
                String elmentValue = element.getValue();

                //Paths are seperated by spaces, so we should stop at the next space occurance
                int firstSpace = elmentValue.indexOf(" ");
                if (firstSpace < 0) {
                    path.append(elmentValue);
                } else {
                    String pathPiece = elmentValue.substring(0, firstSpace);
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
}