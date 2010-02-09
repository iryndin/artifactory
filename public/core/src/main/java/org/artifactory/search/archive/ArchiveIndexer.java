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

package org.artifactory.search.archive;

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrZipFile;
import org.artifactory.log.LoggerFactory;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.LoggingUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * A utility for marking archives and indexing their content
 *
 * @author Noam Tenne
 */
public abstract class ArchiveIndexer {
    private static final Logger log = LoggerFactory.getLogger(ArchiveIndexer.class);

    /**
     * Indexes the content of the given JCR file
     *
     * @param file JcrFile to index
     */
    public static void index(JcrFile file) {
        //Index classes if necessary
        ContentType contentType = NamingUtils.getContentType(file.getPath());
        if (!contentType.isJarVariant()) {
            return;
        }

        //Read all classes and store their names on a
        JcrZipFile jar = null;
        try {
            jar = new JcrZipFile(file);

            @SuppressWarnings("unchecked")
            List<? extends ZipEntry> entries = jar.entries();
            StringBuilder sb = new StringBuilder();
            for (ZipEntry zipEntry : entries) {
                if (!zipEntry.isDirectory()) {
                    sb.append(zipEntry.getName()).append(" ");
                }
            }
            Node node = file.getNode();
            node.setProperty(JcrTypes.PROP_ARTIFACTORY_ARCHIVE_ENTRY, sb.toString(), PropertyType.STRING);
            //Mark the files as indexed
            node.setProperty(JcrTypes.PROP_ARTIFACTORY_ARCHIVE_INDEXED, true);
            node.save();
            log.info("The content of the archive: '{}' was indexed successfully", file.getName());
            log.debug("Indexed the classes: {}", sb.toString());
        } catch (Exception e) {
            LoggingUtils.warnOrDebug(log, "Could not index '" + file.getRepoPath() + "'", e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                }
                catch (Exception e) {
                    log.warn("Could not close jar file '" + file.getRepoPath() + "' properly.", e);
                }
            }
        }
    }

    /**
     * Scan all nodes below the given father node, and mark each jar variant with the "artifactory:archiveIndexed"
     * Property as false, for the archive indexing job to find it
     *
     * @param parentNode The node to start the archive search from
     * @param force      Force indexing upon archives
     * @throws javax.jcr.RepositoryException Any exception that might occur while dealing with the repository
     */
    public static void markArchivesForIndexing(Node parentNode, boolean force) throws RepositoryException {
        NodeIterator childrenNodes = parentNode.getNodes();
        while (childrenNodes.hasNext()) {
            Node archiveNode = childrenNodes.nextNode();
            ContentType contentType = NamingUtils.getContentType(archiveNode.getName());
            if (contentType.isJarVariant()) {
                markArchiveForIndexing(archiveNode, force);
            }
            if (archiveNode.hasNodes()) {
                markArchivesForIndexing(archiveNode, force);
                archiveNode.save();
            }
        }
    }

    /**
     * Flags the given archive for indexing
     *
     * @param archiveNode Archive node to mark
     * @param force       True if should force marking
     * @return True if successfully marked
     * @throws RepositoryException Any exception that might occur during node editing
     */
    public static boolean markArchiveForIndexing(Node archiveNode, boolean force) throws RepositoryException {
        String nodeType = archiveNode.getPrimaryNodeType().getName();
        if (nodeType.equals(JcrTypes.NT_ARTIFACTORY_FILE)) {
            if (force || !archiveNode.hasProperty(JcrTypes.PROP_ARTIFACTORY_ARCHIVE_INDEXED)) {
                archiveNode.setProperty(JcrTypes.PROP_ARTIFACTORY_ARCHIVE_INDEXED, false);
                log.debug("The archive: '{}' was successfully marked for indexing", archiveNode.getName());
                return true;
            }
        }
        return false;
    }

    /**
     * Scans the database for all files marked for indexing, and passes them on to the indexer
     */
    public static void indexMarkedArchives() {
        //Find all files marked for indexing
        InternalArtifactoryContext context = InternalContextHelper.get();
        JcrSession usession = context.getJcrService().getUnmanagedSession();
        List<RepoPath> archiveRepoPaths = new ArrayList<RepoPath>();
        try {
            /*RowIterator rowIterator = GQL.execute(
                    "type:" + JcrFile.NT_ARTIFACTORY_FILE + " \"" + PROP_ARTIFACTORY_ARCHIVE_INDEXED + "\":false",
                    usession);*/
            String queryStr = "/jcr:root" + JcrPath.get().getRepoJcrRootPath() + "//element(*, " +
                    JcrTypes.NT_ARTIFACTORY_FILE + ")[@" + JcrTypes.PROP_ARTIFACTORY_ARCHIVE_INDEXED +
                    "= false()]";
            QueryManager queryManager = usession.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryStr, Query.XPATH);
            QueryResult queryResult = query.execute();
            NodeIterator nodes = queryResult.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String path = node.getPath();
                if (!path.startsWith(JcrPath.get().getRepoJcrRootPath())) {
                    log.warn("Archive in path: '{}' is not located in the repositories branch, and has been excluded" +
                            " from indexing.", path);
                    continue;
                }
                RepoPath repoPath = JcrPath.get().getRepoPath(path);
                archiveRepoPaths.add(repoPath);
            }
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Could not index marked archives", e);
        } finally {
            usession.logout();
        }
        //Schedule the files for indexing
        InternalSearchService searchService = context.beanForType(InternalSearchService.class);
        log.debug("Scheduling indexing for marked archives.");
        searchService.index(archiveRepoPaths);
    }
}