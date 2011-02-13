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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.api.search.deployable.VersionUnitSearchControls;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.search.xml.XmlSearchResult;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.search.xml.metadata.MetadataSearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.util.SerializablePair;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RemoteRepoBase;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.search.archive.ArchiveIndexer;
import org.artifactory.search.archive.ArchiveSearcher;
import org.artifactory.search.build.BuildSearcher;
import org.artifactory.search.deployable.VersionUnitSearcher;
import org.artifactory.search.gavc.GavcSearcher;
import org.artifactory.search.property.PropertySearcher;
import org.artifactory.search.version.SearchVersion;
import org.artifactory.search.xml.XmlFileSearcher;
import org.artifactory.search.xml.metadata.MetadataSearcher;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.PathMatcher;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Frederic Simon
 * @author Yoav Landman
 */
@Service
@Reloadable(beanClass = InternalSearchService.class, initAfter = {InternalRepositoryService.class})
public class SearchServiceImpl implements InternalSearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private RepositoryBrowsingService repoBrowsingService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private CachedThreadPoolTaskExecutor executor;

    private InternalArtifactoryContext context;

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    public SearchResults<ArtifactSearchResult> searchArtifacts(ArtifactSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<ArtifactSearchResult>(Lists.<ArtifactSearchResult>newArrayList());
        }
        ArtifactSearcher searcher = new ArtifactSearcher();
        SearchResults<ArtifactSearchResult> results = searcher.search(controls);
        return results;
    }

    public Set<RepoPath> searchArtifactsByChecksum(ChecksumSearchControls searchControls) {
        ArtifactSearcher searcher = new ArtifactSearcher();
        try {
            return searcher.searchArtifactsByChecksum(searchControls);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public SearchResults<ArchiveSearchResult> searchArchiveContent(ArchiveSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<ArchiveSearchResult>(Lists.<ArchiveSearchResult>newArrayList());
        }
        ArchiveSearcher searcher = new ArchiveSearcher();
        SearchResults<ArchiveSearchResult> results = searcher.search(controls);
        return results;
    }

    public SearchResults<MetadataSearchResult> searchMetadata(MetadataSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<MetadataSearchResult>(Lists.<MetadataSearchResult>newArrayList());
        }
        MetadataSearcher searcher = new MetadataSearcher();
        SearchResults<MetadataSearchResult> results = searcher.search(controls);
        return results;
    }

    public <T> SearchResults<GenericMetadataSearchResult<T>> searchGenericMetadata(
            GenericMetadataSearchControls<T> controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<GenericMetadataSearchResult<T>>(
                    Lists.<GenericMetadataSearchResult<T>>newArrayList());
        }
        MetadataDefinitionService mdService = context.beanForType(MetadataDefinitionService.class);
        MetadataDefinition<T> definition = mdService.getMetadataDefinition(controls.getMetadataClass());
        SearcherBase<GenericMetadataSearchControls<T>, GenericMetadataSearchResult<T>> searcher =
                definition.getSearcher();
        return searcher.search(controls);
    }

    public SearchResults<GavcSearchResult> searchGavc(GavcSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<GavcSearchResult>(Lists.<GavcSearchResult>newArrayList());
        }

        GavcSearcher searcher = new GavcSearcher();
        SearchResults<GavcSearchResult> results = searcher.search(controls);

        return results;
    }

    public SearchResults<XmlSearchResult> searchXmlContent(MetadataSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<XmlSearchResult>(Lists.<XmlSearchResult>newArrayList());
        }

        XmlFileSearcher searcher = new XmlFileSearcher();
        SearchResults<XmlSearchResult> results = searcher.search(controls);

        return results;
    }

    public SearchResults<PropertySearchResult> searchProperty(PropertySearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<PropertySearchResult>(Lists.<PropertySearchResult>newArrayList());
        }

        PropertySearcher searcher = new PropertySearcher();
        SearchResults<PropertySearchResult> results = searcher.search(controls);

        return results;
    }

    public List<SerializablePair<RepoPath, Calendar>> searchArtifactsCreatedOrModifiedInRange(Calendar from,
            Calendar to,
            List<String> reposToSearch) {
        if (from == null && to == null) {
            return Collections.emptyList();
        } else if (from == null) {
            from = Calendar.getInstance();
            from.setTimeInMillis(0);    // 1st Jan 1970
        } else if (to == null) {
            to = Calendar.getInstance();    // now
        }

        try {
            // all artifactory files that were created or modified after input date
            StringBuilder builder = new StringBuilder("/");
            addReposToQueryBuilder(reposToSearch, builder);
            builder.append("/element(*, ").append(JcrTypes.NT_ARTIFACTORY_FILE).append(") [(@").
                    append(JcrTypes.PROP_ARTIFACTORY_CREATED).append(" > xs:dateTime('").append(ISO8601.format(from)).
                    append("') or @").append(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED).append(" > xs:dateTime('").
                    append(ISO8601.format(from)).append("') ) and (@").append(JcrTypes.PROP_ARTIFACTORY_CREATED).
                    append(" <= xs:dateTime('").append(ISO8601.format(to)).append("') or @").
                    append(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED).append(" <= xs:dateTime('").
                    append(ISO8601.format(to)).append("') )]");

            QueryResult resultXpath = jcrService.executeQuery(JcrQuerySpec.xpath(builder.toString()).noLimit());
            NodeIterator nodeIterator = resultXpath.getNodes();
            List<SerializablePair<RepoPath, Calendar>> result = Lists.newArrayList();
            while (nodeIterator.hasNext()) {
                Node fileNode = (Node) nodeIterator.next();
                Calendar modified = fileNode.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getValue().getDate();
                if (!(modified.after(from) && modified.before(to) || modified.equals(to))) {
                    // if created not in range then the last modified is
                    modified = fileNode.getProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED).getValue().getDate();
                }
                RepoPath repoPath = JcrPath.get().getRepoPath(fileNode.getPath());
                if (!isRangeResultValid(repoPath, reposToSearch)) {
                    continue;
                }

                result.add(new SerializablePair<RepoPath, Calendar>(repoPath, modified));
            }
            return result;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public SearchResults<VersionUnitSearchResult> searchVersionUnits(VersionUnitSearchControls controls)
            throws RepositoryException {
        VersionUnitSearcher searcher = new VersionUnitSearcher();
        return searcher.doSearch(controls);
    }

    public Set<BasicBuildInfo> getLatestBuildsByName() {
        BuildSearcher searcher = new BuildSearcher();
        try {
            return searcher.getLatestBuildsByName();
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public List<BasicBuildInfo> findBuildsByArtifactChecksum(String sha1, String md5) {
        BuildSearcher searcher = new BuildSearcher();
        try {
            return searcher.findBuildsByArtifactChecksum(sha1, md5);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public List<BasicBuildInfo> findBuildsByDependencyChecksum(String sha1, String md5) {
        BuildSearcher searcher = new BuildSearcher();
        try {
            return searcher.findBuildsByDependencyChecksum(sha1, md5);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public Set<String> searchArtifactsByPattern(String pattern) throws ExecutionException, InterruptedException,
            TimeoutException {
        if (StringUtils.isBlank(pattern)) {
            throw new IllegalArgumentException("Unable to search for an empty pattern");
        }

        pattern = pattern.trim();
        if (!pattern.contains(":")) {
            throw new IllegalArgumentException("Pattern must be formatted like [repo-key]:[pattern/to/search/for]");
        }

        if (pattern.contains("**")) {
            throw new IllegalArgumentException("Pattern cannot contain the '**' wildcard");
        }

        String[] patternTokens = StringUtils.split(pattern, ":", 2);
        String repoKey = patternTokens[0];

        final Repo repo = repoService.repositoryByKey(repoKey);

        if ((repo == null) || (patternTokens.length == 1) || (StringUtils.isBlank(patternTokens[1]))) {
            return Sets.newHashSet();
        }

        final String innerPattern = StringUtils.replace(patternTokens[1], "\\", "/");

        Callable<Set<String>> callable = new Callable<Set<String>>() {

            public Set<String> call() throws Exception {
                Set<String> pathsToReturn = Sets.newHashSet();
                List<String> patternFragments = Lists.newArrayList(StringUtils.split(innerPattern, "/"));

                if (repo.isReal()) {
                    String repoKey;
                    if (repo.isLocal() || repo.isCache()) {
                        repoKey = repo.getKey();
                    } else {
                        repoKey = ((RemoteRepoBase) repo).getLocalCacheRepo().getKey();
                    }
                    collectLocalRepoItemsRecursively(patternFragments, pathsToReturn, new RepoPathImpl(repoKey, ""));
                } else {
                    collectVirtualRepoItemsRecursively(patternFragments, pathsToReturn,
                            new RepoPathImpl(repo.getKey(), ""));
                }
                return pathsToReturn;
            }
        };

        Future<Set<String>> future = executor.submit(callable);
        return future.get(ConstantValues.searchPatternTimeoutSecs.getLong(), TimeUnit.SECONDS);
    }

    private boolean shouldReturnEmptyResults(SearchControls controls) {
        return checkUnauthorized() || controls.isEmpty();
    }

    private boolean checkUnauthorized() {
        boolean unauthorized =
                !authService.isAuthenticated() || (authService.isAnonymous() && !authService.isAnonAccessEnabled());
        if (unauthorized) {
            AccessLogger.unauthorizedSearch();
        }
        return unauthorized;
    }

    public void init() {
        if (ConstantValues.searchForceArchiveIndexing.getBoolean()) {
            log.info(ConstantValues.searchForceArchiveIndexing.getPropertyName() +
                    " is on: forcing archive indexes recalculation.");
            markArchivesForIndexing(true);
        }
        //Index archives marked for indexing (might have left overs from abrupt shutdown after deploy)
        getAdvisedMe().asyncIndexMarkedArchives();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        SearchVersion.values();
        //We cannot convert the indexes straight away since the JCR will initialize and close the session on us,
        //so we just mark and index on init
        SearchVersion originalVersion = source.getVersion().getSubConfigElementVersion(SearchVersion.class);
        originalVersion.convert(this);
    }

    public void transactionalIndexMarkedArchives() {
        JcrSession session = jcrService.getManagedSession();
        List<RepoPath> archiveRepoPaths = ArchiveIndexer.searchMarkedArchives(session);
        //Schedule the files for indexing
        log.debug("Scheduling indexing for marked archives.");
        index(archiveRepoPaths);
    }

    public void markArchivesForIndexing(boolean force) {
        markArchivesForIndexing(null, force);
    }

    /**
     * Marks all archives under the specified repo path for indexing
     *
     * @param searchPath Path to search under, search under root if null is passed
     * @param force      True if should force marking
     */
    public void markArchivesForIndexing(RepoPath searchPath, boolean force) {
        Session usession = context.getJcrService().getUnmanagedSession();
        try {
            //Scan all file to look for archives, and mark them for content indexing
            String path;
            if (searchPath != null) {
                path = JcrPath.get().getAbsolutePath(searchPath);
            } else {
                path = JcrPath.get().getRepoJcrRootPath();
            }
            Node rootNode = (Node) usession.getItem(path);
            ArchiveIndexer.markArchivesForIndexing(rootNode, force);
            log.info("Successfully marked archives under path: '{}' for indexing", path);
        } catch (RepositoryException e) {
            log.warn("Could not complete archive scanning for indexes calculation.", e);
        } finally {
            usession.logout();
        }
    }

    /**
     * Marks the archive specified in the given repo path for indexing
     *
     * @param newJcrFile
     * @return boolean - Was archive marked
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean markArchiveForIndexing(JcrFile newJcrFile, boolean force) {
        Node archiveNode = newJcrFile.getNode();
        String name = newJcrFile.getName();
        if (NamingUtils.isJarVariant(name)) {
            try {
                return ArchiveIndexer.markArchiveForIndexing(archiveNode, force);
            } catch (RepositoryException e) {
                log.warn("Could not mark the archive '" + newJcrFile + "' for indexing.", e);
            }
        }
        return false;
    }

    /**
     * Indexes all the archives that were marked
     */
    public void asyncIndexMarkedArchives() {
        JcrSession session = jcrService.getUnmanagedSession();
        try {
            List<RepoPath> archiveRepoPaths = ArchiveIndexer.searchMarkedArchives(session);
            //Schedule the files for indexing
            log.debug("Scheduling indexing for marked archives.");
            getAdvisedMe().index(archiveRepoPaths);
        } finally {
            session.save();
            session.logout();
        }
    }

    /**
     * Force indexing on all specified repo paths
     *
     * @param archiveRepoPaths Repo paths to index
     */
    public void index(List<RepoPath> archiveRepoPaths) {
        for (RepoPath repoPath : archiveRepoPaths) {
            getAdvisedMe().index(repoPath);
        }
    }

    public void index(RepoPath archiveRepoPath) {
        if (!NamingUtils.isJarVariant(archiveRepoPath.getPath())) {
            log.trace("Not indexing non jar variant path '{}' - with mime type '{}'.", archiveRepoPath,
                    NamingUtils.getMimeType(archiveRepoPath.getPath()));
            return;
        }

        StoringRepo repo = repoService.storingRepositoryByKey(archiveRepoPath.getRepoKey());
        if (repo == null) {
            log.debug("Skipping archive indexing for {} - repo does not exist.", archiveRepoPath.getRepoKey());
            return;
        }
        JcrFsItem item = repo.getLockedJcrFsItem(archiveRepoPath);
        if ((item != null) && item.isFile()) {
            ArchiveIndexer.index((JcrFile) item);
        } else {
            log.debug("Skipping archive indexing for {} - item does not exist or not a file.", archiveRepoPath);
        }
    }

    public void asyncIndex(RepoPath repoPath) {
        index(repoPath);
    }

    /**
     * Retrieves the Async advised instance of the service
     *
     * @return InternalSearchService - Async advised instance
     */
    private InternalSearchService getAdvisedMe() {
        return context.beanForType(InternalSearchService.class);
    }

    /**
     * Recursively collect items matching a given pattern from a local repo
     *
     * @param patternFragments Accepted pattern fragments
     * @param pathsToReturn    Result path aggregator
     * @param repoPath         Repo path to search at
     */
    private void collectLocalRepoItemsRecursively(List<String> patternFragments, Set<String> pathsToReturn,
            RepoPath repoPath) {

        org.artifactory.fs.ItemInfo itemInfo = repoService.getItemInfo(repoPath);

        if (!patternFragments.isEmpty()) {

            if (itemInfo.isFolder()) {

                String firstFragment = patternFragments.get(0);
                if (StringUtils.isBlank(firstFragment)) {
                    return;
                }

                for (String childName : repoService.getChildrenNames(repoPath)) {

                    if (patternMatches(firstFragment, childName)) {

                        List<String> fragmentsToPass = Lists.newArrayList(patternFragments);
                        fragmentsToPass.remove(0);
                        collectLocalRepoItemsRecursively(fragmentsToPass, pathsToReturn,
                                new RepoPathImpl(repoPath, childName));
                    }
                }
            }
        } else if (!itemInfo.isFolder()) {
            pathsToReturn.add(repoPath.getPath());
        }
    }

    /**
     * Recursively collect items matching a given pattern from a virtual repo
     *
     * @param patternFragments Accepted pattern fragments
     * @param pathsToReturn    Result path aggregator
     * @param repoPath         Repo path to search at
     */
    private void collectVirtualRepoItemsRecursively(List<String> patternFragments, Set<String> pathsToReturn,
            RepoPath repoPath) {

        VirtualRepoItem itemInfo = repoBrowsingService.getVirtualRepoItem(repoPath);

        if (!patternFragments.isEmpty()) {

            if (itemInfo.isFolder()) {

                String firstFragment = patternFragments.get(0);
                if (StringUtils.isBlank(firstFragment)) {
                    return;
                }
                //TODO: [by tc] should not use the remote children
                for (VirtualRepoItem child : repoBrowsingService.getVirtualRepoItems(repoPath)) {

                    if (patternMatches(firstFragment, child.getName())) {

                        List<String> fragmentsToPass = Lists.newArrayList(patternFragments);
                        fragmentsToPass.remove(0);
                        collectVirtualRepoItemsRecursively(fragmentsToPass, pathsToReturn,
                                new RepoPathImpl(repoPath, child.getName()));
                    }
                }
            }
        } else if (!itemInfo.isFolder()) {
            pathsToReturn.add(repoPath.getPath());
        }
    }

    /**
     * Checks if the given repo-relative path matches any of the given accepted patterns
     *
     * @param includePattern Accepted pattern
     * @param path           Repo-relative path to check
     * @return True if the path matches any of the patterns
     */
    private boolean patternMatches(String includePattern, String path) {
        return PathMatcher.matches(path, Lists.newArrayList(includePattern), PathMatcher.getDefaultExcludes());
    }

    /**
     * Appends the provided repo keys to the query builder
     *
     * @param repoKeys     Keys of specific repositories to search within
     * @param queryBuilder Query string builder
     */
    private void addReposToQueryBuilder(List<String> repoKeys, StringBuilder queryBuilder) {
        if ((repoKeys != null) && (!repoKeys.isEmpty())) {
            queryBuilder.append("jcr:root").append(JcrPath.get().getRepoJcrRootPath()).append("/").
                    append(". [");

            Iterator<String> iterator = repoKeys.iterator();
            while (iterator.hasNext()) {
                queryBuilder.append("fn:name() = '").append(iterator.next()).append("'");
                if (iterator.hasNext()) {
                    queryBuilder.append(" or ");
                }
            }

            queryBuilder.append("]/");
        }
    }

    /**
     * Indicates whether the range query result repo path is valid
     *
     * @param repoPath      Repo path of query result
     * @param reposToSearch Lists of repositories to search within
     * @return True if the repo path is valid and comes from a local repo
     */
    private boolean isRangeResultValid(RepoPath repoPath, List<String> reposToSearch) {
        if (repoPath == null) {
            return false;
        }
        if ((reposToSearch != null) && !reposToSearch.isEmpty()) {
            return true;
        }

        LocalRepo localRepo = repoService.localOrCachedRepositoryByKey(repoPath.getRepoKey());
        return (localRepo != null) && (!NamingUtils.isChecksum(repoPath.getPath()));
    }
}
