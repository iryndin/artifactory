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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.thoughtworks.xstream.XStream;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.update.utils.BackupUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.ConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public class TestReadOldBackups {
    private static final Logger log =
            LoggerFactory.getLogger(TestReadOldBackups.class);

    @BeforeClass
    public void setLevel() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(TestReadOldBackups.class).setLevel(Level.DEBUG);
        lc.getLogger(ConverterUtils.class).setLevel(Level.DEBUG);
        lc.getLogger("org.artifactory.update.security").setLevel(Level.DEBUG);
    }

    @Test
    public void testReadingVersion() throws Exception {
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.isCurrent()) {
                // Nothing to do
                continue;
            }
            String backupVersionFolder = "/backups/" + version.name();
            URL resource = getClass().getResource(backupVersionFolder);
            if (resource != null) {
                File backupFolder = new File(resource.toURI());
                ArtifactoryVersion backupVersion = BackupUtils.findVersion(backupFolder);
                Assert.assertEquals(backupVersion, version);
            } else {
                log.warn("Version " + version + " does not have a backup test folder in " +
                        backupVersionFolder);
            }
        }
    }

    @Test
    public void testSecurityConversion() throws Exception {
        ArtifactoryVersion version = ArtifactoryVersion.v130beta2;
        String backupVersionFolder = "/backups/" + version.name();
        URL resource = getClass().getResource(backupVersionFolder);
        Assert.assertNotNull(resource, "Resource folder " + backupVersionFolder + " should exists");
        File backupFolder = new File(resource.toURI());
        ArtifactoryVersion backupVersion = BackupUtils.findVersion(backupFolder);
        Assert.assertEquals(backupVersion, version);
        String newSecurityXml = BackupUtils.convertSecurityFile(backupFolder, version);
        SecurityInfo securityConfig = (SecurityInfo) getXstream().fromXML(newSecurityXml);
        List<UserInfo> users = securityConfig.getUsers();
        Assert.assertEquals(users.size(), 6, "Should have 6 users");
        UserInfo totoUser = users.get(users.indexOf(new UserInfo("toto")));
        Assert.assertFalse(totoUser.isAdmin(), "Toto user should not be admin");
        UserInfo superUser = users.get(users.indexOf(new UserInfo("super")));
        Assert.assertTrue(superUser.isAdmin(), "Super user should be admin");
        List<GroupInfo> groups = securityConfig.getGroups();
        Assert.assertTrue(groups == null || groups.isEmpty(), "There should be no groups");
        List<AclInfo> acls = securityConfig.getAcls();
        Assert.assertEquals(acls.size(), 5, "There should be 5 ACLs");
        AclInfo anythingAcl = acls.get(acls.indexOf(new AclInfo(
                new PermissionTargetInfo(PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME,
                        PermissionTargetInfo.ANY_REPO))));
        Assert.assertEquals(anythingAcl.getAces().size(), 4, "Should be 4 ACE in Anything");
    }

    private static XStream getXstream() {
        XStream xstream = new XStream();
        xstream.processAnnotations(SecurityInfo.class);
        return xstream;
    }

    @Test
    public void testFolderXmlMetadata() {

    }

    @Test
    public void testFileXmlMetadata() {

    }
}
