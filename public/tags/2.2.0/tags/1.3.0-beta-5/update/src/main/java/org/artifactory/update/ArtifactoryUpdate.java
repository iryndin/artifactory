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

import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.cli.Option;
import org.artifactory.cli.OptionParser;
import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.update.config.ArtifactoryConfigUpdate;
import org.artifactory.update.config.ConfigExporter;
import org.artifactory.update.utils.UpdateUtils;
import org.artifactory.update.v122rc0.JcrExporter;
import org.artifactory.utils.ExceptionUtils;
import org.artifactory.utils.PathUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by IntelliJ IDEA. User: freds
 */
public class ArtifactoryUpdate {
    private final static Logger log = Logger.getLogger(ArtifactoryUpdate.class);

    public static final String EXPORT_ALL_NON_CACHED_REPOS = "all-non-cached";
    private static final String WEBAPPS_ARTIFACTORY_WAR = "webapps/artifactory.war";
    private static final String LIB_ARTIFACTORY_CORE = "WEB-INF/lib/artifactory-core";

    private static final OptionParser paramsParser = new UpdateOptionParser();

    public static class UpdateOptionParser extends OptionParser {
        @Override
        public void usage() {
            final StringBuilder builder =
                    new StringBuilder("Usage: \njava ").append(ArtifactoryUpdate.class.getName())
                            .append("\n");
            Option[] optionList = UpdateOption.values();
            addOptionDescription(builder, optionList);
            builder.append("The parameter ").append(UpdateOption.home.argValue()).
                    append(" is mandatory.\n");
            //builder.append("One of ").append(UpdateOption.home.argValue()).
            //        append(" or ").append(UpdateOption.backup.argValue()).
            //        append(" parameter is mandatory.\n");
            //builder.append("The Artifactory version is not needed for ").
            //        append(UpdateOption.backup.argValue()).append(" option.\n");
            builder.append("The Artifactory version will be extracted from ${artifactory.home}/").
                    append(WEBAPPS_ARTIFACTORY_WAR).append(" if present\n");
            builder.append("If the war file is not located there, please do:\n");
            builder.append("1) link or copy it at this location, or pass the version.\n");
            builder.append("2) pass one of the following version as second parameter:\n");
            ArtifactoryVersion[] versions = ArtifactoryVersion.values();
            int i = 0;
            for (ArtifactoryVersion version : versions) {
                builder.append("  ").append(version.getValue());
                if (i % 5 == 4) {
                    builder.append("\n");
                }
                i++;
            }
            builder.append("\n");
            System.out.println(builder.toString());
            System.exit(-1);
        }

        @Override
        public Option getOption(String value) {
            return UpdateOption.valueOf(value);
        }
    }

    /**
     * Main function, starts the Artifactory migration process.
     */
    public static void main(String[] args) {
        int returnValue = 0;
        try {
            paramsParser.analyzeParameters(args);

            if (UpdateOption.help.isSet()) {
                usage();
            }

            paramsParser.checkExclusive(UpdateOption.convert, UpdateOption.noconvert);
            paramsParser.checkExclusive(UpdateOption.repo, UpdateOption.norepo);

            if (UpdateOption.home.isSet()) {
                exportFromDB();
            } else {
                usage();
            }
        } catch (Exception e) {
            System.err.println("Problem during Artifactory migration: " + e);
            e.printStackTrace();
            returnValue = -1;
        }
        System.exit(returnValue);
    }

    /**
     * You never return from usage since it throws System.exit(). But for good static analysis may
     * be Neal is rigth about the Nothing type?
     */
    private static void usage() {
        paramsParser.usage();
    }

    private static void exportFromDB()
            throws IOException, ParserConfigurationException, SAXException {
        ArtifactoryHome.setReadOnly(true);
        File artifactoryHomeFile = getArtifactoryHome();
        // Don't create artifactory home yet, we need to convert the config file

        // Find the version
        final ArtifactoryVersion version = getArtifactoryVersion(artifactoryHomeFile);

        // If nothing set between norepo and repo set to all repo by default
        if (!UpdateOption.repo.isSet() && !UpdateOption.norepo.isSet()) {
            UpdateOption.repo.setValue(EXPORT_ALL_NON_CACHED_REPOS);
        }

        // Above v130beta1 no need for convert
        if (version.ordinal() >= ArtifactoryVersion.v130beta1.ordinal()) {
            if (!UpdateOption.convert.isSet() && !UpdateOption.noconvert.isSet()) {
                UpdateOption.noconvert.set();
            }
        } else {
            if (!UpdateOption.convert.isSet() && !UpdateOption.noconvert.isSet()) {
                UpdateOption.convert.set();
            }
        }

        // Set the system properties instead of artifactory.properties file
        System.setProperty("artifactory.version", version.getValue());
        System.setProperty("artifactory.revision", "" + version.getRevision());

        VersionsHolder.setOriginalVersion(version);
        VersionsHolder.setFinalVersion(ArtifactoryVersion.getCurrent());
        UpdateUtils.initArtifactoryHome(artifactoryHomeFile);

        AbstractApplicationContext context = null;
        try {
            context = UpdateUtils.getSpringContext();
            //Main job
            export(context);
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
                        "Please refer to http://wiki.jfrog.org/confluence/display/RTF/Upgrading+Artifactory");
                throw saxException;
            }
            throw e;
        } finally {
            if (context != null) {
                context.destroy();
            }
        }
    }

    /*private static void exportFromBackupFolder()
            throws IOException, ParserConfigurationException, SAXException {
        File backupFolder = new File(UpdateOption.backup.getValue());
        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            throw new RuntimeException(
                    "The parameter " + UpdateOption.backup.argValue() + " " +
                            UpdateOption.backup.getValue() + " should point to a directory\n" +
                            "But it does not exists or is not a directory.");
        }
        // If no specific conversion is set activate all of them
        if (!UpdateOption.convert.isSet() &&
                !UpdateOption.config.isSet() &&
                !UpdateOption.security.isSet()) {
            UpdateOption.convert.set();
            UpdateOption.config.set();
            UpdateOption.security.set();
        }
        if (UpdateOption.config.isSet()) {
            File oldConfigFile = new File(backupFolder, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
            File newConfigFile = ArtifactoryConfigUpdate.convertConfigFile(oldConfigFile);
            FileUtils.switchFiles(oldConfigFile, newConfigFile);
        }
        if (UpdateOption.convert.isSet()) {
            ArtifactoryConfigUpdate.migrateLocalRepoToVirtual(backupFolder);
        }
        if (UpdateOption.security.isSet()) {
        }
    }*/

    private static void export(ApplicationContext context)
            throws IOException, SAXException, ParserConfigurationException {
        if (!UpdateOption.dest.isSet()) {
            UpdateOption.dest.setValue("tmpExport");
        }
        File exportDir = new File(UpdateOption.dest.getValue());
        if (!exportDir.exists()) {
            exportDir.mkdir();
        }
        log.info("Doing an export on dir=[" + exportDir.getAbsolutePath() + "]");
        StatusHolder status = new StatusHolder();
        File result;
        ImportableExportable securityExporter = UpdateUtils.getSecurityExporter(context);
        ExportSettings settings = new ExportSettings(exportDir);
        if (UpdateOption.security.isSet()) {
            // only export the security settings
            securityExporter.exportTo(settings, status);
            result = (File) status.getCallback();
            log.info("Did a security export into the file [" + result.getAbsolutePath() + "]");
            // Cannot have convert here for sure
            UpdateOption.noconvert.set();
        } else {
            ConfigExporter configExporter = UpdateUtils.getCentralConfigExporter(context);
            configExporter.exportTo(settings, status);
            securityExporter.exportTo(settings, status);
            if (UpdateOption.repo.isSet()) {
                JcrExporter jcrExporter = UpdateUtils.getJcrExporter(context);
                if (!UpdateOption.repo.getValue().equals(EXPORT_ALL_NON_CACHED_REPOS)) {
                    List<String> repos = PathUtils.delimitedListToStringList(
                            UpdateOption.repo.getValue(), ",", "\r\n\t\f ");
                    jcrExporter.setRepositoriesToExport(repos);
                } else if (UpdateOption.caches.isSet()) {
                    jcrExporter.setIncludeCaches(true);
                }

                jcrExporter.exportTo(settings, status);
            }
            result = exportDir;
            log.info("Did a full export in [" + result.getAbsolutePath() + "]");
        }

        if (UpdateOption.noconvert.isSet()) {
            log.info("Param " + UpdateOption.noconvert.argValue() +
                    " passed! No conversion of local repository to virtual done.");
        } else {
            ArtifactoryConfigUpdate.migrateLocalRepoToVirtual(exportDir);
        }
    }


    /**
     * This method is for the sake of supressing unchecked assignment
     *
     * @param e
     * @return
     */
    private static SAXParseException extractSaxException(BeansException e) {
        return (SAXParseException) ExceptionUtils.getCauseOfTypes(e, SAXParseException.class);
    }

    private static ArtifactoryVersion getArtifactoryVersion(File artifactoryHomeFile)
            throws IOException {
        String versionName = null;
        if (UpdateOption.version.isSet()) {
            versionName = UpdateOption.version.getValue();
        } else {
            log.info("Finding version...");
            File warFile = new File(artifactoryHomeFile, WEBAPPS_ARTIFACTORY_WAR);
            if (!warFile.exists()) {
                log.error("War file " + warFile +
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
                log.error("Did not find the version in " + warFile + " looking for " +
                        LIB_ARTIFACTORY_CORE);
                usage();
            }
            log.info("Found version name " + versionName);
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
            log.error(
                    "Version " + versionName + " is wrong or is not supported by this updater");
            log.error("If you know a good close version, please give a version param");
            usage();
            // Too avoid the may have NPE below
            return null;
        }
        if (version.isCurrentVersion()) {
            log.error("Version " + versionName + " is the latest version, no update needed");
            log.error("If you know it's an old version, please give a version param");
            usage();
        }
        log.info("Found supported version " + version.getValue() + " revision " +
                version.getRevision());
        // If the version is before the xs:id usage print a warning message
        return version;
    }

    private static File getArtifactoryHome() {
        if (System.getProperty(ArtifactoryHome.SYS_PROP) != null) {
            log.error(
                    "Artifactory Update should not be called with -Dartifactory.home=XXX set.");
            log.error("Artifactory Home is the first application parameter.");
            usage();
        }
        String artifactoryHome = UpdateOption.home.getValue();
        File artifactoryHomeDir = new File(artifactoryHome);
        if (!artifactoryHomeDir.exists() || !artifactoryHomeDir.isDirectory()) {
            log.error("Artifactory home " + artifactoryHomeDir.getAbsolutePath() +
                    " does not exists or is not a directory.");
            usage();
        }
        ArtifactoryHome.setHomeDir(artifactoryHomeDir);

        log.info(
                "Will migrate Artifactory Home dir=[" + artifactoryHomeDir.getAbsolutePath() + "]");
        return artifactoryHomeDir;
    }
}
