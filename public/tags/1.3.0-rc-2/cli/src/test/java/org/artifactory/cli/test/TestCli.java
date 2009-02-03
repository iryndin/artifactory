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
package org.artifactory.cli.test;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.cli.main.ArtifactoryCli;
import org.artifactory.cli.main.CliOption;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
@Test(sequential = true)
public class TestCli {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(TestCli.class);

    public static final String API_ROOT = "http://localhost:8080/artifactory/api/";
    private List<MdToFind> mdExported;

    static class MdToFind {
        String uri;
        File imported;
    }

    @BeforeClass
    public void setLevel() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        //lc.getLogger(ConfigXmlConversionTest.class).setLevel(Level.DEBUG);
        lc.getLogger("org.artifactory.update").setLevel(Level.DEBUG);
        ArtifactoryCli.DO_SYSTEM_EXIT = false;
    }

    @Test
    public void testInfo() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "info",
                "--url", API_ROOT,
                "--username", "admin",
                "--password", "password"
        });
    }

    @Test
    public void testExportImport() throws Exception {
        File importFrom = exportToTemp();
        Thread.sleep(2000);
        importFromTemp(importFrom);
    }

    private void importFromTemp(File importFrom) throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "import", importFrom.getAbsolutePath(),
                "--server", "localhost:8080",
                "--syncImport",
                "--username", "admin",
                "--password", "password"
        });
        for (MdToFind mdPath : mdExported) {
            String url = "http://localhost:8080/artifactory" + mdPath.uri;
            URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.connect();
            //Assert.assertEquals(urlConnection.getContentLength(), mdPath.imported.length(), "md "+mdPath.uri+" not the same than "+mdPath.imported.getAbsolutePath());
            assertTrue(urlConnection.getContentType().contains("xml"), "URL " + url + " is not xml");
        }
    }

    private File exportToTemp() throws IOException {
        Random random = new Random(System.currentTimeMillis());
        File exportTo = new File(System.getProperty("java.io.tmpdir"), "test/" + random.nextInt());
        FileUtils.deleteDirectory(exportTo);
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "export", exportTo.getAbsolutePath(),
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password", "--m2"
        });
        assertTrue(exportTo.exists() && exportTo.isDirectory());
        File[] files = exportTo.listFiles();
        assertTrue(files.length > 0);
        File importFrom = null;
        for (File exported : files) {
            Assert.assertFalse(exported.getName().endsWith(".tmp"));
            if (exported.isDirectory()) {
                importFrom = exported;
            }
        }
        Assert.assertNotNull(importFrom);
        File secFile = new File(importFrom, "security.xml");
        File confFile = new File(importFrom, ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        File propFile = new File(importFrom, ArtifactoryHome.ARTIFACTORY_PROPERTIES_FILE);
        File repositories = new File(importFrom, "repositories");
        assertTrue(secFile.exists());
        assertTrue(confFile.exists());
        assertTrue(propFile.exists());
        assertTrue(repositories.exists());
        Collection<File> mdFiles = FileUtils.listFiles(repositories, new String[]{"xml"}, true);
        ArrayList<File> m2MDList = new ArrayList<File>();
        mdExported = new ArrayList<MdToFind>();
        boolean hasMetadataXml = false;
        boolean hasM2MetadataXml = false;
        for (File file : mdFiles) {
            assertTrue(file.length() != 0, "File " + file + " is empty!");
            if (MavenNaming.isMavenMetadataFileName(file.getName())) {
                hasMetadataXml = true;
                String path = file.getAbsolutePath().substring(repositories.getAbsolutePath().length());
                MdToFind toFind = new MdToFind();
                toFind.uri = path.replace(".artifactory-metadata", "").replace("-local", "").replace('\\', '/');
                toFind.imported = file;
                mdExported.add(toFind);
                if (!file.getParent().contains("metadata")) {
                    hasM2MetadataXml = true;
                    lookForChecksums(file);
                }
            }
        }
        assertTrue(hasMetadataXml, "Did not find any maven-metadata.xml files!");
        assertTrue(hasM2MetadataXml, "Did not find any m2 compatible maven-metadata.xml files!");

        Collection<File> artifactFiles = FileUtils.listFiles(repositories, new String[]{"pom", "jar"}, true);
        for (File file : artifactFiles) {
            assertTrue(file.length() != 0, "File " + file + " is empty!");
            lookForChecksums(file);
        }

        return importFrom;
    }

    private void lookForChecksums(File file) {
        File fileMD5 = new File(file.getAbsolutePath() + ".md5");
        File fileSHA1 = new File(file.getAbsolutePath() + ".sha1");
        Assert.assertTrue(fileMD5.exists(),
                "Exporting with m2 flag on. MD5 checksum was expected for file: " + file.getAbsolutePath());
        Assert.assertTrue(fileSHA1.exists(),
                "Exporting with m2 flag on. SHA1 checksum was expected for file: " + file.getAbsolutePath());
        assertTrue(fileMD5.length() != 0, "File " + fileMD5 + " is empty!");
        assertTrue(fileSHA1.length() != 0, "File " + fileSHA1 + " is empty!");
    }

    @Test(enabled = false)
    public void testJfrogImport() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "import", "cli/src/test/backups/current",
                "--symlinks",
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password",
                "--timeout", "3600"
        });
    }

    @Test
    public void testSimpleSecurityImport() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "help", "security"
        });
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "security",
                "--update", "cli/src/test/resources/cli/simple_security.xml",
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password"
        });
    }

    @Test
    public void testDumpAndImport130Beta3() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "help", "dump",
        });
        dumpAndImport(ArtifactoryVersion.v130beta3, 7, 3, 3);
    }

    @Test
    public void testDumpAndImport130Beta4() throws Exception {
        dumpAndImport(ArtifactoryVersion.v130beta4, 2, 0, 1);
    }

    @Test
    public void testDumpAndImport130Beta5() throws Exception {
        dumpAndImport(ArtifactoryVersion.v130beta5, 2, 0, 1);
    }

    @Test
    public void testDumpAndImport130Beta61() throws Exception {
        dumpAndImport(ArtifactoryVersion.v130beta61, 6, 0, 7);
    }

    private void dumpAndImport(ArtifactoryVersion version, int nbUsers, int nbGroups, int nbAcls) throws IOException {
        log.debug("Dumping database of version {}", version);
        cleanOptions();
        File exportTmpDir = new File(System.getProperty("java.io.tmpdir"), "testArtifactoryDump/" + version.name());
        FileUtils.deleteDirectory(exportTmpDir);
        ArtifactoryCli.main(new String[]{
                "dump", "../test/dbs/" + version.name(),
                "--dest", exportTmpDir.getAbsolutePath(),
                "--caches",
                "--version", version.getValue()
        });
        File secXmlFile = new File(exportTmpDir, "security.xml");
        assertTrue(secXmlFile.exists());
        Document doc = ConverterUtils.parse(FileUtils.readFileToString(secXmlFile));
        Assert.assertEquals(doc.getRootElement().getChild("users").getChildren("user").size(), nbUsers);
        Assert.assertEquals(doc.getRootElement().getChild("groups").getChildren("group").size(), nbGroups);
        Assert.assertEquals(doc.getRootElement().getChild("acls").getChildren("acl").size(), nbAcls);
        cleanOptions();

        ArtifactoryCli.main(new String[]{
                "import", exportTmpDir.getAbsolutePath(),
                "--syncImport",
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password",
                "--timeout", "3600"
        });
    }

    private static void cleanOptions() {
        CliOption[] cliOptions = CliOption.values();
        for (CliOption option : cliOptions) {
            option.setValue(null);
        }
    }
}
