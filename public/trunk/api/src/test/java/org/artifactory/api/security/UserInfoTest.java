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

package org.artifactory.api.security;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests the UserInfo.
 *
 * @author Yossi Shaul
 */
@Test
public class UserInfoTest {

    public void copyConstructor() {
        UserInfo orig = new UserInfo("momo");
        orig.setGenPasswordKey("blablablablabla");
        UserInfo copy = new UserInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");

        orig.setPrivateKey("myprivatekey");
        orig.setPublicKey("mypublickey");
        copy = new UserInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy),
                "Orig and copy differ after setting public/private keys");
    }

    public void addRemoveGroup() {
        UserInfo userInfo = new UserInfo("momo");
        userInfo.addGroup("mygroup");
        Assert.assertTrue(userInfo.getGroups().contains(new UserInfo.UserGroupInfo("mygroup")));
        userInfo.removeGroup("mygroup");
        Assert.assertTrue(userInfo.getGroups().isEmpty(), "groups should now be empty");

        Set<String> strings = new HashSet<String>();
        strings.add("mygroup2");
        userInfo.setInternalGroups(strings);
        Assert.assertTrue(userInfo.getGroups().contains(new UserInfo.UserGroupInfo("mygroup2")));
    }
}
