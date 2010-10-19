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

package org.artifactory.jcr.version;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.JcrConfResourceLoader;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * Converts all the repo.xml files found by artifactory.system.properties and by scanning the etc dir
 *
 * @author Yoav Landman
 */
public abstract class RepoConfigConverterBase implements ConfigurationConverter<ArtifactoryHome> {

    private static final Logger log = LoggerFactory.getLogger(RepoConfigConverterBase.class);
    private static final String DEFAULT_PREV_RESOURCE_NAME = "v161-compatible-" + ArtifactoryHome.ARTIFACTORY_JCR_FILE;

    /**
     * Locates the repo.xml config files
     *
     * @param etcDir Artifactory home etc dir
     * @return Map of repo.xml files to convert
     */
    protected Map<File, Boolean> locateRepoConfigs(File etcDir) {
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

    protected static JcrConfResourceLoader getJcrConfResourceLoader() {
        JcrConfResourceLoader currentJcrConf;
        //If we were previously using the default config we need to copy over the older pure derby config.
        //Future versions should already have a default config copied over from this conversion.
        if (JcrConfResourceLoader.getUserJcrConfigDir() == null) {
            currentJcrConf = new JcrConfResourceLoader(DEFAULT_PREV_RESOURCE_NAME);
        } else {
            currentJcrConf = new JcrConfResourceLoader();
        }
        return currentJcrConf;
    }
}