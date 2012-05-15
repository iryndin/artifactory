/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.jcr.version.v210;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.JcrConfResourceLoader;
import org.artifactory.jcr.version.RepoConfigConverterBase;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.XmlUtils;
import org.jdom.DocType;
import org.jdom.Document;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Removes the ds_ prefix from the default datastore if copies the default repo.xml (no user jcr config), and updates
 * the DTD if exist in all repo.xml's
 *
 * @author Yoav Landman
 */
public class RepoConfigConverterV210 extends RepoConfigConverterBase {

    private static final Logger log = LoggerFactory.getLogger(RepoConfigConverterV210.class);

    //Pure derby repo.xml which is backward compatible with the default installation in prev versions

    @Override
    public void convert(ArtifactoryHome artifactoryHome) {
        //We may get a transient xml so turn it off do the conversion on the real file then turn on the transient
        //and convert it
        String transientRepoXml = JcrConfResourceLoader.removeTransientRepoXml();
        convert();
        if (transientRepoXml != null) {
            JcrConfResourceLoader.setTransientRepoXml(transientRepoXml);
            convert();
        }
        //Update the DTDs
        convertRepoConfigs(artifactoryHome);
    }

    /**
     * Will cause the default config to be copied or just read the user configured old repo.xml (in this case nothing to
     * replace)
     */
    private void convert() {
        JcrConfResourceLoader currentJcrConf = getJcrConfResourceLoader();
        try {
            //Remove the ds_ prefix from the default datastore
            InputStream inputStream = currentJcrConf.getInputStream();
            String currentConf = IOUtils.toString(inputStream, "utf-8");
            String updatedConfig =
                    StringUtils.replace(currentConf, "<param name=\"schemaObjectPrefix\" value=\"ds_\"/>",
                            "<param name=\"schemaObjectPrefix\" value=\"\"/>");
            if (!updatedConfig.equals(currentConf)) {
                log.info("Datastore prefix removed from default config.");
                //Only save the config if not transient
                if (JcrConfResourceLoader.getTransientRepoXml() == null) {
                    File repoConfigFile = currentJcrConf.getConfigFile();
                    FileUtils.writeStringToFile(repoConfigFile, updatedConfig, "utf-8");
                } else {
                    JcrConfResourceLoader.setTransientRepoXml(updatedConfig);
                }
            }
        } catch (IOException e) {
            log.error("Error creating the repo configuration for JCR migration!", e);
        } finally {
            currentJcrConf.close();
        }
    }

    /**
     * Converts all the given repo.xml files
     */
    private void convertRepoConfigs(ArtifactoryHome artifactoryHome) {
        //TODO: [by yl] skip the local config
        Map<File, Boolean> repoConfigFileMap = locateRepoConfigs(artifactoryHome.getEtcDir());

        for (File repoConfigFile : repoConfigFileMap.keySet()) {
            if (!repoConfigFile.isFile()) {
                log.warn("repo.xml file at '{}' is either non-existing or not a file. Skipped by the converter.");
                continue;
            }

            FileInputStream repoConfigInputStream = null;
            String configAbsPath = repoConfigFile.getAbsolutePath();
            try {
                repoConfigInputStream = FileUtils.openInputStream(repoConfigFile);
                Document doc = XmlUtils.parse(repoConfigInputStream);

                //Remove the system id and replace with a public id
                DocType docType = doc.getDocType();
                if (docType != null) {
                    log.info("Update the repo configuration at '{}'.", configAbsPath);
                    docType.setPublicID("-//The Apache Software Foundation//DTD Jackrabbit 2.0//EN");
                    docType.setSystemID("http://jackrabbit.apache.org/dtd/repository-2.0.dtd");
                    String updatedConfig = XmlUtils.outputString(doc);
                    FileUtils.writeStringToFile(repoConfigFile, updatedConfig, "utf-8");
                }
            } catch (IOException e) {
                log.error("Error occurred while reading repo.xml config at '{}' for conversion: {}.", configAbsPath,
                        e.getMessage());
                log.debug("Error occurred while reading repo.xml at '{}' config at for conversion", configAbsPath, e);
            } finally {
                IOUtils.closeQuietly(repoConfigInputStream);
            }
        }
    }
}