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

package org.artifactory.jcr.version.v150;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.JcrConfResourceLoader;
import org.artifactory.jcr.version.RepoConfigConverterBase;
import org.artifactory.jcr.version.v150.xml.RepoXmlConverter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.XmlUtils;
import org.jdom.Document;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Converts all the repo.xml files found by artifactory.system.properties and by scanning the etc dir
 *
 * @author Noam Y. Tenne
 */
public class RepoConfigConverterV150 extends RepoConfigConverterBase {

    private static final Logger log = LoggerFactory.getLogger(RepoConfigConverterV150.class);

    public void convert(ArtifactoryHome artifactoryHome) {
        Map<File, Boolean> repoConfigFileMap = locateRepoConfigs(artifactoryHome.getEtcDir());
        convertRepoConfigs(repoConfigFileMap);

        // Add the temporary repo.xml without search index for speeding JcrMetadataConverter
        JcrConfResourceLoader currentJcrConf = null;
        try {
            currentJcrConf = getJcrConfResourceLoader();
            String currentConf = IOUtils.toString(currentJcrConf.getInputStream(), "utf-8");
            int begin = currentConf.indexOf("<SearchIndex ");
            String endTag = "SearchIndex>";
            int end = currentConf.lastIndexOf(endTag);
            if (begin == -1 || end == -1 || begin >= end) {
                log.error(
                        "Repo.xml configuration has invalid SearchIndex position, doing conversion with search index on: Slower!");
            } else {
                log.info("Temporarily deactivating search capabilities during conversion");
                currentConf = currentConf.substring(0, begin) + currentConf.substring(end + 1 + endTag.length());
                JcrConfResourceLoader.setTransientRepoXml(currentConf);
            }
        } catch (IOException e) {
            log.error("Error creating the repo configuration for JCR migration!", e);
        } finally {
            if (currentJcrConf != null) {
                currentJcrConf.close();
            }
        }
    }

    /**
     * Converts all the given repo.xml files
     *
     * @param repoConfigFileMap Map of repo.xml files to convert
     */
    private void convertRepoConfigs(Map<File, Boolean> repoConfigFileMap) {
        RepoXmlConverter repoXmlConverter = new RepoXmlConverter();

        for (Map.Entry<File, Boolean> repoConfigEntry : repoConfigFileMap.entrySet()) {
            File repoConfigFile = repoConfigEntry.getKey();
            if (!repoConfigFile.isFile()) {
                log.warn("repo.xml file at '{}' is either non-existing or not a file. Skipped by the converter.");
                continue;
            }

            FileInputStream repoConfigInputStream = null;
            try {
                repoConfigInputStream = FileUtils.openInputStream(repoConfigFile);
                Document document = XmlUtils.parse(repoConfigInputStream);
                repoXmlConverter.convert(document, repoConfigEntry.getValue());
                String updatedConfig = XmlUtils.outputString(document);
                FileUtils.writeStringToFile(repoConfigFile, updatedConfig, "utf-8");
            } catch (IOException e) {
                String configAbsPath = repoConfigFile.getAbsolutePath();
                log.error("Error occurred while reading repo.xml config at '{}' for conversion: {}.", configAbsPath,
                        e.getMessage());
                log.debug(String.format("Error occurred while reading repo.xml at '%s' config at for conversion",
                        configAbsPath), e);
            } finally {
                IOUtils.closeQuietly(repoConfigInputStream);
            }
        }
    }
}