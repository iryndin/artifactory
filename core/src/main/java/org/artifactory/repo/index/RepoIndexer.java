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

package org.artifactory.repo.index;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.TempFileStreamHandle;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.index.creator.JcrJarFileContentsIndexCreator;
import org.artifactory.repo.index.creator.JcrMinimalArtifactInfoIndexCreator;
import org.artifactory.repo.index.locator.MetadataLocator;
import org.artifactory.repo.index.locator.PomLocator;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.resource.RepoResource;
import org.artifactory.util.FileUtils;
import org.artifactory.util.ZipUtils;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.Logger;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactScanningListener;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.DefaultNexusIndexer;
import org.sonatype.nexus.index.DefaultQueryCreator;
import org.sonatype.nexus.index.IndexUtils;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.creator.DefaultIndexerEngine;
import org.sonatype.nexus.index.creator.IndexCreator;
import org.sonatype.nexus.index.scan.DefaultScanner;
import org.sonatype.nexus.index.scan.ScanningResult;
import org.sonatype.nexus.index.search.DefaultSearchEngine;
import org.springframework.security.util.FieldUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author yoavl
 */
class RepoIndexer extends DefaultNexusIndexer implements ArtifactScanningListener {
    private static final Logger log = LoggerFactory.getLogger(RepoIndexer.class);

    private StoringRepo repo;
    private IndexingContext context;

    RepoIndexer(StoringRepo repo) {
        this.repo = repo;
        //Unplexus
        FieldUtils.setProtectedFieldValue("indexerEngine", this, new DefaultIndexerEngine());
        DefaultScanner scanner = new DefaultScanner();
        DefaultArtifactContextProducer artifactContextProducer = new DefaultArtifactContextProducer();
        FieldUtils.setProtectedFieldValue("pl", artifactContextProducer, new PomLocator());
        FieldUtils.setProtectedFieldValue("ml", artifactContextProducer, new MetadataLocator());
        FieldUtils.setProtectedFieldValue("artifactContextProducer", scanner, artifactContextProducer);
        FieldUtils.setProtectedFieldValue("scanner", this, scanner);
        DefaultQueryCreator queryCreator = new DefaultQueryCreator();
        FieldUtils.setProtectedFieldValue("logger", queryCreator,
                new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_INFO, "console"));
        FieldUtils.setProtectedFieldValue("queryCreator", this, queryCreator);
        FieldUtils.setProtectedFieldValue("searcher", this, new DefaultSearchEngine());
    }

    public void scanningStarted(IndexingContext ctx) {
    }

    public void scanningFinished(IndexingContext ctx, ScanningResult result) {
    }

    public void artifactError(ArtifactContext ac, Exception e) {
    }

    public void artifactDiscovered(ArtifactContext ac) {
        //Be nice with other threads
        Thread.yield();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    ResourceStreamHandle index(Date fireTime) throws Exception {
        //Use a file based dir with a temp file to conserve memory
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        // TODO: Should use the temp file of the repo
        File dir = FileUtils.createRandomDir(artifactoryHome.getWorkTmpDir(), "artifactory.index." + repo.getKey());
        Directory indexDir = FSDirectory.getDirectory(dir);
        try {
            return createIndex(indexDir, true);
        } catch (Exception e) {
            throw new RuntimeException("Indexing failed.", e);
        } finally {
            //Remove the temp index dir and files
            removeTempIndexFiles(dir);
        }
    }

    ResourceStreamHandle createIndex(Directory indexDir, boolean scan) throws IOException {
        OutputStream os = null;
        try {
            createContext(indexDir);
            context.updateTimestamp();
            if (scan) {
                //Update the dir content by scanning the repo
                scan(context, this);
            }
            //Create the index and return its inputStream
            //Zip to a temp file to aviod in-memory storage
            File tmpZip = File.createTempFile("artifactory.index." + repo.getKey() + ".", null);
            os = new BufferedOutputStream(new FileOutputStream(tmpZip));
            IndexUtils.packIndexArchive(context, os);
            os.close();
            //Return the handle to the zip file (will be remove when the handle is closed)
            TempFileStreamHandle zipIndexHandle = new TempFileStreamHandle(tmpZip);
            return zipIndexHandle;
        } catch (Exception e) {
            IOUtils.closeQuietly(os);
            throw new RuntimeException("Index creation failed.", e);
        }
    }

    ResourceStreamHandle getProperties() {
        Properties info = new Properties();
        info.setProperty(IndexingContext.INDEX_ID, context.getId());
        SimpleDateFormat df = new SimpleDateFormat(IndexingContext.INDEX_TIME_FORMAT);
        info.setProperty(IndexingContext.INDEX_TIMESTAMP, df.format(context.getTimestamp()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            info.store(os, null);
        } catch (IOException e) {
            throw new RuntimeException("Index properties calculation failed.", e);
        }
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        ResourceStreamHandle handle = new SimpleResourceStreamHandle(is, os.size());
        return handle;
    }

    void mergeInto(FSDirectory targetDirectory, StoringRepo repoToMerge,
            Map<StoringRepo, FSDirectory> extractedRepoIndexes) throws Exception {
        FSDirectory repoToMergeIndexDir = getIndexDir(repoToMerge, extractedRepoIndexes);
        if (repoToMergeIndexDir == null) {
            //No local index exists
            return;
        }
        //Merge the provided index into the repo-specific temp index dir
        try {
            log.debug("Merging index directory {} into {}.", repoToMergeIndexDir.getFile().getAbsolutePath(),
                    targetDirectory.getFile().getAbsolutePath());
            context.merge(repoToMergeIndexDir);
            /*IndexWriter indexWriter = context.getIndexWriter();
            indexWriter.addIndexes(new Directory[]{indexDir});
            indexWriter.close();*/
        } catch (FileNotFoundException e) {
            //Merged-into directory is new - just copy instead of merging
            log.debug("Target index directory is new: merging is skipped.");
        }
        //TODO: [by YS] the copy is necessary? the context.merge doing the merge, no?
        //Directory.copy(repoToMergeIndexDir, targetDirectory, true);
    }

    StoringRepo getRepo() {
        return repo;
    }

    void removeTempIndexFiles(File dir) {
        if (dir != null) {
            /**
             * Remove indexing context and delete the created files in a proper manner
             */
            try {
                removeIndexingContext(context, true);
            } catch (IOException e) {
                log.warn("Could not remove temporary index context '{}'.", context);
            }
            /**
             * We have to delete the index dir ourselves because the nexus removal
             *  tool deletes the files, but leaves the dir.
             */
            org.apache.commons.io.FileUtils.deleteQuietly(dir);
        }
    }

    void createContext(Directory indexDir) throws IOException, UnsupportedExistingLuceneIndexException {
        String repoKey = repo.getKey();
        JcrFolder repoDir = repo.getRootFolder();
        List<IndexCreator> indexCreators = new ArrayList<IndexCreator>(2);
        indexCreators.add(new JcrMinimalArtifactInfoIndexCreator(repo));
        indexCreators.add(new JcrJarFileContentsIndexCreator());
        context = addIndexingContextForced(repoKey, repoKey, repoDir, indexDir, null, null, indexCreators);
    }

    private static FSDirectory getIndexDir(StoringRepo repo, Map<StoringRepo, FSDirectory> extractedRepoIndexes)
            throws Exception {
        //Check if need to extract the local index first
        FSDirectory indexDir = extractedRepoIndexes.get(repo);
        if (indexDir == null) {
            RepoResource indexRes = repo.getInfo(new NullRequestContext(MavenNaming.NEXUS_INDEX_ZIP_PATH));
            if (!indexRes.isFound()) {
                log.debug("Cannot find index resource for repository {}", repo);
                return null;
            }
            //Copy the index file
            File indexZipFile;
            ResourceStreamHandle handle = repo.getResourceStreamHandle(indexRes);
            try {
                indexZipFile = File.createTempFile("index", null);
                indexZipFile.deleteOnExit();
                IOUtils.copy(handle.getInputStream(), new FileOutputStream(indexZipFile));
            } finally {
                handle.close();
            }
            //We can release the lock on the zip file (or expanding the repo root will block)
            LockingHelper.removeLockEntry(new RepoPath(repo.getKey(), MavenNaming.NEXUS_INDEX_ZIP_PATH));
            //Extract the index zip
            ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
            File indexUnzippedDir = FileUtils.createRandomDir(artifactoryHome.getWorkTmpDir(),
                    "artifactory.merged-index." + repo.getKey());
            indexUnzippedDir.deleteOnExit();
            ZipUtils.extract(indexZipFile, indexUnzippedDir);
            indexDir = FSDirectory.getDirectory(indexUnzippedDir);
            //Remember the extracted index
            extractedRepoIndexes.put(repo, indexDir);
        }
        return indexDir;
    }
}