package org.artifactory.test.ldap;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.security.ldap.LdapConnectionTester;
import org.springframework.ldap.CommunicationException;
import org.springframework.security.BadCredentialsException;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class LdapConnectionTesterTest {
    protected LdapServer ldap;
    private String validLdapUrl = "ldap://localhost:5555/dc=jfrog,dc=org";
    private LdapConnectionTester ldapTester;

    @BeforeClass
    public void startLdapServer() throws Exception {
        ldap = new LdapServer(5555);
        ldap.start();
        ldap.importLdif("/ldap/users.ldif");
        ldapTester = new LdapConnectionTester();
    }

    @AfterClass
    public void stopLdapServer() {
        ldap.shutdown();
    }

    public void noUserDnAndNoSearchFilter() throws Exception {
        LdapSetting ldapSetting = new LdapSetting();
        StatusHolder status = ldapTester.testLdapConnection(ldapSetting, "na", "na");

        assertTrue(status.isError(), "Connection should have failed");
    }

    public void connectionFailure() throws Exception {
        LdapSetting ldapSetting = createValidSearchSettings();
        ldapSetting.setLdapUrl("ldap://noserverhere:389");
        StatusHolder status = ldapTester.testLdapConnection(ldapSetting, "yossis", "yossis");

        assertTrue(status.isError(), "Connection should have failed");
        assertNotNull(status.getException(), "Connection failed but status holds no exception");
        assertEquals(status.getException().getClass(), CommunicationException.class,
                "Wrong exception");
    }

    public void validConnectionSettingsWithUserDn() throws Exception {
        LdapSetting ldapSetting = createValidUserDnSettings();
        StatusHolder status = ldapTester.testLdapConnection(ldapSetting, "yossis", "yossis");

        assertFalse(status.isError(), "Connection failed: " + status.getStatusMsg());
    }

    public void validConnectionSettingsWithSearchFilter() throws Exception {
        LdapSetting ldapSetting = createValidSearchSettings();
        StatusHolder status = ldapTester.testLdapConnection(ldapSetting, "yossis", "yossis");

        assertFalse(status.isError(), "Connection failed: " + status.getStatusMsg());
    }

    public void badPassword() throws Exception {
        LdapSetting ldapSetting = createValidUserDnSettings();
        StatusHolder status = ldapTester.testLdapConnection(ldapSetting, "yossis", "blabla");

        assertTrue(status.isError(), "Connection failed: " + status.getStatusMsg());
        assertNotNull(status.getException(),
                "Connection failed but status holds no exception");
        assertEquals(status.getException().getClass(), BadCredentialsException.class,
                "Wrong exception");
    }

    public void noSuchUsername() throws Exception {
        LdapSetting ldapSetting = createValidUserDnSettings();
        StatusHolder status = ldapTester.testLdapConnection(ldapSetting, "bobo", "yossis");

        assertTrue(status.isError(), "Connection failed: " + status.getStatusMsg());
        assertNotNull(status.getException(),
                "Connection failed but status holds no exception");
        assertEquals(status.getException().getClass(), BadCredentialsException.class,
                "Wrong exception");
    }

    private LdapSetting createValidUserDnSettings() {
        LdapSetting ldapSetting = new LdapSetting();
        ldapSetting.setLdapUrl(validLdapUrl);
        ldapSetting.setUserDnPattern("uid={0},ou=People");
        return ldapSetting;
    }

    private LdapSetting createValidSearchSettings() {
        LdapSetting ldapSetting = new LdapSetting();
        ldapSetting.setLdapUrl(validLdapUrl);
        SearchPattern search = new SearchPattern();
        search.setSearchFilter("uid={0}");
        ldapSetting.setSearch(search);
        return ldapSetting;
    }

}