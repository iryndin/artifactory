package org.artifactory.repo;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.artifactory.common.ArtifactoryProperties;
import org.artifactory.common.ConstantsValue;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Yoav Landman
 */
public class HttpRepoTest {
    InternalRepositoryService internalRepoService = EasyMock.createMock(InternalRepositoryService.class);

    @BeforeClass
    public void setup() {
        ArtifactoryProperties.get().setProperty(ConstantsValue.artifactoryVersion.getPropertyName(), "test");
    }

    @Test
    public void testProxyRemoteAuthAndMultihome() {
        ProxyDescriptor proxyDescriptor = new ProxyDescriptor();
        proxyDescriptor.setHost("proxyHost");
        proxyDescriptor.setUsername("proxy-username");
        proxyDescriptor.setPassword("proxy-password");

        HttpRepoDescriptor httpRepoDescriptor = new HttpRepoDescriptor();
        httpRepoDescriptor.setUrl("http://test");

        httpRepoDescriptor.setProxy(proxyDescriptor);

        httpRepoDescriptor.setUsername("repo-username");
        httpRepoDescriptor.setPassword("repo-password");

        httpRepoDescriptor.setLocalAddress("0.0.0.0");

        HttpRepo httpRepo = new HttpRepo(internalRepoService, httpRepoDescriptor, false);
        HttpClient client = httpRepo.createHttpClient();

        Credentials proxyCredentials = client.getState().getProxyCredentials(AuthScope.ANY);
        Assert.assertNotNull(proxyCredentials);
        Assert.assertTrue(proxyCredentials instanceof UsernamePasswordCredentials,
                "proxyCredentials are of the wrong class");
        Assert.assertEquals(((UsernamePasswordCredentials) proxyCredentials).getUserName(), "proxy-username");
        Assert.assertEquals(((UsernamePasswordCredentials) proxyCredentials).getPassword(), "proxy-password");

        Credentials repoCredentials = client.getState().getCredentials(
                new AuthScope("test", AuthScope.ANY_PORT, AuthScope.ANY_REALM));
        Assert.assertNotNull(repoCredentials);
        Assert.assertTrue(repoCredentials instanceof UsernamePasswordCredentials,
                "repoCredentials are of the wrong class");
        Assert.assertEquals(((UsernamePasswordCredentials) repoCredentials).getUserName(), "repo-username");
        Assert.assertEquals(((UsernamePasswordCredentials) repoCredentials).getPassword(), "repo-password");

        Assert.assertEquals(client.getHostConfiguration().getLocalAddress().getHostAddress(), "0.0.0.0");
    }
}