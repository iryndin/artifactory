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
package org.artifactory.update.md.current;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author freds
 * @date Nov 13, 2008
 */
public class MetadataReaderImpl implements MetadataReader {
    private static final Logger log = LoggerFactory.getLogger(MetadataReaderImpl.class);

    public List<MetadataEntry> getMetadataEntries(File file, ImportSettings settings, StatusHolder status) {
        if (!file.isDirectory()) {
            status.setError("Expecting a directory but got file: " + file.getAbsolutePath(), log);
            return Collections.emptyList();
        }

        String[] metadataFileNames = file.list(new SuffixFileFilter(".xml"));
        if (metadataFileNames == null) {
            status.setError("Cannot read list of metadata files from " + file.getAbsolutePath(), log);
            return Collections.emptyList();
        }

        //Import all the xml files within the metadata folder
        List<MetadataEntry> result = new ArrayList<MetadataEntry>();
        for (String metadataFileName : metadataFileNames) {
            File metadataFile = new File(file, metadataFileName);
            String extension = PathUtils.getExtension(metadataFileName);
            if (!verify(status, metadataFileName, metadataFile, extension)) {
                continue;
            }
            status.setDebug("Importing metadata from '" + metadataFile.getPath() + "'.", log);

            try {
                // metadata name is the name of the file without the extension
                String metadataName = PathUtils.stripExtension(metadataFileName);
                String xmlContent = FileUtils.readFileToString(metadataFile);
                MetadataEntry metadataEntry = createMetadataEntry(metadataName, xmlContent);
                result.add(metadataEntry);
            } catch (Exception e) {
                status.setError("Failed to import xml metadata from '" +
                        metadataFile.getAbsolutePath() + "'.", e, log);
            }
        }
        return result;
    }

    private boolean verify(StatusHolder status, String metadataFileName, File metadataFile, String extension) {
        if (metadataFile.exists() && metadataFile.isDirectory()) {
            //Sanity chek
            status.setWarning("Skipping xml metadata import from '" + metadataFile.getAbsolutePath() +
                    "'. Expected a file but encountered a folder.", log);
            return false;
        }
        ContentType type = null;
        if (extension != null) {
            type = NamingUtils.getContentTypeByExtension(extension);
        }
        if (type == null || !type.isXml()) {
            //Sanity chek
            status.setWarning("Skipping xml metadata import from '" + metadataFile.getAbsolutePath() +
                    "'. Expected an XML file but encountered the extension " + extension +
                    " which is not an XML type.", log);
            return false;
        }
        if (extension.length() + 1 >= metadataFileName.length()) {
            // No name for file, just extension
            status.setWarning("Skipping xml metadata import from '" + metadataFile.getAbsolutePath() +
                    "'. The file does not have a name.", log);
            return false;
        }
        return true;
    }

    protected MetadataEntry createMetadataEntry(String metadataName, String xmlContent) {
        // current doesn't need any conversions...
        MetadataEntry metadataEntry = new MetadataEntry(metadataName, xmlContent);
        return metadataEntry;
    }

}
