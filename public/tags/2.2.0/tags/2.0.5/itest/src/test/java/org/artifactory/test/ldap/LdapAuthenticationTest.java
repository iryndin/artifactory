package org.artifactory.test.ldap;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.artifactory.test.http.ArtifactoryServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests artifactory with annonymous access disabled and ldap configured.
 * This test will start an embedded ldap server and import some users to it.
 *
 * @author Yossi Shaul
 */
@Test(enabled = true)
public class LdapAuthenticationTest {
    protected ArtifactoryServer artifactory;
    protected LdapServer ldap;

    @BeforeClass
    public void setUp() throws Exception {
        ldap = new LdapServer(10389);
        artifactory = new ArtifactoryServer("LdapTest", "ldapenabled");
        ldap.start();
        ldap.importLdif("/ldap/users.ldif");
        artifactory.start();
    }

    @AfterClass
    public void shutdown() {
        if (artifactory != null) {
            artifactory.stop();
        }
        if (ldap != null) {
            ldap.shutdown();
        }
    }

    public void notAuthorizedRequest() throws IOException {
        HttpClient client = new HttpClient();

        String url = "http://localhost:8081/artifactory/api/system";
        GetMethod getMethod = new GetMethod(url);
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_UNAUTHORIZED,
                "Annon access disabled and no credentials are sent");
    }

    public void adminDbAuthorizedRequest() throws IOException {
        HttpClient client = createHttpClient("admin", "password");
        String url = "http://localhost:8081/artifactory/repo/whatever";
        GetMethod getMethod = new GetMethod(url);
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_NOT_FOUND);
    }

    public void ldapAuthorizedRequest() throws IOException {
        HttpClient client = createHttpClient("yossis", "yossis");
        String url = "http://localhost:8081/artifactory/repo/whatever";
        GetMethod getMethod = new GetMethod(url);
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_NOT_FOUND,
                "Failed to authenticate user");
    }

    public void wrongPasswordRequest() throws IOException {
        HttpClient client = createHttpClient("yossis", "yossis123");
        String url = "http://localhost:8081/artifactory/api/system";
        GetMethod getMethod = new GetMethod(url);
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_UNAUTHORIZED);
    }

    private HttpClient createHttpClient(String username, String password) {
        HttpClient client = new HttpClient();
        Credentials admin = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, admin);
        return client;
    }

}