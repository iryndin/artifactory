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
import org.artifactory.cli.main.ArtifactoryCli;
import org.artifactory.cli.main.CliOption;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
@Test(sequential = true)
public class TestCli {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(TestCli.class);

    public static final String API_ROOT = "http://localhost:8080/artifactory/api/";

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
        Random random = new Random(System.currentTimeMillis());
        String exportTo = "/tmp/test" + random.nextInt();
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "export", exportTo,
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password"
        });
        File file = new File(exportTo);
        Assert.assertTrue(file.exists() && file.isDirectory());
        File[] files = file.listFiles();
        Assert.assertTrue(files.length > 0);
        File importFrom = null;
        for (File exported : files) {
            Assert.assertFalse(exported.getName().endsWith(".tmp"));
            if (exported.isDirectory()) {
                importFrom = exported;
            }
        }
        Assert.assertNotNull(importFrom);
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "import", importFrom.getAbsolutePath(),
                "--server", "localhost:8080",
                "--syncImport",
                "--username", "admin",
                "--password", "password"
        });
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

    private void dumpAndImport(ArtifactoryVersion version, int nbUsers, int nbGroups, int nbAcls) throws IOException {
        log.debug("Dumping database of version {}", version);
        cleanOptions();
        File exportTmpDir = new File(System.getProperty("java.io.tmpdir"), "testArtifactoryDump/" + version.name());
        FileUtils.deleteDirectory(exportTmpDir);
        ArtifactoryCli.main(new String[]{
                "dump", "cli/src/test/dbs/" + version.name(),
                "--dest", exportTmpDir.getAbsolutePath(),
                "--caches",
                "--version", "1.3.0-beta-3"
        });
        File secXmlFile = new File(exportTmpDir, "security.xml");
        Assert.assertTrue(secXmlFile.exists());
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
