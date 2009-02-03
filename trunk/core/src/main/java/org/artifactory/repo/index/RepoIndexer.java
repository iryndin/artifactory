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
package org.artifactory.repo.index;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.TempFileStreamHandle;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.index.creator.JcrMinimalArtifactInfoIndexCreator;
import org.artifactory.repo.index.locator.ExtensionBasedLocator;
import org.artifactory.repo.index.locator.MetadataLocator;
import org.artifactory.repo.index.locator.PomLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.ArtifactScanningListener;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.DefaultNexusIndexer;
import org.sonatype.nexus.index.IndexUtils;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.creator.DefaultIndexerEngine;
import org.sonatype.nexus.index.creator.IndexCreator;
import org.sonatype.nexus.index.scan.DefaultScanner;
import org.sonatype.nexus.index.scan.ScanningResult;
import org.springframework.security.util.FieldUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
class RepoIndexer extends DefaultNexusIndexer implements ArtifactScanningListener {
    private static final Logger log = LoggerFactory.getLogger(RepoIndexer.class);

    private LocalRepo repo;
    private IndexingContext context;

    RepoIndexer(LocalRepo repo) {
        this.repo = repo;
        //Unplexus
        FieldUtils.setProtectedFieldValue("indexer", this, new DefaultIndexerEngine());
        DefaultScanner scanner = new DefaultScanner();
        DefaultArtifactContextProducer artifactContextProducer =
                new DefaultArtifactContextProducer();
        FieldUtils.setProtectedFieldValue(
                "artifactContextProducer", scanner, artifactContextProducer);
        FieldUtils.setProtectedFieldValue("al", artifactContextProducer,
                new ExtensionBasedLocator(repo, ".jar"));
        FieldUtils.setProtectedFieldValue("pl", artifactContextProducer, new PomLocator(repo));
        FieldUtils.setProtectedFieldValue("ml", artifactContextProducer, new MetadataLocator(repo));
        FieldUtils.setProtectedFieldValue("scanner", this, scanner);
    }

    public void scanningStarted(IndexingContext ctx) {
    }

    public void scanningFinished(IndexingContext ctx, ScanningResult result) {
    }

    public void artifactError(ArtifactContext ac, Exception e) {
    }

    public void artifactDiscovered(ArtifactContext ac) {
        //TODO: [by yl] This callback doesn't happen
        //Be nice with other threads
        Thread.yield();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    ResourceStreamHandle index(Date fireTime) throws Exception {
        String repoKey = repo.getKey();
        JcrFolder repoDir = repo.getRootFolder();
        //Use a file based dir with a temp file to conserve memory
        File dir = org.artifactory.utils.FileUtils.createRandomDir(
                ArtifactoryHome.getTmpDir(), "artifactory.index." + repoKey + ".");
        Directory indexDir = FSDirectory.getDirectory(dir);
        List<IndexCreator> indexCreators = new ArrayList<IndexCreator>(1);
        indexCreators.add(new JcrMinimalArtifactInfoIndexCreator(repo));
        //indexCreators.add(new ;JcrJarFileContentsIndexCreator());
        OutputStream os = null;
        try {
            context = addIndexingContext(
                    repoKey, repoKey, repoDir, indexDir, repo.getUrl(), null, indexCreators, false);
            scan(context, this);
            //Create the index and return its inputStream
            //Zip to a temp file to aviod in-memory storage
            File tmpZip = File.createTempFile("artifactory.index." + repoKey + ".", null);
            os = new BufferedOutputStream(new FileOutputStream(tmpZip));
            IndexUtils.packIndexArchive(context, os);
            os.close();
            //Return the handle to the zip file (will be remove when the handle is closed)
            TempFileStreamHandle zipIndexHandle = new TempFileStreamHandle(tmpZip);
            return zipIndexHandle;
        } catch (Exception e) {
            IOUtils.closeQuietly(os);
            throw new RuntimeException("Indexing failed.", e);
        } finally {
            //Remove the temp index dir
            try {
                if (dir != null) {
                    /**
                     * Remove indexing context and delete the created files in a proper manner
                     */
                    removeIndexingContext(context, true);
                    /**
                     * We have to delete the index dir ourselves because the nexus removal
                     *  tool deletes the files, but leaves the dir.
                     */
                    org.apache.commons.io.FileUtils.deleteDirectory(dir);
                }
            } catch (IOException e) {
                log.warn("Failed to delete temporary index dir '" + dir.getPath() + "'.");
            }
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
        ResourceStreamHandle handle = new SimpleResourceStreamHandle(is);
        return handle;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void printDocs() throws IOException {
        IndexingContext context = getIndexingContexts().get("test");
        IndexReader reader = context.getIndexSearcher().getIndexReader();
        int numDocs = reader.numDocs();
        for (int i = 0; i < numDocs; i++) {
            Document doc = reader.document(i);
            System.err.println(i + " " + doc.get(ArtifactInfo.UINFO) + " : " +
                    doc.get(ArtifactInfo.PACKAGING));
        }
    }
}