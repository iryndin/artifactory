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

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.archive.ArchiveSearchControls;
import org.artifactory.api.search.archive.ArchiveSearchResult;
import org.artifactory.api.search.artifact.ArtifactSearchControls;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.MetadataSearchResult;
import org.artifactory.api.search.metadata.pom.PomSearchControls;
import org.artifactory.api.search.metadata.pom.PomSearchResult;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.util.Pair;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.version.JcrVersion;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.search.archive.ArchiveIndexer;
import org.artifactory.search.archive.ArchiveSearcher;
import org.artifactory.search.gavc.GavcSearcher;
import org.artifactory.search.metadata.MetadataSearcher;
import org.artifactory.search.metadata.pom.PomSearcher;
import org.artifactory.search.property.PropertySearcher;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * @author Frederic Simon
 * @author Yoav Landman
 */
@Service
public class SearchServiceImpl implements InternalSearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private AuthorizationService authService;

    private InternalArtifactoryContext context;

    private JcrVersion originalVersion;

    @PostConstruct
    public void register() {
        context.addReloadableBean(InternalSearchService.class);
    }

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    public SearchResults<ArtifactSearchResult> searchArtifacts(ArtifactSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<ArtifactSearchResult>(new ArrayList<ArtifactSearchResult>());
        }
        ArtifactSearcher searcher = new ArtifactSearcher();
        SearchResults<ArtifactSearchResult> results = searcher.search(controls);
        return results;

    }

    public SearchResults<ArchiveSearchResult> searchArchiveContent(ArchiveSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<ArchiveSearchResult>(new ArrayList<ArchiveSearchResult>());
        }
        ArchiveSearcher searcher = new ArchiveSearcher();
        SearchResults<ArchiveSearchResult> results = searcher.search(controls);
        return results;
    }

    public SearchResults<MetadataSearchResult> searchMetadata(MetadataSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<MetadataSearchResult>(new ArrayList<MetadataSearchResult>());
        }
        MetadataSearcher searcher = new MetadataSearcher();
        SearchResults<MetadataSearchResult> results = searcher.search(controls);
        return results;
    }

    public SearchResults<GavcSearchResult> searchGavc(GavcSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<GavcSearchResult>(new ArrayList<GavcSearchResult>());
        }

        GavcSearcher searcher = new GavcSearcher();
        SearchResults<GavcSearchResult> results = searcher.search(controls);

        return results;
    }

    public SearchResults<PomSearchResult> searchPomContent(MetadataSearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<PomSearchResult>(new ArrayList<PomSearchResult>());
        }

        PomSearcher searcher = new PomSearcher();
        SearchResults<PomSearchResult> results = searcher.search(controls);

        return results;
    }

    public SearchResults<PropertySearchResult> searchProperty(PropertySearchControls controls) {
        if (shouldReturnEmptyResults(controls)) {
            return new SearchResults<PropertySearchResult>(new ArrayList<PropertySearchResult>());
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

        String formattedFrom = ISODateTimeFormat.dateTime().print(from.getTimeInMillis());
        String formattedTo = ISODateTimeFormat.dateTime().print(to.getTimeInMillis());
        try {
            // all artifactory files that were created or modified after input date
            String xpath = String.format(
                    "/jcr:root/repositories//*/artifactory:metadata/artifactory-file-ext[" +
                            "(@artifactory:created > xs:dateTime('%s') or @artifactory:lastModified > xs:dateTime('%s')) " +
                            "and " +
                            "(@artifactory:created <= xs:dateTime('%s') or @artifactory:lastModified <= xs:dateTime('%s'))" +
                            "]", formattedFrom, formattedFrom, formattedTo, formattedTo);

            QueryResult resultXpath = jcrService.executeXpathQuery(xpath);
            NodeIterator nodeIterator = resultXpath.getNodes();
            List<Pair<RepoPath, Calendar>> result = new ArrayList<Pair<RepoPath, Calendar>>();
            while (nodeIterator.hasNext()) {
                Node fileExtraInfoNode = (Node) nodeIterator.next();
                Calendar modified = fileExtraInfoNode.getProperty("artifactory:created").getValue().getDate();
                if (!(modified.after(from) && modified.before(to) || modified.equals(to))) {
                    // if created not in range then the last modified is
                    modified = fileExtraInfoNode.getProperty("artifactory:lastModified").getValue().getDate();
                }
                Node fileNode = fileExtraInfoNode.getParent().getParent();
                RepoPath repoPath = JcrPath.get().getRepoPath(fileNode.getPath());
                result.add(new Pair<RepoPath, Calendar>(repoPath, modified));
            }
            return result;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public QueryResult searchPomInPath(RepoPath repoPath) throws RepositoryException {
        PomSearcher searcher = new PomSearcher();
        QueryResult result = searcher.searchForDeployableUnits(new PomSearchControls(repoPath));
        return result;
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
        } else if (originalVersion != null) {
            originalVersion.convertIndexes(this);
        }
        //Index archives marked for indexing (might have left overs from abrupt shutdown after deploy)
        getAdvisedMe().indexMarkedArchives();
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{MetadataService.class, InternalRepositoryService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        JcrVersion.values();
        //We cannot convert the indexes staright away since the JCR will initialize and close the sssion on us,
        //so we just mark and will do this on init
        originalVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);
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
     * @param archiveRepoPath Repo path of the archive to mark
     * @return boolean - Was archive marked
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean markArchiveForIndexing(RepoPath archiveRepoPath, boolean force) {
        StoringRepo repo = repoService.storingRepositoryByKey(archiveRepoPath.getRepoKey());
        JcrFile jcrFile = repo.getJcrFile(archiveRepoPath);
        Node archiveNode = jcrFile.getNode();
        String name = jcrFile.getName();
        ContentType contentType = NamingUtils.getContentType(name);
        if (contentType.isJarVariant()) {
            try {
                return ArchiveIndexer.markArchiveForIndexing(archiveNode, force);
            } catch (RepositoryException e) {
                log.warn("Could not mark the archive '" + archiveRepoPath + "' for indexing.", e);
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
        StoringRepo repo = repoService.storingRepositoryByKey(archiveRepoPath.getRepoKey());
        JcrFsItem item = repo.getLockedJcrFsItem(archiveRepoPath);
        if ((item != null) && item.isFile()) {
            ArchiveIndexer.index((JcrFile) item);
        } else {
            log.debug("Skipping archive indexing for {} - item is non-existing or not a file.", archiveRepoPath);
        }
    }

    public void asyncIndex(RepoPath archiveRepoPath) {
        index(archiveRepoPath);
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