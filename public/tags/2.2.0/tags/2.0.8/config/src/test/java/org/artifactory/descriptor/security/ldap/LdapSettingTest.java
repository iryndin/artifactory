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

    /**
     * Tests the URL protocol lower-case transformer
     */
    public void testUrlProtocolTransformer() {
        testLdapUrl("", "", "Empty LDAP URL should have remained unchanged");

        testLdapUrl("invalidLDAPURL", "invalidLDAPURL", "Inavlid LDAP URL should have remained unchanged");

        testLdapUrl("LDAP://url", "ldap://url", "Upper-cased LDAP URL should have been transformed to lower case");

        testLdapUrl("LDAP://URL", "ldap://URL", "Upper-cased LDAP URL should have been transformed to lower case");

        testLdapUrl("ldap://url", "ldap://url", "Valid LDAP URL should have remained unchanged");

        testLdapUrl("ldap://URL", "ldap://URL", "Valid LDAP URL should have remained unchanged");

        testLdapUrl("ldap://URL:30/moo=MOO", "ldap://URL:30/moo=MOO", "Valid LDAP URL should have remained unchanged");

        testLdapUrl("ldap://URL:30/moo=MOO,maa=MAA", "ldap://URL:30/moo=MOO,maa=MAA",
                "Valid LDAP URL should have remained unchanged");
    }

    /**
     * Tests the LDAP URL getter & setter (needed since URL is being transformed)
     *
     * @param inputUrl     URL to supply to setter
     * @param outputUrl    URL to compare with results from getter
     * @param errorMessage Message to print on unexpected result
     */
    private void testLdapUrl(String inputUrl, String outputUrl, String errorMessage) {
        LdapSetting ldap = new LdapSetting();
        ldap.setLdapUrl(inputUrl);
        Assert.assertEquals(ldap.getLdapUrl(), outputUrl, errorMessage);
    }
}
