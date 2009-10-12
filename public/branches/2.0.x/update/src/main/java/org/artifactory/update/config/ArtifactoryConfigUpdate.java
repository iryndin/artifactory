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
package org.artifactory.update.config;

import org.apache.commons.io.IOUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.common.ConstantsValue;
import org.artifactory.update.jcr.JcrPathUpdate;
import org.artifactory.update.utils.UpdateUtils;
import org.artifactory.util.FileUtils;
import org.artifactory.version.ArtifactoryConfigVersion;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: freds Date: Jul 1, 2008 Time: 10:34:47 PM
 */
public class ArtifactoryConfigUpdate {
    private final static Logger log = LoggerFactory.getLogger(ArtifactoryConfigUpdate.class);

    private static final String ENCODING = "utf-8";
    private static final String VIRTUAL_REPOSITORIES = "virtualRepositories";
    private static final String REMOTE_REPOSITORIES = "remoteRepositories";
    private static final String KEY_TAG = "key";
    private static final String REPOSITORY_REF = "repositoryRef";

    @SuppressWarnings({"OverlyComplexMethod"})
    public static void migrateLocalRepoToVirtual(File exportDir)
            throws ParserConfigurationException, SAXException, IOException {
        log.info("Migrating local repo to virtual on " + exportDir);

        File currentConfigFile = new File(exportDir, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        if (!currentConfigFile.exists()) {
            throw new RuntimeException(
                    "Config file to convert " + currentConfigFile + " does not exists");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(currentConfigFile);

        Map<String, Element> localRepoKeys = getRepoKeys(document, "localRepositories", true);
        Map<String, Element> remoteRepoKeys = getRepoKeys(document, REMOTE_REPOSITORIES, true);
        Map<String, Element> virtualRepoKeys = getRepoKeys(document, VIRTUAL_REPOSITORIES, true);
        Map<String, Element> excludedRepoKeys = getRepoKeys(document, "excludedRepositories", false);

        // Check if we already did the work of repo reorg
        boolean alreadyDone = true;
        if (localRepoKeys.size() <= virtualRepoKeys.size()) {
            // For each local should have a virtual
            Set<String> keys = localRepoKeys.keySet();
            for (String localRepoKey : keys) {
                if (localRepoKey.endsWith(UpdateUtils.LOCAL_SUFFIX)) {
                    String virtualRepoKey = localRepoKey
                            .substring(0,
                                    localRepoKey.length() - UpdateUtils.LOCAL_SUFFIX.length());
                    if (virtualRepoKeys.containsKey(virtualRepoKey)) {
                        log.info("Already matching local/virtual repository local=" +
                                localRepoKey + " virtual=" + virtualRepoKey);
                    } else {
                        log.info("No matching local/virtual repository local=" + localRepoKey +
                                " virtual=" + virtualRepoKey);
                        alreadyDone = false;
                    }
                } else {
                    log.debug(
                            "Local key after migration should end with '-local' =" + localRepoKey);
                    alreadyDone = false;
                }
            }
        } else {
            log.debug("Not enough virtual repositories");
            alreadyDone = false;
        }

        if (alreadyDone) {
            log.info("Config file " + currentConfigFile +
                    " has already the virtual repositories for each local ones");
            return;
        }

        // Now the config is in last version format
        JcrPathUpdate.setVersion(ArtifactoryVersion.getCurrent());

        // First rename all local repo to localKey-local
        Set<String> keys = localRepoKeys.keySet();
        Set<String> excludedKeys = excludedRepoKeys.keySet();
        boolean localRepositoryNameChanged = false;
        for (String localRepoKey : keys) {
            if (!localRepoKey.endsWith(UpdateUtils.LOCAL_SUFFIX)) {
                // First rename the xml key element
                Element localRepoEl = localRepoKeys.get(localRepoKey);
                String newLocalRepoKey = localRepoKey + UpdateUtils.LOCAL_SUFFIX;
                getKeyTag(localRepoEl, KEY_TAG).setTextContent(newLocalRepoKey);
                log.info(String.format("Renaming local repo %s to %s",
                        localRepoKey, newLocalRepoKey));
                //If the changed local key exists in the backup excludes list, rename it too
                if (excludedKeys.contains(localRepoKey)) {
                    Element excludedElement = excludedRepoKeys.get(localRepoKey);
                    Node firstChild = excludedElement.getFirstChild();
                    firstChild.setTextContent(newLocalRepoKey);
                }

                localRepositoryNameChanged = true;
                // local repositories already named with the -local prefix
                // it is done there because we also need the new name as the value
                // of repoKey tag in the artifactory.file/folder.xml
                /*// Just rename export folder to new key
                File localRepoDir = JcrPath.get().getRepoExportDir(exportDir, localRepoKey);
                File newLocalRepoDir = JcrPath.get().getRepoExportDir(exportDir, newLocalRepoKey);
                if (localRepoDir.exists() && !newLocalRepoDir.exists()) {
                    log.info("Renaming export directory of " + localRepoDir + " to " +
                            newLocalRepoDir);
                    localRepoDir.renameTo(newLocalRepoDir);
                } else {
                    log.warn("Trying to migrate export directory of " + localRepoDir + " to " +
                            newLocalRepoDir + " but:\n" +
                            "Origin does not exists or destination exists. May be already migrated?");
                }*/
            }
        }

        if (localRepositoryNameChanged) {
            displayMessageOnLocalRepositoryRename();
        }

        // If no remote repositories, nothing else to do
        if (remoteRepoKeys.isEmpty()) {
            log.info("No remote repositories are defined so no need for virtual repositories");
        } else {
            // Create the needed virtual repo config
            Element virtualRepositoriesTag;
            NodeList virtualReposTag = document.getElementsByTagName(VIRTUAL_REPOSITORIES);
            if (virtualReposTag.getLength() == 0) {
                // Need to add the virtualRepositories tag manually
                virtualRepositoriesTag = document.createElement(VIRTUAL_REPOSITORIES);
                Element configEl = (Element) document.getElementsByTagName("config").item(0);
                Node remoteRepos = configEl.getElementsByTagName(REMOTE_REPOSITORIES).item(0);
                Node afterRemoteRepos = remoteRepos.getNextSibling();
                if (afterRemoteRepos == null) {
                    configEl.appendChild(virtualRepositoriesTag);
                } else {
                    configEl.insertBefore(virtualRepositoriesTag, afterRemoteRepos);
                }
            } else {
                virtualRepositoriesTag = (Element) virtualReposTag.item(0);
            }

            for (String localRepoKey : keys) {
                if (!localRepoKey.endsWith(UpdateUtils.LOCAL_SUFFIX)) {
                    // First rename the xml key element
                    String newLocalRepoKey = localRepoKey + UpdateUtils.LOCAL_SUFFIX;
                    if (virtualRepoKeys.containsKey(localRepoKey)) {
                        log.info("A virtual repository " + localRepoKey + " already exists");
                    } else {
                        Element newVirtualRepo = document.createElement("virtualRepository");
                        virtualRepositoriesTag.appendChild(newVirtualRepo);
                        Element keyTag = document.createElement(KEY_TAG);
                        newVirtualRepo.appendChild(keyTag);
                        keyTag.setTextContent(localRepoKey);
                        Element repositories = document.createElement("repositories");
                        newVirtualRepo.appendChild(repositories);
                        Element repoRef = document.createElement(REPOSITORY_REF);
                        repoRef.setTextContent(newLocalRepoKey);
                        repositories.appendChild(repoRef);
                        Set<String> remoteRepos = remoteRepoKeys.keySet();
                        for (String remoteRepo : remoteRepos) {
                            repoRef = document.createElement(REPOSITORY_REF);
                            repoRef.setTextContent(remoteRepo);
                            repositories.appendChild(repoRef);
                        }
                    }
                }
            }
        }

        File newConfigFile = new File(exportDir, "new_" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        generateXmlFile(document, newConfigFile);
        FileUtils.switchFiles(currentConfigFile, newConfigFile);

        log.info("Finish migrating local repo to virtual on " + exportDir);
    }

    private static void generateXmlFile(Document doc, File outFile) throws IOException {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(outFile), ENCODING);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        } catch (Exception e) {
            log.error("Failed serializing config file", e);
            throw new IOException(e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static Map<String, Element> getRepoKeys(Document document, String tagname, boolean getByKeyTag) {
        Map<String, Element> repoKeys = new HashMap<String, Element>();
        NodeList repositoriesTag = document.getElementsByTagName(tagname);
        if (repositoriesTag.getLength() == 0) {
            log.warn("No repositories " + tagname + " found in " + document.getDocumentURI());
        } else {
            NodeList repos = repositoriesTag.item(0).getChildNodes();
            int nbRepos = repos.getLength();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < nbRepos; i++) {
                Node node = repos.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    //If the repo key should be under the <key> tag
                    if (getByKeyTag) {
                        Element keyTag = getKeyTag(element, KEY_TAG);
                        if (keyTag == null) {
                            log.warn("No key found for " + tagname + " repository " + element);
                        } else {
                            String key = keyTag.getTextContent();
                            repoKeys.put(key, element);
                            builder.append(" ").append(key);
                        }
                    } else { //If the repo key is in a different location (excludes for example)
                        Node firstChild = element.getFirstChild();
                        if ((firstChild == null) || (firstChild.getTextContent() == null) ||
                                ("".equals(firstChild.getTextContent()))) {
                            log.warn("No key found for " + tagname + " repository " + element);
                        } else {
                            String key = firstChild.getTextContent();
                            repoKeys.put(key, element);
                            builder.append(" ").append(key);
                        }
                    }
                }
            }
            builder.insert(0, " repos: ").insert(0, tagname).insert(0, " ")
                    .insert(0, repoKeys.size()).insert(0, "Found ");
            log.info(builder.toString());
        }
        return repoKeys;
    }

    private static Element getKeyTag(Element element, String keyTagName) {
        NodeList list = element.getElementsByTagName(keyTagName);
        if (list.getLength() == 0) {
            return null;
        }
        return (Element) list.item(0);
    }

    private static void convertConfigFile(File oldConfigFile, File newConfigFile)
            throws IOException {
        FileInputStream is = null;
        String configXml;
        try {
            is = new FileInputStream(oldConfigFile);
            configXml = IOUtils.toString(is, "utf-8");
        } finally {
            if (is != null) {
                is.close();
            }
        }
        String newConfigXml;
        ArtifactoryConfigVersion configVersion =
                ArtifactoryConfigVersion.getConfigVersion(configXml);
        if (configVersion == null) {
            throw new RuntimeException(
                    "No valid Artifactory XSD version found in config file " + oldConfigFile);
        } else if (configVersion == ArtifactoryConfigVersion.getCurrent()) {
            log.warn("Version detected in config file " + oldConfigFile +
                    " is the latest one!");
            log.warn("Config file will not be converted! Already did it?");
            newConfigXml = configXml;
        } else {
            addDefaultRepoKeySubstitute(configVersion);
            displayMessageOnSubstitute();
            newConfigXml = configVersion.convert(configXml);
        }

        // Create the new artifactory.config.xml file
        FileOutputStream newFos = null;
        try {
            newFos = new FileOutputStream(newConfigFile);
            IOUtils.write(newConfigXml, newFos, "utf-8");
        } finally {
            if (newFos != null) {
                newFos.close();
            }
        }
    }

    private static void displayMessageOnSubstitute() {
        Map<String, String> keys = ArtifactoryProperties.get().getSubstituteRepoKeys();
        if (!keys.isEmpty()) {
            log.warn("****************** ATTENTION ******************");
            log.warn("Some substitution keys will be used.");
            log.warn("Please make sure you add the following JVM params " +
                    "to your Artifactory if needed:");
            Set<Map.Entry<String, String>> entries = keys.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                log.warn("-D" + ConstantsValue.substituteRepoKeys.getPropertyName() +
                        entry.getKey() + "=" + entry.getValue() + " \\");
            }
            log.warn("***********************************************");
        }
    }

    private static void displayMessageOnLocalRepositoryRename() {
        if (!ArtifactoryProperties.get().getSubstituteRepoKeys().isEmpty()) {
            log.warn("****************** ATTENTION ******************");
            log.warn("Some local repository names were changed.");
            log.warn("Please make sure to update your repository url in the distribution " +
                    "management section of the pom.xml if needed .");
            log.warn("***********************************************");
        }
    }

    private static void addDefaultRepoKeySubstitute(ArtifactoryConfigVersion configVersion) {
        ArtifactoryProperties properties = ArtifactoryProperties.get();
        Map<String, String> keys = properties.getSubstituteRepoKeys();
        if (configVersion == ArtifactoryConfigVersion.OneZero &&
                keys.isEmpty()) {
            log.warn(
                    "The config file is version 1.0.0 which may have broken repo key, and the substitute key map is empty");
            log.warn(
                    "The default substitute keys will be added. Don't forget to add them to the JAVA_OPTIONS of Artifactory start script");
            properties.addSubstitute("3rd-party", "third-party");
            properties.addSubstitute("3rdp-releases", "third-party-releases");
            properties.addSubstitute("3rdp-snapshots", "third-party-snapshots");
        }
    }

    /**
     * @param oldConfigFile the old artifactory.config.xml file
     * @return the new config file new_artifactory.config.xml File
     * @throws java.io.IOException if the new file cannot be created
     */
    public static File convertConfigFile(File oldConfigFile) throws IOException {
        if (!oldConfigFile.exists()) {
            throw new IOException(
                    "Cannot convert an old version of Artifactory without the config file " +
                            oldConfigFile);
        }
        File newConfigFile = new File(oldConfigFile.getParentFile(),
                "new_" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);

        convertConfigFile(oldConfigFile, newConfigFile);
        return newConfigFile;
    }
}
