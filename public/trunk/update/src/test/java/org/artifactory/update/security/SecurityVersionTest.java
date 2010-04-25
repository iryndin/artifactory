/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.update.security;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.UserInfo;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ResourceUtils;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionTest;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author freds
 * @author Yossi Shaul
 */
@Test
public class SecurityVersionTest extends VersionTest {
    private static final Logger log = LoggerFactory.getLogger(SecurityVersionTest.class);

    @Override
    protected SubConfigElementVersion[] getVersions() {
        return SecurityVersion.values();
    }

    public void convertFromVersion1() throws IOException {
        XStream xstream = XStreamFactory.create(PermissionTargetInfo.class);
        log.debug(xstream.toXML(new PermissionTargetInfo("blbla", Arrays.asList("repo"))));

        File toConvert = ResourceUtils.getResourceAsFile("/security/v1/security.xml");
        String secXmlData = FileUtils.readFileToString(toConvert, "utf-8");
        String result = SecurityVersion.v1.convert(secXmlData);

        log.debug("convertFromv125 result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 3, "3 users expected");
        UserInfo admin = users.get(users.indexOf(new UserInfo("admin")));
        assertEquals(admin.getUsername(), "admin");
        assertTrue(admin.isAdmin(), "Admin is admin...");
        assertFalse(admin.isAnonymous());

        UserInfo momo = users.get(users.indexOf(new UserInfo("momo")));
        assertEquals(momo.getUsername(), "momo");
        assertFalse(momo.isAdmin());
        assertFalse(momo.isAnonymous());

        assertNull(securityInfo.getGroups(), "No groups before 130beta3");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 3);

        AclInfo acl1 = acls.get(0);
        assertEquals(acl1.getPermissionTarget().getName(), "Anything");
        assertEquals(acl1.getPermissionTarget().getRepoKeys(), Arrays.asList("ANY"));
        assertEquals(acl1.getAces().size(), 1);

        AclInfo acl2 = acls.get(1);
        assertEquals(acl2.getPermissionTarget().getName(), "libs-releases:org.apache");
        assertEquals(acl2.getPermissionTarget().getRepoKeys(), Arrays.asList("libs-releases"));
        assertEquals(acl2.getAces().size(), 2);

        // any remote added
        AclInfo acl3 = acls.get(2);
        assertEquals(acl3.getPermissionTarget().getName(), "Any Remote");
        assertEquals(acl3.getPermissionTarget().getRepoKeys(), Arrays.asList("ANY REMOTE"));
        assertEquals(acl3.getAces().size(), 1);
    }

    public void convertFromVersion1Big() throws IOException {
        File toConvert = ResourceUtils.getResourceAsFile("/security/v1/security-big.xml");
        String secXmlData = FileUtils.readFileToString(toConvert, "utf-8");
        String result = SecurityVersion.v1.convert(secXmlData);

        log.debug("convertFromv125Big result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 5, "5 users expected");
        int indexOfAdmin = users.indexOf(new UserInfo("admin"));
        UserInfo admin = users.get(indexOfAdmin);
        assertEquals(admin.getUsername(), "admin");
        assertTrue(admin.isAdmin(), "Admin is admin...");
        assertFalse(admin.isAnonymous());

        // no more admins
        for (int i = 0; i != indexOfAdmin && i < users.size(); i++) {
            UserInfo user = users.get(i);
            assertFalse(user.isAdmin(), "Not expecting more admins: " + user.getUsername());
        }

        assertNull(securityInfo.getGroups(), "No groups before 130beta3");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 6, "6 acls expected");

        AclInfo acl1 = acls.get(4);
        assertEquals(acl1.getPermissionTarget().getName(), "libs-releases:ANY");
        assertEquals(acl1.getPermissionTarget().getRepoKeys(), Arrays.asList("libs-releases"));
        assertEquals(acl1.getAces().size(), 3);
    }

    public void convertFromVersion2() throws IOException {
        File toConvert = ResourceUtils.getResourceAsFile("/security/v2/security.xml");
        String secXmlData = FileUtils.readFileToString(toConvert, "utf-8");
        String result = SecurityVersion.v2.convert(secXmlData);

        log.debug("convertFromv130beta1 result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 4, "4 users expected");
        UserInfo admin = users.get(users.indexOf(new UserInfo("admin")));
        assertEquals(admin.getUsername(), "admin");
        assertTrue(admin.isAdmin());
        assertFalse(admin.isAnonymous());

        UserInfo yossis = users.get(users.indexOf(new UserInfo("yossis")));
        assertEquals(yossis.getUsername(), "yossis");
        assertFalse(yossis.isAdmin());
        assertFalse(yossis.isAnonymous());

        assertNull(securityInfo.getGroups(), "No groups before 130beta3");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 3);

        AclInfo acl1 = acls.get(0);
        assertEquals(acl1.getPermissionTarget().getName(), "Anything");
        assertEquals(acl1.getPermissionTarget().getRepoKeys(), Arrays.asList("ANY"));
        assertEquals(acl1.getAces().size(), 1);

        AclInfo acl2 = acls.get(1);
        assertEquals(acl2.getPermissionTarget().getName(), "libs-releases-local:org.art");
        assertEquals(acl2.getPermissionTarget().getRepoKeys(), Arrays.asList("libs-releases-local"));
        assertEquals(acl2.getAces().size(), 2);
    }

    public void convertFromVersion3() throws IOException {
        File toConvert = ResourceUtils.getResourceAsFile("/security/v3/security.xml");
        String secXmlData = FileUtils.readFileToString(toConvert, "utf-8");
        String result = SecurityVersion.v3.convert(secXmlData);

        log.debug("convertFromv130beta1 result:\n{}", result);

        SecurityInfo securityInfo = getSecurityInfo(result);

        List<UserInfo> users = securityInfo.getUsers();
        assertEquals(users.size(), 5, "5 users expected");
        assertEquals(securityInfo.getGroups().size(), 2, "2 groups expected");

        List<AclInfo> acls = securityInfo.getAcls();
        assertEquals(acls.size(), 5, "Any remote should have been added");

        // any remote added
        AclInfo acl3 = acls.get(4);
        assertEquals(acl3.getPermissionTarget().getName(), "Any Remote");
        assertEquals(acl3.getPermissionTarget().getRepoKeys(), Arrays.asList("ANY REMOTE"));
        assertEquals(acl3.getAces().size(), 1);
    }

    private SecurityInfo getSecurityInfo(String result) {
        XStream xstream = XStreamFactory.create(SecurityInfo.class);
        SecurityInfo securityInfo = (SecurityInfo) xstream.fromXML(result);
        return securityInfo;
    }
}