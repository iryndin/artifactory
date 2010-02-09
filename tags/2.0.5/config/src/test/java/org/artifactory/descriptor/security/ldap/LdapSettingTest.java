package org.artifactory.descriptor.security.ldap;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the LdapSetting.
 *
 * @author Yossi Shaul
 */
@Test
public class LdapSettingTest {

    public void defaultConstructor() {
        LdapSetting ldap = new LdapSetting();

        Assert.assertNull(ldap.getKey());
        Assert.assertNull(ldap.getLdapUrl());
        Assert.assertNull(ldap.getUserDnPattern());
        Assert.assertNull(ldap.getSearch());
    }

}
