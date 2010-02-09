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

import com.google.common.collect.Lists;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.api.search.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.metadata.GenericMetadataSearchResult;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.api.search.metadata.pom.PomSearchControls;
import org.artifactory.api.search.metadata.pom.PomSearchResult;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.util.Pair;
import org.artifactory.build.api.Build;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.search.archive.ArchiveIndexer;
import org.artifactory.search.archive.ArchiveSearcher;
import org.artifactory.search.build.BuildSearcher;
import org.artifactory.search.gavc.GavcSearcher;
import org.artifactory.search.metadata.MetadataSearcher;
import org.artifactory.search.metadata.xml.XmlFileSearcher;
import org.artifactory.search.property.PropertySearcher;
import org.artifactory.search.version.SearchVersion;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.Reloadable;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Frederic Simon
 * @author Yoav Landman
 */
@Service
@Reloadable(beanClass = InternalSearchService.class,
        initAfter = {MetadataService.class, InternalRepositoryService.class})
public class SearchServiceImpl implements InternalSearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private AuthorizationService authService;

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

    public Set<RepoPath> searchArtifactsByChecksum(String sha1, String md5) {
        ArtifactSearcher searcher = new ArtifactSearcher();
        try {
            return searcher.searchArtifactsByChecksum(sha1, md5);
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

    public SearchResults<PomSearchResult> searchXmlContent(MetadataSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<PomSearchResult>(Lists.<PomSearchResult>newArrayList());
        }

        XmlFileSearcher searcher = new XmlFileSearcher();
        SearchResults<PomSearchResult> results = searcher.search(controls);

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

    public List<Pair<RepoPath, Calendar>> searchArtifactsCreatedOrModifiedInRange(Calendar from, Calendar to) {
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
            String queryStr =
                    "/jcr:root/repositories//element(*, " + JcrTypes.NT_ARTIFACTORY_FILE + ") " +
                            "[(@" + JcrTypes.PROP_ARTIFACTORY_CREATED + " > " + from.getTimeInMillis() +
                            " or @" + JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED + " > " + from.getTimeInMillis() + " ) " +
                            "and " +
                            "(@" + JcrTypes.PROP_ARTIFACTORY_CREATED + " <= " + to.getTimeInMillis() +
                            " or @" + JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED + " <= " + to.getTimeInMillis() + " )]";

            QueryResult resultXpath = jcrService.executeQuery(JcrQuerySpec.xpath(queryStr).noLimit());
            NodeIterator nodeIterator = resultXpath.getNodes();
            List<Pair<RepoPath, Calendar>> result = new ArrayList<Pair<RepoPath, Calendar>>();
            while (nodeIterator.hasNext()) {
                Node fileNode = (Node) nodeIterator.next();
                Calendar modified = fileNode.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getValue().getDate();
                if (!(modified.after(from) && modified.before(to) || modified.equals(to))) {
                    // if created not in range then the last modified is
                    modified = fileNode.getProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED).getValue().getDate();
                }
                RepoPath repoPath = JcrPath.get().getRepoPath(fileNode.getPath());
                result.add(new Pair<RepoPath, Calendar>(repoPath, modified));
            }
            return result;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public QueryResult searchPomInPath(RepoPath repoPath) throws RepositoryException {
        XmlFileSearcher searcher = new XmlFileSearcher();
        QueryResult result = searcher.searchForDeployableUnits(new PomSearchControls(repoPath));
        return result;
    }

    public List<Build> getLatestBuildsByName() {
        BuildSearcher searcher = new BuildSearcher();
        try {
            return searcher.getLatestBuildsByName();
        } catch (Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public List<Build> findBuildsByArtifactChecksum(String sha1, String md5) {
        BuildSearcher searcher = new BuildSearcher();
        try {
            return searcher.findBuildsByArtifactChecksum(sha1, md5);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public List<Build> findBuildsByDependencyChecksum(String sha1, String md5) {
        BuildSearcher searcher = new BuildSearcher();
        try {
            return searcher.findBuildsByDependencyChecksum(sha1, md5);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
        if (ConstantValues.forceArchiveIndexing.getBoolean()) {
            log.info(ConstantValues.forceArchiveIndexing.getPropertyName() +
                    " is on: forcing archive indexes recalculation.");
            markArchivesForIndexing(true);
        }
        //Index archives marked for indexing (might have left overs from abrupt shutdown after deploy)
        getAdvisedMe().indexMarkedArchives();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        SearchVersion.values();
        //We cannot convert the indexes staright away since the JCR will initialize and close the session on us,
        //so we just mark and index on init
        SearchVersion originalVersion = source.getVersion().getSubConfigElementVersion(SearchVersion.class);
        originalVersion.convert(this);
    }

    public void markArchivesForIndexing(boolean force) {
        markArchivesForIndexing(null, force);
    }

    /**
     * Marks all archives under the sepecified repo path for indexing
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
        ContentType contentType = NamingUtils.getContentType(name);
        if (contentType.isJarVariant()) {
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
    public void indexMarkedArchives() {
        ArchiveIndexer.indexMarkedArchives();
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
        ContentType contentType = NamingUtils.getContentType(archiveRepoPath.getPath());
        if (!contentType.isJarVariant()) {
            log.trace("Not indexing non jar variant path '{}' - with mime type '{}'.", archiveRepoPath, contentType);
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
}
