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
import org.artifactory.api.mime.PackagingType;
import org.artifactory.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Nov 13, 2008
 */
public class MetadataReaderImpl implements MetadataReader {
    private static final Logger log = LoggerFactory.getLogger(MetadataReaderImpl.class);

    public List<MetadataEntry> getMetadataEntries(File file, ImportSettings settings, StatusHolder status) {
        //TODO: Check that the file is a folder...
        List<MetadataEntry> result = new ArrayList<MetadataEntry>();
        //Import all the xml files within the metadata folder
        String[] metadataFileNames = file.list(new SuffixFileFilter(".xml"));
        if (metadataFileNames == null) {
            status.setError(
                    "Cannot read list of metadata file from " + file.getAbsolutePath(), log);
            return result;
        }
        for (String metadataFileName : metadataFileNames) {
            File metadataFile = new File(file, metadataFileName);
            if (metadataFile.exists() && metadataFile.isDirectory()) {
                //Sanity chek
                status.setWarning("Skipping xml metadata import from '" +
                        metadataFile.getAbsolutePath() +
                        "'. Expected a file but encountered a folder.", log);
                continue;
            }
            String extension = PathUtils.getExtension(metadataFileName);
            ContentType type = null;
            if (extension != null) {
                type = PackagingType.getContentTypeByExtension(extension);
            }
            if (type == null || !type.isXml()) {
                //Sanity chek
                status.setWarning("Skipping xml metadata import from '" +
                        metadataFile.getAbsolutePath() +
                        "'. Expected an XML file but encountered the extension " + extension +
                        " which is not an XML type.", log);
                continue;
            }
            if (extension.length() + 1 >= metadataFileName.length()) {
                // No name for file, just extension
                status.setWarning("Skipping xml metadata import from '" +
                        metadataFile.getAbsolutePath() +
                        "'. The file does not have a name.", log);
                continue;
            }
            status.setDebug("Importing metadata from '" + metadataFile.getPath() + "'.", log);
            try {
                String metadataName = metadataFileName.substring(0, metadataFileName.length() - extension.length() - 1);
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

    protected MetadataEntry createMetadataEntry(String metadataName, String xmlContent) {
        MetadataEntry metadataEntry = new MetadataEntry(metadataName, xmlContent);
        return metadataEntry;
    }

}