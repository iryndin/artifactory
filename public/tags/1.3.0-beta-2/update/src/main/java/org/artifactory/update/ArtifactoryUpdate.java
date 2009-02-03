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
package org.artifactory.update;

import com.sun.org.apache.xml.internal.serialize.DOMSerializer;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryConstants;
import org.artifactory.ArtifactoryHome;
import org.artifactory.common.ExceptionUtils;
import org.artifactory.jcr.JcrPath;
import org.artifactory.process.StatusHolder;
import org.artifactory.spring.ArtifactoryApplicationContext;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ArtifactoryContextThreadBinder;
import org.springframework.beans.BeansException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by IntelliJ IDEA. User: freds
 */
public class ArtifactoryUpdate {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryUpdate.class);

    private static final String WEBAPPS_ARTIFACTORY_WAR = "webapps/artifactory.war";
    private static final String LIB_ARTIFACTORY_CORE = "WEB-INF/lib/artifactory-core";
    private static final String ARTIFACTORY_CONFIG_ORIGINAL_XML = "artifactory.config.original.xml";
    private static final String ARTIFACTORY_CONFIG_ORIGINAL = "artifactory.config.original.";
    private static final String LOCAL_SUFFIX = "-local";
    private static final String CONFIG_PARAM = "--config";
    private static final String NOCONVERT_PARAM = "--noconvert";
    protected static final String ENCODING = "UTF-8";
    private static final String VIRTUAL_REPOSITORIES = "virtualRepositories";
    private static final String REMOTE_REPOSITORIES = "remoteRepositories";
    private static final String KEY_TAG = "key";
    private static final String REPOSITORY_REF = "repositoryRef";

    /**
     * Main function, starts the Artifactory migration process.
     */
    public static void main(String[] args) {
        int returnValue = 0;
        ArtifactoryContext context = null;
        try {
            boolean onlyConvertLocalRepo = false;
            boolean convertLocalRepo = true;
            String exportFolder = null;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals(NOCONVERT_PARAM)) {
                    convertLocalRepo = false;
                } else if (arg.equals(CONFIG_PARAM)) {
                    if (i == args.length-1) {
                        System.err.println("The param " + CONFIG_PARAM +" shoudl be followed by the export folder name");
                        usage();
                    }
                    onlyConvertLocalRepo = true;
                    exportFolder = args[args.length - 1];
                }
            }
            if (onlyConvertLocalRepo && !convertLocalRepo) {
                // param are exclusive
                System.err.println("Parameter "+CONFIG_PARAM+" and "+NOCONVERT_PARAM+" cannot be specified togetger!");
                usage();
            }

            if (onlyConvertLocalRepo) {
                if (exportFolder == null) {
                    usage();
                }
                File exportDir = new File(exportFolder);
                if (!exportDir.exists() || !exportDir.isDirectory()) {
                    throw new RuntimeException(
                            "The export folder " + exportFolder + " does not exists or is not a directory");
                }
                migrateLocalRepoToVirtual(exportDir);
                return;
            }

            File artifactoryHomeFile = getArtifactoryHome(args);
            // Don't create artifactory home yet, we need to convert the config file

            // Find the version
            final ArtifactoryVersion version = getArtifactoryVersion(args, artifactoryHomeFile);

            // Set the system properties instead of artifactory.properties file
            System.setProperty("artifactory.version", version.getValue());
            System.setProperty("artifactory.revision", "" + version.getRevision());

            JcrPathUpdate.register(version);

            // Convert the config xml version
            File etcDir = new File(artifactoryHomeFile, "etc");
            if (!etcDir.exists()) {
                throw new RuntimeException(
                        "Cannot convert an old version of Artifactory without the etc folder in " +
                                etcDir);
            }
            File oldConfigFile = new File(etcDir, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            if (!oldConfigFile.exists()) {
                throw new RuntimeException(
                        "Cannot convert an old version of Artifactory without the config file " +
                                oldConfigFile);
            }
            File newConfigFile = new File(etcDir, "new_" + ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);

            convertConfigFile(oldConfigFile, newConfigFile);
            System.setProperty(ArtifactoryHome.SYS_PROP_ARTIFACTORY_CONFIG,
                    newConfigFile.getAbsolutePath());
            ArtifactoryHome.create();
            // At rev 1291 the root jcr folder moved up
            if (version.getRevision() < 1291) {
                ArtifactoryHome.setJcrRootDir(
                        ArtifactoryHome.getOrCreateSubDir(ArtifactoryHome.getDataDir(), "jcr"));
            }

            // TODO: fetch or compare the revision version to the actual db value ?
            // TODO: Make a list of feature per commit rev
            int appdbChange = 1291;

            String appDbDirName;
            if (version.getRevision() < appdbChange) {
                appDbDirName = "appdb";
            } else {
                appDbDirName = "db";
            }
            System.setProperty("appdb.dirname", appDbDirName);
            VersionsHolder.setOriginalVersion(version);
            VersionsHolder.setFinalVersion(ArtifactoryVersion.v130beta1);

            try {
                context = new ArtifactoryApplicationContext("updateApplicationContext.xml");
            } catch (BeansException e) {
                // Find if the original exception is due to xs:id conversion
                SAXParseException saxException = extractSaxException(e);
                if (saxException != null &&
                        saxException.getMessage().contains("is not a valid value for 'NCName'")) {
                    System.err.println(
                            "You got this error because repository keys needs to respect XML ID specification");
                    System.err.println(
                            "Please execute the Artifactory Updater with the same JVM parameters.");
                    System.err.println("You should have a list of -D" + ArtifactoryConstants
                            .SYS_PROP_PREFIX_REPO_KEY_SUBST + "oldRepoKey=newRepoKey");
                    System.err.println(
                            "Please refer to http://www.jfrog.org/confluence/display/RTF/Upgrading+Artifactory");
                    throw saxException;
                }
                throw e;
            }
            File exportDir = export(context);
            if (convertLocalRepo) {
                migrateLocalRepoToVirtual(exportDir);
            } else {
                LOGGER.info("Param "+NOCONVERT_PARAM+" passed! No conversion of local repository to virtual done.");
            }
        } catch (Exception e) {
            System.err.println("Problem during Artifactory migration: " + e);
            e.printStackTrace();
            returnValue = -1;
        } finally {
            if (context != null) {
                //                context.destroy();
            }
        }
        System.exit(returnValue);
    }

    private static void migrateLocalRepoToVirtual(File exportDir) throws ParserConfigurationException, SAXException, IOException {
        LOGGER.info("Migrating local repo to virtual on " + exportDir);

        File currentConfigFile = new File(exportDir, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        if (!currentConfigFile.exists()) {
            throw new RuntimeException("Config file to convert " + currentConfigFile + " does not exists");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(currentConfigFile);

        Map<String, Element> localRepoKeys = getRepoKeys(document, "localRepositories");
        Map<String, Element> remoteRepoKeys = getRepoKeys(document, REMOTE_REPOSITORIES);
        Map<String, Element> virtualRepoKeys = getRepoKeys(document, VIRTUAL_REPOSITORIES);

        // Check if we already did the work of repo reorg
        boolean alreadyDone = true;
        if (localRepoKeys.size() < virtualRepoKeys.size()) {
            // For each local should have a virtual
            Set<String> keys = localRepoKeys.keySet();
            for (String localRepoKey : keys) {
                if (localRepoKey.endsWith(LOCAL_SUFFIX)) {
                    String virtualRepoKey = localRepoKey.substring(0, localRepoKey.length() - LOCAL_SUFFIX.length());
                    if (virtualRepoKeys.containsKey(virtualRepoKey)) {
                        LOGGER.info("Already matching local/virtual repository local=" + localRepoKey + " virtual=" + virtualRepoKey);
                    } else {
                        LOGGER.info("No matching local/virtual repository local=" + localRepoKey + " virtual=" + virtualRepoKey);
                        alreadyDone = false;
                    }
                } else {
                    LOGGER.debug("Local key after migration should end with '-local' =" + localRepoKey);
                    alreadyDone = false;
                }
            }
        } else {
            LOGGER.debug("Not enough virtual repositories");
            alreadyDone = false;
        }

        if (alreadyDone) {
            LOGGER.info("Config file " + currentConfigFile + " has already the virtual repositories for each local ones");
            return;
        }

        // Now the config is in last version format
        JcrPathUpdate.register(ArtifactoryVersion.v130beta1);

        // First rename all local repo to localKey-local
        Set<String> keys = localRepoKeys.keySet();
        for (String localRepoKey : keys) {
            if (!localRepoKey.endsWith(LOCAL_SUFFIX)) {
                // First rename the xml key element
                Element localRepoEl = localRepoKeys.get(localRepoKey);
                String newLocalRepoKey = localRepoKey + LOCAL_SUFFIX;
                getKeyTag(localRepoEl).setTextContent(newLocalRepoKey);
                // Just rename export folder to new key
                File localRepoDir = JcrPath.get().getRepoExportDir(exportDir, localRepoKey);
                File newLocalRepoDir = JcrPath.get().getRepoExportDir(exportDir, newLocalRepoKey);
                if (localRepoDir.exists() && !newLocalRepoDir.exists()) {
                    LOGGER.info("Renaming export directory of " + localRepoDir + " to " + newLocalRepoDir);
                    localRepoDir.renameTo(newLocalRepoDir);
                } else {
                    LOGGER.warn("Trying to migrate export directory of " + localRepoDir + " to " + newLocalRepoDir + " but:\n" +
                            "Origine does not exists or destination exists. May be already migrated?");
                }
            }
        }

        // If no remote repositories, nothing else to do
        if (remoteRepoKeys.isEmpty()) {
            LOGGER.info("No remote repositories are defined so no need for virtual repositories");
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
                if (!localRepoKey.endsWith(LOCAL_SUFFIX)) {
                    // First rename the xml key element
                    String newLocalRepoKey = localRepoKey + LOCAL_SUFFIX;
                    if (virtualRepoKeys.containsKey(localRepoKey)) {
                        LOGGER.info("A virtual repository " + localRepoKey + " already exists");
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
        switchConfigFiles(exportDir, currentConfigFile, newConfigFile);

        LOGGER.info("Finish migrating local repo to virtual on " + exportDir);
    }

    private static void generateXmlFile(Document doc, File outFile) throws IOException {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(outFile), ENCODING);
            OutputFormat outputFormat = new OutputFormat(); // should this be null ???
            outputFormat.setEncoding(ENCODING);
            outputFormat.setIndenting(true);
            DOMSerializer serializer = new XMLSerializer(writer, outputFormat);
            serializer.serialize(doc);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static Map<String, Element> getRepoKeys(Document document, String tagname) {
        Map<String, Element> repoKeys = new HashMap<String, Element>();
        NodeList repositoriesTag = document.getElementsByTagName(tagname);
        if (repositoriesTag.getLength() == 0) {
            LOGGER.warn("No repositories " + tagname + " found in " + document.getDocumentURI());
        } else {
            NodeList repos = repositoriesTag.item(0).getChildNodes();
            int nbRepos = repos.getLength();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < nbRepos; i++) {
                Node node = repos.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    Element keyTag = getKeyTag(element);
                    if (keyTag == null) {
                        LOGGER.warn("No key found for " + tagname + " repository " + element);
                    } else {
                        String key = keyTag.getTextContent();
                        repoKeys.put(key, element);
                        builder.append(" ").append(key);
                    }
                }
            }
            builder.insert(0, " repos: ").insert(0, tagname).insert(0, " ").insert(0, repoKeys.size()).insert(0, "Found ");
            LOGGER.info(builder.toString());
        }
        return repoKeys;
    }

    private static Element getKeyTag(Element element) {
        NodeList list = element.getElementsByTagName(KEY_TAG);
        if (list.getLength() == 0) {
            return null;
        }
        return (Element) list.item(0);
    }

    private static File export(ArtifactoryContext context) {
        try {
            ArtifactoryContextThreadBinder.bind(context);

            File tmpExport = new File("tmpExport");
            if (!tmpExport.exists()) {
                tmpExport.mkdir();
            }
            LOGGER.info(
                    "Doing an export on dir=[" + tmpExport.getAbsolutePath() + "]");
            StatusHolder status = new StatusHolder();
            context.exportTo(tmpExport, null, false, new Date(), status);
            File result = (File) status.getCallback();
            LOGGER.info(
                    "Did a full export in [" + result.getAbsolutePath() + "]");
            return result;
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }


    /**
     * This method is for the sake of supressing unchecked assignment
     *
     * @param e
     * @return
     */
    @SuppressWarnings({"unchecked"})
    private static SAXParseException extractSaxException(BeansException e) {
        return (SAXParseException) ExceptionUtils.getCauseOfTypes(e, SAXParseException.class);
    }

    private static void switchConfigFiles(File etcDir, File oldConfigFile, File newConfigFile) {
        File savedOrigConfig = new File(etcDir, ARTIFACTORY_CONFIG_ORIGINAL_XML);
        if (savedOrigConfig.exists()) {
            // Rolling files
            String[] allOrigFiles = etcDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(ARTIFACTORY_CONFIG_ORIGINAL) && name.endsWith(".xml") &&
                            name.length() > ARTIFACTORY_CONFIG_ORIGINAL_XML.length();
                }
            });
            int maxNb = 1;
            for (String origFile : allOrigFiles) {
                try {
                    String middle = origFile.substring(ARTIFACTORY_CONFIG_ORIGINAL.length(),
                            origFile.length() - 4);
                    int value = Integer.parseInt(middle);
                    if (value >= maxNb) {
                        maxNb = value + 1;
                    }
                } catch (Exception e) {
                    System.out
                            .println("Minor issue in file name " + origFile + ":" + e.getMessage());
                }
            }
            savedOrigConfig = new File(etcDir, ARTIFACTORY_CONFIG_ORIGINAL + maxNb + ".xml");
        }
        oldConfigFile.renameTo(savedOrigConfig);
        newConfigFile.renameTo(new File(etcDir, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE));
    }

    private static void convertConfigFile(File oldConfigFile, File newConfigFile)
            throws IOException {
        FileInputStream is = null;
        String configXml;
        try {
            is = new FileInputStream(oldConfigFile);
            configXml = IOUtils.toString(is, "UTF-8");
        } finally {
            if (is != null) {
                is.close();
            }
        }
        String newConfigXml = null;
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        for (ArtifactoryConfigVersion configVersion : versions) {
            if (configXml.contains(configVersion.getXsdUri())) {
                // If last version nothing to do, So why do you run me?
                if (configVersion == ArtifactoryConfigVersion.OneThree) {
                    LOGGER.warn("Version detected in config file " + oldConfigFile +
                            " is the latest one!");
                    LOGGER.warn("Config file will not be converted! Already did it?");
                    newConfigXml = configXml;
                } else {
                    if (configVersion == ArtifactoryConfigVersion.OneZero &&
                            ArtifactoryConstants.substituteRepoKeys.isEmpty()) {
                        LOGGER.warn(
                                "The config file is version 1.0.0 which may have broken repo key, and the substitute key map is empty");
                        LOGGER.warn(
                                "The default substitute keys will be added. Don't forget to add them to the JAVA_OPTIONS of Artifactory start script");
                        ArtifactoryConstants.substituteRepoKeys.put("3rd-party", "third-party");
                        ArtifactoryConstants.substituteRepoKeys
                                .put("3rdp-releases", "third-party-releases");
                        ArtifactoryConstants.substituteRepoKeys
                                .put("3rdp-snapshots", "third-party-snapshots");
                    }
                    if (!ArtifactoryConstants.substituteRepoKeys.isEmpty()) {
                        LOGGER.info("The substitution keys used are:");
                        StringBuffer buffer = new StringBuffer("\n");
                        Set<Map.Entry<String, String>> entries =
                                ArtifactoryConstants.substituteRepoKeys.entrySet();
                        for (Map.Entry<String, String> entry : entries) {
                            buffer.append("-D")
                                    .append(ArtifactoryConstants.SYS_PROP_PREFIX_REPO_KEY_SUBST).
                                    append(entry.getKey()).append("=").
                                    append(entry.getValue()).append(" \\").append("\n");
                        }
                        LOGGER.info(buffer.toString());
                    }
                    newConfigXml = configVersion.convert(configXml);
                }
                break;
            }
        }
        if (newConfigXml == null) {
            throw new RuntimeException(
                    "No valid Artifactory XSD version found in config file " + oldConfigFile);
        }

        // Create the new artifactory.config.xml file
        FileOutputStream newFos = null;
        try {
            newFos = new FileOutputStream(newConfigFile);
            IOUtils.write(newConfigXml, newFos, "UTF-8");
        } finally {
            if (newFos != null) {
                newFos.close();
            }
        }
    }

    /**
     * You never return from usage since it throws System.exit(). But for good static analysis may
     * be Neal is rigth about the Nothing type?
     */
    private static void usage() {
        System.out.println();
        System.out.println("java -jar artifactory-update.jar artifactory.home [original version] [--config exportFolder]");
        System.out.println("The Artifactory home dir parameter is mandatory.");
        System.out.println("The Artifactory version will be extracted from ${artifactpry.home}/" +
                WEBAPPS_ARTIFACTORY_WAR);
        System.out.println("The optional param --config execute just the config migration on the export folder passed");
        System.out.println("If the war file is not located there, please do:");
        System.out.println("1) link or copy it at this location, or pass the version.");
        System.out.println("2) pass one of the following version as second parameter:");
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        int i = 0;
        for (ArtifactoryVersion version : versions) {
            System.out.print("  " + version.getValue());
            if (i % 5 == 4) {
                System.out.println();
            }
            i++;
        }
        System.out.println();
        System.exit(-1);
    }

    private static ArtifactoryVersion getArtifactoryVersion(String[] args, File artifactoryHomeFile)
            throws IOException {
        String versionName = null;
        if (args.length > 1 && !args[1].startsWith("--")) {
            versionName = args[1];
        } else {
            LOGGER.info("Finding version...");
            File warFile = new File(artifactoryHomeFile, WEBAPPS_ARTIFACTORY_WAR);
            if (!warFile.exists()) {
                LOGGER.error("War file " + warFile +
                        " does not exists please put it there or give a version param");
                usage();
            }
            ZipFile zipFile = new ZipFile(warFile);
            Enumeration<? extends ZipEntry> warEntries = zipFile.entries();
            while (warEntries.hasMoreElements()) {
                ZipEntry zipEntry = warEntries.nextElement();
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.startsWith(LIB_ARTIFACTORY_CORE) &&
                        zipEntryName.endsWith(".jar")) {
                    versionName = zipEntryName.substring(LIB_ARTIFACTORY_CORE.length() + 1,
                            zipEntryName.length() - 4);
                    break;
                }
            }
            if (versionName == null) {
                LOGGER.error("Did not find the version in " + warFile + " looking for " +
                        LIB_ARTIFACTORY_CORE);
                usage();
            }
            LOGGER.info("found version name " + versionName);
        }

        ArtifactoryVersion[] artifactoryVersions = ArtifactoryVersion.values();
        ArtifactoryVersion version = null;
        for (ArtifactoryVersion artifactoryVersion : artifactoryVersions) {
            if (artifactoryVersion.getValue().equals(versionName)) {
                version = artifactoryVersion;
                break;
            }
        }
        if (version == null) {
            LOGGER.error(
                    "Version " + versionName + " is wrong or is not supported by this updater");
            LOGGER.error("If you know a good close version, please give a version param");
            usage();
            // Too avoid the may have NPE below
            return null;
        }
        if (version.isLastVersion()) {
            LOGGER.error("Version " + versionName + " is the latest version, no update needed");
            LOGGER.error("If you know it's an old version, please give a version param");
            usage();
        }
        LOGGER.info("Found supported version " + version.getValue() + " revision " +
                version.getRevision());
        // If the version is before the xs:id usage print a warning message
        return version;
    }

    private static File getArtifactoryHome(String[] args) {
        if (args.length == 0) {
            usage();
        }
        if (System.getProperty(ArtifactoryHome.SYS_PROP) != null) {
            LOGGER.error(
                    "Artifactory Update should not be called with -Dartifactory.home=XXX set.");
            LOGGER.error("Artifactory Home is the first application parameter.");
            usage();
        }
        String artifactoryHome = args[0];
        if (artifactoryHome == null) {
            LOGGER.error("Did not find artifactory home value.\n" +
                    "Please pass the artifactory home dir as a parameter.");
            usage();
        }
        File artifactoryHomeDir = new File(artifactoryHome);
        if (!artifactoryHomeDir.exists() || !artifactoryHomeDir.isDirectory()) {
            LOGGER.error("Artifactory home " + artifactoryHomeDir.getAbsolutePath() +
                    "does not exists or is not a directory.");
            usage();
        }
        ArtifactoryHome.setHomeDir(artifactoryHomeDir);

        LOGGER.info(
                "Will migrate Artifactory Home dir=[" + artifactoryHomeDir.getAbsolutePath() + "]");
        return artifactoryHomeDir;
    }
}
