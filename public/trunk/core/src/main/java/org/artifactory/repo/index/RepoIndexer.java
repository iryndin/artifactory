/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
/*
 * Additional contributors:
 *    JFrog Ltd.
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
import org.artifactory.repo.index.creator.JcrMavenArchetypeArtifactInfoIndexCreator;
import org.artifactory.repo.index.creator.JcrMavenPluginArtifactInfoIndexCreator;
import org.artifactory.repo.index.creator.JcrMinimalArtifactInfoIndexCreator;
import org.artifactory.repo.index.locator.MetadataLocator;
import org.artifactory.repo.index.locator.PomLocator;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.resource.RepoResource;
import org.artifactory.util.FileUtils;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.Logger;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactScanningListener;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.DefaultIndexerEngine;
import org.sonatype.nexus.index.DefaultNexusIndexer;
import org.sonatype.nexus.index.DefaultQueryCreator;
import org.sonatype.nexus.index.DefaultScanner;
import org.sonatype.nexus.index.DefaultSearchEngine;
import org.sonatype.nexus.index.ScanningResult;
import org.sonatype.nexus.index.context.IndexCreator;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.incremental.DefaultIncrementalHandler;
import org.sonatype.nexus.index.incremental.IncrementalHandler;
import org.sonatype.nexus.index.packer.DefaultIndexPacker;
import org.sonatype.nexus.index.packer.IndexPacker;
import org.sonatype.nexus.index.packer.IndexPackingRequest;
import org.sonatype.nexus.index.updater.DefaultIndexUpdater;
import org.springframework.security.util.FieldUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author yoavl
 * @author yossis
 */
class RepoIndexer extends DefaultNexusIndexer implements ArtifactScanningListener {
    private static final Logger log = LoggerFactory.getLogger(RepoIndexer.class);

    private StoringRepo repo;
    private IndexingContext context;
    private IndexPacker packer;

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
        //packer
        IncrementalHandler incrementalHandler = new DefaultIncrementalHandler();
        FieldUtils.setProtectedFieldValue("logger", incrementalHandler,
                new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_INFO, "console"));
        packer = new DefaultIndexPacker();
        FieldUtils.setProtectedFieldValue("incrementalHandler", packer, incrementalHandler);
        FieldUtils.setProtectedFieldValue("logger", packer,
                new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_WARN, "console"));
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
            ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
            File outputFolder = FileUtils.createRandomDir(artifactoryHome.getWorkTmpDir(),
                    "artifactory.index." + repo.getKey());
            outputFolder.deleteOnExit();
            IndexPackingRequest request = newIndexPackingRequest(outputFolder);
            //Pack - will create the index files inside the folder
            packer.packIndex(request);
            //Return the handle to the zip file (will be remove when the handle is closed)
            File tmpGz = new File(outputFolder, MavenNaming.NEXUS_INDEX_GZ);
            if (!tmpGz.exists()) {
                throw new RuntimeException("Temp index file '" + tmpGz.getAbsolutePath() + "' does not exist.");
            }
            TempFileStreamHandle zipIndexHandle = new TempFileStreamHandle(tmpGz);
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

    void mergeInto(StoringRepo localRepo, Map<StoringRepo, FSDirectory> extractedRepoIndexes) throws Exception {
        FSDirectory repoToMergeIndexDir = getIndexDir(localRepo, extractedRepoIndexes);
        if (repoToMergeIndexDir == null) {
            //No local index exists
            return;
        }
        //Merge the provided index into the repo-specific temp index dir
        try {
            log.debug("Merging local index of {} into {}.", localRepo, repo);
            context.merge(repoToMergeIndexDir);
            /*IndexWriter indexWriter = context.getIndexWriter();
            indexWriter.addIndexes(new Directory[]{indexDir});
            indexWriter.close();*/
        } catch (FileNotFoundException e) {
            //Merged-into directory is new - just copy instead of merging
            log.debug("Target index directory is new: merging is skipped.");
        }
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
        List<IndexCreator> indexCreators = new ArrayList<IndexCreator>(4);
        indexCreators.add(new JcrMinimalArtifactInfoIndexCreator(repo));
        indexCreators.add(new JcrJarFileContentsIndexCreator());
        indexCreators.add(new JcrMavenPluginArtifactInfoIndexCreator());
        indexCreators.add(new JcrMavenArchetypeArtifactInfoIndexCreator());
        context = addIndexingContextForced(repoKey, repoKey, repoDir, indexDir, null, null, indexCreators);
    }

    private IndexPackingRequest newIndexPackingRequest(File outputFolder) {
        IndexPackingRequest request = new IndexPackingRequest(context, outputFolder);
        request.setCreateChecksumFiles(false);
        request.setCreateIncrementalChunks(false);
        //create new index format
        request.setFormats(Arrays.asList(/*IndexPackingRequest.IndexFormat.FORMAT_LEGACY,*/
                IndexPackingRequest.IndexFormat.FORMAT_V1));
        return request;
    }

    private FSDirectory getIndexDir(StoringRepo repo, Map<StoringRepo, FSDirectory> extractedRepoIndexes)
            throws Exception {
        //Check if need to extract the local index first
        FSDirectory indexDir = extractedRepoIndexes.get(repo);
        if (indexDir == null) {
            //Extraction required
            RepoResource indexRes = repo.getInfo(new NullRequestContext(MavenNaming.NEXUS_INDEX_GZ_PATH));
            if (!indexRes.isFound()) {
                log.debug("Cannot find index resource for repository {}", repo);
                return null;
            }
            //Copy the index file
            ResourceStreamHandle handle = repo.getResourceStreamHandle(indexRes);
            try {
                ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
                File indexUnzippedDir = FileUtils.createRandomDir(artifactoryHome.getWorkTmpDir(),
                        "artifactory.merged-index." + repo.getKey());
                indexUnzippedDir.deleteOnExit();
                indexDir = FSDirectory.getDirectory(indexUnzippedDir);
                //Get the extracted lucene dir
                DefaultIndexUpdater.unpackIndexData(handle.getInputStream(), indexDir, context);
            } finally {
                handle.close();
            }
            //We can release the lock on the zip file (or expanding the repo root will block)
            LockingHelper.removeLockEntry(new RepoPath(repo.getKey(), MavenNaming.NEXUS_INDEX_GZ_PATH));
            //Remember the extracted index
            extractedRepoIndexes.put(repo, indexDir);
        }
        return indexDir;
    }
}