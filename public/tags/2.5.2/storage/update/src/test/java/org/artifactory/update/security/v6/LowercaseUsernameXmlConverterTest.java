/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.update.security.v6;

import com.google.common.collect.Lists;
import org.artifactory.update.security.SecurityConverterTest;
import org.jdom.Document;
import org.jdom.Element;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test the lowercase username converter
 *
 * @author Tomer Cohen
 */
@Test
public class LowercaseUsernameXmlConverterTest extends SecurityConverterTest {

    public void convertValidFile() throws Exception {
        String fileMetadata = "/security/v6/security.upperlowercase.xml";
        Document document = convertXml(fileMetadata, new LowercaseUsernameXmlConverter());
        Element rootElement = document.getRootElement();
        Element child = rootElement.getChild("users");
        List children = child.getChildren("user");

        assertEquals(children.size(), 4, "There should only be 4 users left");
        Element firstUser = getElementByUserName(children, "toto");
        String firstUserName = firstUser.getChild("username").getText();
        assertEquals(firstUserName, "toto");
        Assert.assertNull(firstUser.getChild("email"), "email child in xml should be null");
        Element singleUser = getElementByUserName(children, "singleuser");
        String singleUserName = singleUser.getChild("username").getText();
        assertEquals(singleUserName, "singleuser");
        List singleUserGroups = singleUser.getChild("groups").getChildren("userGroup");
        assertEquals(singleUserGroups.size(), 1, "Should contain one group");
        Element singleUserGroup = (Element) singleUserGroups.get(0);
        assertEquals(singleUserGroup.getText(), "group3", "Should contain one group");
        Element secondUser = getElementByUserName(children, "admin");
        String secondUserName = secondUser.getChild("username").getText();
        assertEquals(secondUserName, "admin");
        Element thirdUser = getElementByUserName(children, "momo");
        String thirdUserName = thirdUser.getChild("username").getText();
        assertEquals(thirdUserName, "momo");
        Element isAdmin = thirdUser.getChild("admin");
        assertTrue(Boolean.parseBoolean(isAdmin.getText()), "user momo should be admin");
        Element emailElement = thirdUser.getChild("email");
        assertEquals(emailElement.getText(), "momo@momo.com", "user emails do not match");
        List firstUserGroups = firstUser.getChild("groups").getChildren("userGroup");
        assertEquals(firstUserGroups.size(), 3, "User should belong to three groups");

        List<String> groupNames = Lists.newArrayList();
        for (Object userGroup : firstUserGroups) {
            Element userGroupElement = (Element) userGroup;
            groupNames.add(userGroupElement.getText());
        }

        assertTrue(groupNames.contains("group3"));
        assertTrue(groupNames.contains("group1"));
        assertTrue(groupNames.contains("group2"));

        Element groups = rootElement.getChild("groups");
        List listOfGroups = groups.getChildren("group");
        Element secondGroup = (Element) listOfGroups.get(0);
        assertTrue(Boolean.parseBoolean(secondGroup.getChild("newUserDefault").getText()), "Group should be auto-join");
        List thirdUserGroups = thirdUser.getChild("groups").getChildren("userGroup");
        assertEquals(thirdUserGroups.size(), 1, "User should belong to one group");
        Element thirdUserGroup = (Element) thirdUserGroups.get(0);
        assertEquals(thirdUserGroup.getText(), "group2");

        Element aclsElement = rootElement.getChild("acls");
        List acls = aclsElement.getChildren("acl");
        Element acl = (Element) acls.get(1);
        Element aces = acl.getChild("aces");
        List ace = aces.getChildren("ace");
        Element momoAce = (Element) ace.get(3);
        assertEquals(momoAce.getChild("principal").getText(), "momo");
        Element groupTwoElement = (Element) ace.get(4);
        assertEquals(groupTwoElement.getChild("principal").getText(), "group2");
        assertEquals(groupTwoElement.getChild("mask").getText(), "11");
    }

    private Element getElementByUserName(List users, String userName) {
        for (Object user : users) {
            Element userElement = (Element) user;
            String xmlUserName = userElement.getChild("username").getText();
            if (userName.equals(xmlUserName)) {
                return userElement;
            }
        }
        return null;
    }
}
