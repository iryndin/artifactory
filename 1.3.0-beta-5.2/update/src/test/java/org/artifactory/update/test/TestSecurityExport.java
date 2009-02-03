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
package org.artifactory.update.test;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.update.VersionsHolder;
import org.artifactory.update.utils.UpdateUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * User: freds Date: Aug 14, 2008 Time: 7:58:47 PM
 */
public class TestSecurityExport {
    @Test
    public void exportFrom122rc0() throws Exception {
        ArtifactoryVersion version = ArtifactoryVersion.v122rc0;
        testExportSecurity(version);
    }

    @Test
    public void exportFrom122() throws Exception {
        ArtifactoryVersion version = ArtifactoryVersion.v122;
        testExportSecurity(version);
    }

    @Test
    public void exportFrom125() throws Exception {
        ArtifactoryVersion version = ArtifactoryVersion.v125;
        testExportSecurity(version);
    }

    @Test
    public void exportFrom125u1() throws Exception {
        ArtifactoryVersion version = ArtifactoryVersion.v125u1;
        testExportSecurity(version);
    }

    @Test
    public void exportFrom130beta2() throws Exception {
        ArtifactoryVersion version = ArtifactoryVersion.v130beta2;
        testExportSecurity(version);
    }

    private static void testExportSecurity(ArtifactoryVersion version)
            throws FileNotFoundException {
        AbstractApplicationContext ctx = null;
        File exportedSecurityFile;
        try {
            ctx = setVersionAndGetSpringContext(version);
            exportedSecurityFile = exportSecurity(ctx, version);
        } finally {
            if (ctx != null) {
                ctx.destroy();
            }
        }
        testExportedFile(exportedSecurityFile);
    }

    private static File exportSecurity(ApplicationContext ctx, ArtifactoryVersion version) {
        ImportableExportable exporter = UpdateUtils.getSecurityExporter(ctx);
        StatusHolder statusHolder = new StatusHolder();
        File exportDir = new File("target/exportFrom" + version.name());
        ExportSettings settings = new ExportSettings(exportDir);
        exporter.exportTo(settings, statusHolder);
        Assert.assertNull(statusHolder.getException());
        File exportedSecurityFile = new File(exportDir, "security.xml");
        Assert.assertTrue(exportedSecurityFile.exists(),
                "Exported file not found at " + exportedSecurityFile.getAbsolutePath());
        return exportedSecurityFile;
    }

    private static void testExportedFile(File exportedSecurityFile) throws FileNotFoundException {
        // read the security info from the generated xml
        XStream xstream = UpdateUtils.getXstream(SecurityInfo.class);
        SecurityInfo securityInfo = (SecurityInfo)
                xstream.fromXML(new FileInputStream(exportedSecurityFile));
        Assert.assertNotNull(securityInfo);
    }

    public static AbstractApplicationContext setVersionAndGetSpringContext(
            ArtifactoryVersion origVersion) {
        String testHome = System.getProperty("artifactory.test.home");
        if (testHome == null) {
            Assert.fail("artifactory.test.home system property not set");
        }
        VersionsHolder.setOriginalVersion(origVersion);
        VersionsHolder.setFinalVersion(ArtifactoryVersion.getCurrent());
        UpdateUtils
                .initArtifactoryHome(new File(testHome, "artifactory-" + origVersion.getValue()));
        return UpdateUtils.getSpringContext();
    }

}
