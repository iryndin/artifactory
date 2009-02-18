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
        UserInfo copy = new UserInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");

        orig.setPrivateKey("myprivatekey");
        orig.setPublicKey("mypublickey");
        copy = new UserInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy),
                "Orig and copy differ after setting public/private keys");
    }
}
