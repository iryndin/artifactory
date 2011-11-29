/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.artifactory.api.search.ItemSearchResult;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.Searcher;
import org.artifactory.common.ConstantValues;
import org.artifactory.fs.ItemInfo;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.VfsDataService;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.search.InvalidQueryRuntimeException;
import org.artifactory.sapi.search.VfsQueryService;
import org.artifactory.sapi.search.VfsRepoQuery;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.util.ExceptionUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * @author yoavl
 */
public abstract class SearcherBase<C extends SearchControls, R extends ItemSearchResult> implements Searcher<C, R> {
    private static final Logger log = LoggerFactory.getLogger(SearcherBase.class);

    private final VfsDataService vfsDataService;
    private final VfsQueryService vfsQueryService;
    private final InternalRepositoryService repoService;

    //Cache this calculation (and don't make it a static...)
    private final int maxResults = ConstantValues.searchMaxResults.getInt() + 1;

    protected SearcherBase() {
        ArtifactoryContext context = ContextHelper.get();
        vfsDataService = context.beanForType(VfsDataService.class);
        vfsQueryService = context.beanForType(VfsQueryService.class);
        repoService = context.beanForType(InternalRepositoryService.class);
    }

    public final ItemSearchResults<R> search(C controls) {
        long start = System.currentTimeMillis();
        ItemSearchResults<R> results;
        try {
            results = doSearch(controls);
        } catch (TaskInterruptedException e) {
            throw e;
        } catch (Exception e) {
            //Handle bad queries
            @SuppressWarnings({"unchecked", "ThrowableResultOfMethodCallIgnored"})
            Throwable invalidQueryException = ExceptionUtils.getCauseOfTypes(e, InvalidQueryRuntimeException.class);
            if (invalidQueryException != null) {
                log.debug("Invalid query encountered.", e);
                throw (InvalidQueryRuntimeException) e;
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


    protected VfsRepoQuery createRepoQuery(SearchControls controls) {
        VfsRepoQuery query = getVfsQueryService().createRepoQuery();
        if (controls.isSpecificRepoSearch()) {
            query.setRepoKeys(controls.getSelectedRepoForSearch());
        } else {
            query.addAllSubPathFilter();
        }
        return query;
    }

    public abstract ItemSearchResults<R> doSearch(C controls);

    public InternalRepositoryService getRepoService() {
        return repoService;
    }

    public VfsQueryService getVfsQueryService() {
        return vfsQueryService;
    }

    public VfsDataService getVfsDataService() {
        return vfsDataService;
    }

    //We can use this method to monitor the actions of the Lucene StandardAnalyzer

    @SuppressWarnings({"UnusedDeclaration", "deprecation"})
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

    protected int getMaxResults() {
        return maxResults;
    }

    protected ItemInfo getProxyItemInfo(VfsNode artifactNode) {
        RepoPath repoPath = PathFactoryHolder.get().getRepoPath(artifactNode.absolutePath());
        switch (artifactNode.nodeType()) {
            case FILE:
                return VfsItemFactory.createFileInfoProxy(repoPath);
            case FOLDER:
                return VfsItemFactory.createFolderInfoProxy(repoPath);
            default:
                throw new RepositoryRuntimeException("Got a node " + repoPath + " which neither a file or folder");
        }
    }

    /**
     * Indicates whether the given result repo path is valid or not.<br> A repo path will stated as valid if the given
     * origin local repo is not null, the path is readable (permission and repo-configuration-wise) and if it is not a
     * checksum file.
     *
     * @param repoPath Repo path to validate
     * @return True if the repo path is valid
     */
    protected boolean isResultAcceptable(RepoPath repoPath) {
        if (repoPath == null) {
            return false;
        }
        LocalRepo localRepo = getRepoService().localOrCachedRepositoryByKey(repoPath.getRepoKey());
        return isResultAcceptable(repoPath, localRepo);
    }

    /**
     * Indicates whether the given result repo path is valid or not.<br> A repo path will stated as valid if the given
     * origin local repo is not null, the path is readable (permission and repo-configuration-wise) and if it is not a
     * checksum file.
     *
     * @param repoPath        Repo path to validate
     * @param sourceLocalRepo Source local repo to assert as valid
     * @return True if the repo path is valid
     */
    protected boolean isResultAcceptable(RepoPath repoPath, LocalRepo sourceLocalRepo) {
        return (sourceLocalRepo != null) && repoService.isRepoPathVisible(repoPath) &&
                (!NamingUtils.isChecksum(repoPath.getPath()));
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
}