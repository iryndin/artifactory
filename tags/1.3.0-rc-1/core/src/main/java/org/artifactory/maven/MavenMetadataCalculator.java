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
package org.artifactory.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.tx.SessionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Calculates the maven-metadata.xml for a folder
 *
 * @author yoavl
 */
public class MavenMetadataCalculator implements SessionResource {
    private static final Logger log = LoggerFactory.getLogger(MavenMetadataCalculator.class);

    LinkedHashSet<JcrFolder> mavenMetadataContainers = new LinkedHashSet<JcrFolder>();

    public void afterCompletion(boolean commit) {
    }

    public boolean hasResources() {
        return mavenMetadataContainers.size() > 0;
    }

    public boolean hasPendingChanges() {
        return hasResources();
    }

    public void onSessionSave() {
        //Update the metadata
        for (JcrFolder mavenMetadataContainer : mavenMetadataContainers) {
            recalculate(mavenMetadataContainer);
        }
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    public static void recalculate(JcrFolder container) {
        //For now do a releases-only naive impl - read the current md iterate on versions and update them
        String metdata = container.getXmlMetdata(MavenNaming.MAVEN_METADATA_NAME);
        //Sanity check
        if (metdata == null) {
            log.error("Cannot calculate maven-metadata for non-existing metadata on {}.", container);
        }
        try {
            MetadataXpp3Reader reader = new MetadataXpp3Reader();
            Metadata metadata = reader.read(new StringReader(metdata));
            //Recalc the versioning and version
            Versioning versioning = metadata.getVersioning();
            if (versioning == null) {
                log.debug("No versioning found for {}.", container);
                return;
            }
            List versions = versioning.getVersions();
            if (versions == null || versions.size() == 0) {
                log.debug("No versions found for {}.", container);
                return;
            }
            List<JcrFsItem> items = container.getItems();
            List<String> updatedVersions = new ArrayList<String>(items.size());
            String version = null;
            long lastUpdated = 0;
            for (JcrFsItem item : items) {
                if (item.isFolder()) {
                    String itemVersion = item.getName();
                    updatedVersions.add(itemVersion);
                    long itemLastModified = item.lastModified();
                    if (itemLastModified > lastUpdated) {
                        lastUpdated = itemLastModified;
                    }
                    if (version == null) {
                        //Update the single version entry
                        version = itemVersion;
                    }
                }
            }
            //Update
            boolean updated = false;
            /*if (version != null) {
                metadata.setVersion(version);
                updated = true;
            }*/
            if (lastUpdated > 0) {
                versioning.setLastUpdated(MavenUtils.dateToTimestamp(new Date(lastUpdated)));
                updated = true;
            }
            if (updatedVersions.size() > 0) {
                versioning.setVersions(updatedVersions);
                //Update the release
                versioning.setRelease(updatedVersions.get(updatedVersions.size() - 1));
                updated = true;
            }
            if (updated) {
                MetadataXpp3Writer writer = new MetadataXpp3Writer();
                StringWriter stringWriter = new StringWriter();
                writer.write(stringWriter, metadata);
                String newMetadata = stringWriter.toString();
                container.setXmlMetadata(MavenNaming.MAVEN_METADATA_NAME, newMetadata);
            } else {
                log.debug("No maven metadata upgrade required on {}.", container);
            }
        } catch (Exception e) {
            log.error("Failed to update the maven metadata on " + container + ".", e);
        }
    }

    /**
     * Calculate the maven metadata after a file change/deletion
     *
     * @param changedItem The item changed (or deleted)
     */
    public void addResource(JcrFsItem changedItem) {
        if (changedItem.isFolder()) {
            JcrFolder changedFolder = (JcrFolder) changedItem;
            if (mavenMetadataContainers.contains(changedFolder)) {
                //Do not process folders that have been changed/removed
                mavenMetadataContainers.remove(changedFolder);
            }
        }
        JcrFolder parentFolder = changedItem.getParentFolder();
        boolean hasMavenMetadata = parentFolder.hasXmlMetdata(MavenNaming.MAVEN_METADATA_NAME);
        if (hasMavenMetadata) {
            mavenMetadataContainers.add(parentFolder);
        }
    }
}