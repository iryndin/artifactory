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

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.JcrConfResourceLoader;
import org.artifactory.jcr.version.v150.xml.RepoXmlConverter;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.artifactory.version.XmlConverterUtils;
import org.artifactory.version.converter.ConfigurationConverter;
import org.jdom.Document;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import static org.artifactory.jcr.JcrConfResourceLoader.ARTIFACTORY_ALTERNATE_REPO_XML;

/**
 * Converts all the repo.xml files found by artifactory.system.properties and by scanning the etc dir
 *
 * @author Noam Y. Tenne
 */
public class RepoConfigConverter implements ConfigurationConverter<ArtifactoryHome> {

    private static final Logger log = LoggerFactory.getLogger(RepoConfigConverter.class);

    public void convert(ArtifactoryHome artifactoryHome) {
        if (artifactoryHome.getArtifactoryProperties().getProperty(ARTIFACTORY_ALTERNATE_REPO_XML, null) != null) {
            // Already done the alternate version
            return;
        }
        Map<File, Boolean> repoConfigFileMap = locateRepoConfigs(artifactoryHome.getEtcDir());
        convertRepoConfigs(repoConfigFileMap);

        // Add the temporary repo.xml without search index for speeding JcrMetadataConverter
        JcrConfResourceLoader currentJcrConf = null;
        try {
            currentJcrConf = new JcrConfResourceLoader("repo.xml");
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
                artifactoryHome.getArtifactoryProperties().setProperty(ARTIFACTORY_ALTERNATE_REPO_XML, currentConf);
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
     * Locates the repo.xml config files
     *
     * @param etcDir Artifactory home etc dir
     * @return Map of repo.xml files to convert
     */
    private Map<File, Boolean> locateRepoConfigs(File etcDir) {
        Map<File, Boolean> repoConfigFileMap = Maps.newHashMap();
        if (!etcDir.isDirectory()) {
            log.warn("Etc dir at '{}' is either non-existing or not a directory. repo.xml conversion aborted.",
                    etcDir.getAbsolutePath());
            return repoConfigFileMap;
        }

        addActiveRepoConfigFile(etcDir, repoConfigFileMap);
        addAllRepoConfigsFromEtcDir(etcDir, repoConfigFileMap);
        return repoConfigFileMap;
    }

    /**
     * Adds the active repo.xml file which is declared in artifactory.system.properties to the map
     *
     * @param etcDir            Artifactory home etc dir
     * @param repoConfigFileMap Map of repo.xml files to convert
     */
    private void addActiveRepoConfigFile(File etcDir, Map<File, Boolean> repoConfigFileMap) {
        String jcrConfigDir = ConstantValues.jcrConfigDir.getString();
        if (!PathUtils.hasText(jcrConfigDir)) {
            log.warn("JCR config dir is not defined.");
            return;
        }

        jcrConfigDir = jcrConfigDir.trim();

        //If it is a relative url, relate it to $ARTIFACTORY_HOME/etc
        boolean startWithSlash = jcrConfigDir.startsWith("/");
        boolean startWithFile = jcrConfigDir.startsWith("file:");
        if ((!startWithSlash) && (!startWithFile)) {
            jcrConfigDir = new StringBuilder().append(etcDir.getAbsoluteFile()).append("/").
                    append(jcrConfigDir).toString();
        } else if (startWithFile) {
            try {
                jcrConfigDir = new URL(jcrConfigDir).getFile();
            } catch (MalformedURLException e) {
                throw new RuntimeException("Error while resolving configuration file: " + e.getMessage());
            }
        }

        repoConfigFileMap.put(new File(jcrConfigDir, ArtifactoryHome.ARTIFACTORY_JCR_FILE), true);
    }

    /**
     * Scans the etc dir for repo.xml files and adds them to the map
     *
     * @param etcDir            Artifactory home etc dir
     * @param repoConfigFileMap Map of repo.xml files to convert
     */
    private void addAllRepoConfigsFromEtcDir(File etcDir, Map<File, Boolean> repoConfigFileMap) {
        IOFileFilter repoXmlFileFilter = new AbstractFileFilter() {
            @Override
            public boolean accept(File file) {
                return ArtifactoryHome.ARTIFACTORY_JCR_FILE.equals(file.getName());
            }
        };

        //noinspection unchecked
        Collection<File> repoXmlFiles = FileUtils.listFiles(etcDir, repoXmlFileFilter, DirectoryFileFilter.DIRECTORY);
        for (File file : repoXmlFiles) {
            if (!repoConfigFileMap.containsKey(file)) {
                repoConfigFileMap.put(file, false);
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

        for (File repoConfigFile : repoConfigFileMap.keySet()) {
            if (!repoConfigFile.isFile()) {
                log.warn("repo.xml file at '{}' is either non-existing or not a file. Skipped by the converter.");
                continue;
            }

            FileInputStream repoConfigInputStream = null;
            try {
                repoConfigInputStream = FileUtils.openInputStream(repoConfigFile);
                Document document = XmlConverterUtils.parse(repoConfigInputStream);
                repoXmlConverter.convert(document, repoConfigFileMap.get(repoConfigFile));
                String updatedConfig = XmlConverterUtils.outputString(document);
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