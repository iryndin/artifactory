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
package org.artifactory.update.md.v125rc0;

import org.apache.commons.io.FileUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.md.MetadataConverterUtils;
import org.artifactory.update.md.v130beta6.ChecksumsConverter;
import org.artifactory.update.md.v130beta6.FolderAdditionalInfoNameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author freds
 * @date Nov 13, 2008
 */
public class MetadataReader125 implements MetadataReader {
    private static final Logger log = LoggerFactory.getLogger(MetadataReader125.class);
    private final MetadataConverter folderConverter = new MdFolderConverter();
    private final MetadataConverter fileConverter = new MdFileConverter();
    private final MetadataConverter statsConverter = new MdStatsConverter();

    public List<MetadataEntry> getMetadataEntries(File file, ImportSettings settings, StatusHolder status) {
        if (!file.isFile()) {
            status.setError("Expecting a file but got a directory: " + file.getAbsolutePath(), log);
            return Collections.emptyList();
        }
        List<MetadataEntry> result = new ArrayList<MetadataEntry>();
        try {
            String xmlContent = FileUtils.readFileToString(file);
            if (xmlContent.contains("<file>")) {
                // convert to file metadata
                String fileXmlContent = MetadataConverterUtils.convertString(fileConverter, xmlContent);
                ChecksumsConverter checksumsConverter = new ChecksumsConverter();
                fileXmlContent = MetadataConverterUtils.convertString(checksumsConverter, fileXmlContent);
                String fileMetadataName = checksumsConverter.getNewMetadataName();
                result.add(new MetadataEntry(fileMetadataName, fileXmlContent));

                // get the stats data from the original xml content
                result.add(convert(xmlContent, statsConverter));
            } else if (xmlContent.contains("<folder>")) {
                xmlContent = MetadataConverterUtils.convertString(this.folderConverter, xmlContent);
                FolderAdditionalInfoNameConverter folderConverter = new FolderAdditionalInfoNameConverter();
                xmlContent = MetadataConverterUtils.convertString(folderConverter, xmlContent);
                String fileMetadataName = folderConverter.getNewMetadataName();
                result.add(new MetadataEntry(fileMetadataName, xmlContent));
            } else {
                status.setError("Failed to import xml metadata from '" +
                        file.getAbsolutePath() + "' since it does not contain any known XML tag <file> <folder>.",
                        log);
            }
        } catch (IOException e) {
            status.setError("Failed to import xml metadata from '" +
                    file.getAbsolutePath() + "'.", e, log);
        }
        return result;
    }

    private MetadataEntry convert(String xmlContent, MetadataConverter converter) {
        MetadataEntry metadataEntry;
        xmlContent = MetadataConverterUtils.convertString(converter, xmlContent);
        metadataEntry = new MetadataEntry(converter.getNewMetadataName(), xmlContent);
        return metadataEntry;
    }
}