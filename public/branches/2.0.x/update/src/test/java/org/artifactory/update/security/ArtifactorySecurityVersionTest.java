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
package org.artifactory.update.security;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.update.test.TestUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author freds
 * @author Yossi Shaul
 */
@Test
public class ArtifactorySecurityVersionTest {
    private final static Logger log = LoggerFactory.getLogger(ArtifactorySecurityVersionTest.class);

    public void testCoverage() {
        // Check that all Artifactory versions are covered by a DB version
        ArtifactorySecurityVersion[] versions = ArtifactorySecurityVersion.values();
        Assert.assertTrue(versions.length > 0);
        assertEquals(versions[0].getComparator().getFrom(), ArtifactoryVersion.v122rc0,
                "First version should start at first supported Artifactory version");
        assertEquals(versions[versions.length - 1].getComparator().getUntil(), ArtifactoryVersion.getCurrent(),
                "Last version should be the current one");
        for (int i = 0; i < versions.length; i++) {
            ArtifactorySecurityVersion version = versions[i];
            if (i + 1 < versions.length) {
                assertEquals(version.getComparator().getUntil().ordinal(),
                        versions[i + 1].getComparator().getFrom().ordinal() - 1,
                        "Versions should ave full coverage and leave no holes in the list of Artifactory versions");
            }
        }
    }

    public void convertFromv125() throws IOException {
        File toConvert = TestUtils.getResourceAsFile("/security/v125/security.xml");
        String secXmlData = FileUtils.readFileToString(toConvert);
        String result = ArtifactorySecurityVersion.v125u1.convert(secXmlData);

        log.debug("convertFromv125 result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 3, "3 users expected");
        UserInfo admin = users.get(0);
        assertEquals(admin.getUsername(), "admin");
        assertTrue(admin.isAdmin(), "Admin is admin...");
        assertFalse(admin.isAnonymous());

        UserInfo momo = users.get(1);
        assertEquals(momo.getUsername(), "momo");
        assertFalse(momo.isAdmin());
        assertFalse(momo.isAnonymous());

        assertNull(securityInfo.getGroups(), "No groups before 130beta3");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 2);

        AclInfo acl1 = acls.get(0);
        assertEquals(acl1.getPermissionTarget().getName(), "Anything");
        assertEquals(acl1.getPermissionTarget().getRepoKey(), "ANY");
        assertEquals(acl1.getAces().size(), 1);

        AclInfo acl2 = acls.get(1);
        assertEquals(acl2.getPermissionTarget().getName(), "libs-releases:org.apache");
        assertEquals(acl2.getPermissionTarget().getRepoKey(), "libs-releases");
        assertEquals(acl2.getAces().size(), 2);
    }

    public void convertFromv125Big() throws IOException {
        File toConvert = TestUtils.getResourceAsFile("/security/v125/security-big.xml");
        String secXmlData = FileUtils.readFileToString(toConvert);
        String result = ArtifactorySecurityVersion.v125u1.convert(secXmlData);

        log.debug("convertFromv125Big result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 5, "5 users expected");
        UserInfo admin = users.get(0);
        assertEquals(admin.getUsername(), "admin");
        assertTrue(admin.isAdmin(), "Admin is admin...");
        assertFalse(admin.isAnonymous());

        // no more admins
        for (int i = 1; i < users.size(); i++) {
            UserInfo user = users.get(i);
            assertFalse(user.isAdmin(), "Not expecting more admins: " + user.getUsername());
        }

        assertNull(securityInfo.getGroups(), "No groups before 130beta3");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 5, "5 acls expected");

        AclInfo acl1 = acls.get(4);
        assertEquals(acl1.getPermissionTarget().getName(), "libs-releases:ANY");
        assertEquals(acl1.getPermissionTarget().getRepoKey(), "libs-releases");
        assertEquals(acl1.getAces().size(), 3);
    }

    public void convertFromv130beta1() throws IOException {
        File toConvert = TestUtils.getResourceAsFile("/security/v130beta1/security.xml");
        String secXmlData = FileUtils.readFileToString(toConvert);
        String result = ArtifactorySecurityVersion.v130beta2.convert(secXmlData);

        log.debug("convertFromv130beta1 result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 4, "4 users expected");
        UserInfo admin = users.get(0);
        assertEquals(admin.getUsername(), "admin");
        assertTrue(admin.isAdmin());
        assertFalse(admin.isAnonymous());

        UserInfo yossis = users.get(2);
        assertEquals(yossis.getUsername(), "yossis");
        assertFalse(yossis.isAdmin());
        assertFalse(yossis.isAnonymous());

        assertNull(securityInfo.getGroups(), "No groups before 130beta3");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 2);

        AclInfo acl1 = acls.get(0);
        assertEquals(acl1.getPermissionTarget().getName(), "Anything");
        assertEquals(acl1.getPermissionTarget().getRepoKey(), "ANY");
        assertEquals(acl1.getAces().size(), 1);

        AclInfo acl2 = acls.get(1);
        assertEquals(acl2.getPermissionTarget().getName(), "libs-releases-local:org.art");
        assertEquals(acl2.getPermissionTarget().getRepoKey(), "libs-releases-local");
        assertEquals(acl2.getAces().size(), 2);
    }

    private SecurityInfo getSecurityInfo(String result) {
        XStream xstream = new XStream();
        xstream.processAnnotations(new Class[]{SecurityInfo.class});
        SecurityInfo securityInfo = (SecurityInfo) xstream.fromXML(result);
        return securityInfo;
    }
}