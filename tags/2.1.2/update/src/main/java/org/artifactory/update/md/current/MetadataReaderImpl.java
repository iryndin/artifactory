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

package org.artifactory.update.md.current;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.md.MetadataReader;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

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
