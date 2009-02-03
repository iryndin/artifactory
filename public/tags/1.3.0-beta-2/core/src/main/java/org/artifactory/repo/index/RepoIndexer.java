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

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.index.creator.JcrMinimalArtifactInfoIndexCreator;
import org.artifactory.repo.index.locator.ArtifactLocator;
import org.artifactory.repo.index.locator.MetadataLocator;
import org.artifactory.repo.index.locator.PomLocator;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.ClassUtils;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.DefaultNexusIndexer;
import org.sonatype.nexus.index.IndexUtils;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.creator.DefaultIndexerEngine;
import org.sonatype.nexus.index.creator.IndexCreator;
import org.sonatype.nexus.index.creator.IndexerEngine;
import org.sonatype.nexus.index.locator.Locator;
import org.sonatype.nexus.index.scan.DefaultScanner;
import org.sonatype.nexus.index.scan.Scanner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class RepoIndexer extends DefaultNexusIndexer {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RepoIndexer.class);

    private LocalRepo repo;
    private IndexingContext context;

    @SuppressWarnings({"ThisEscapedInObjectConstruction"})
    public RepoIndexer(LocalRepo repo) {
        this.repo = repo;
        //Unplexus
        ClassUtils.setAccessibleField(
                "indexer", IndexerEngine.class, this, new DefaultIndexerEngine());
        DefaultScanner scanner = new DefaultScanner();
        DefaultArtifactContextProducer artifactContextProducer =
                new DefaultArtifactContextProducer();
        ClassUtils.setAccessibleField("artifactContextProducer", ArtifactContextProducer.class,
                scanner, artifactContextProducer);
        ClassUtils.setAccessibleField(
                "al", Locator.class, artifactContextProducer, new ArtifactLocator());
        ClassUtils.setAccessibleField(
                "pl", Locator.class, artifactContextProducer, new PomLocator());
        ClassUtils.setAccessibleField(
                "ml", Locator.class, artifactContextProducer, new MetadataLocator());
        ClassUtils.setAccessibleField("scanner", Scanner.class, this, scanner);
    }

    @SuppressWarnings({"UnusedDeclaration", "UnnecessaryLocalVariable"})
    public ResourceStreamHandle index(Date fireTime) throws Exception {
        JcrWrapper jcr = ContextHelper.get().getJcr();
        String repoKey = repo.getKey();
        JcrFolder repoDir = repo.getFolder();
        Directory indexDir = new RAMDirectory();
        List<IndexCreator> indexCreators = new ArrayList<IndexCreator>(1);
        indexCreators.add(new JcrMinimalArtifactInfoIndexCreator());
        //indexCreators.add(new JcrJarFileContentsIndexCreator());
        try {
            context = addIndexingContext(
                    repoKey, repoKey, repoDir, indexDir, repo.getUrl(), null, indexCreators, false);
            scan(context);
            //Create the index and return its inputStream
            //TODO: [by yl] Find a better way to do this other than in-memory
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IndexUtils.packIndexArchive(context, os);
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            ResourceStreamHandle handle = new SimpleResourceStreamHandle(is);
            return handle;
        } catch (Exception e) {
            throw new RuntimeException("Indexing failed.", e);
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public ResourceStreamHandle getProperties() {
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