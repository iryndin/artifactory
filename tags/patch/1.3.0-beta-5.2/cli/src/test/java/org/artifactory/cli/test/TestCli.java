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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.cli.ArtifactoryCli;
import org.artifactory.cli.CliOption;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Random;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
public class TestCli {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = LogManager.getLogger(TestCli.class);

    private static final String API_ROOT =
            "http://localhost:8080/artifactory/api/";

    @Test
    public void testError() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "--url", API_ROOT,
                "--username",
                "--password", "password",
                "--info"
        });
    }

    @Test
    public void testInfo() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "--url", API_ROOT,
                "--username", "admin",
                "--password", "password",
                "--info"
        });
    }

    @Test
    public void testExportImport() throws Exception {
        Random random = new Random(System.currentTimeMillis());
        String exportTo = "/tmp/test" + random.nextInt();
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password",
                "--export", exportTo
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
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password",
                "--import", importFrom.getAbsolutePath()
        });
    }

    @Test
    public void testJfrogImport() throws Exception {
        cleanOptions();
        ArtifactoryCli.main(new String[]{
                "--server", "localhost:8080",
                "--username", "admin",
                "--password", "password",
                "--import", "/Users/yoavl/Documents/Projects/jfrog/jfrog.export",
                "--symlinks",
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
