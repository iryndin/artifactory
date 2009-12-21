/*
 * This file is part of Artifactory.
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
}
