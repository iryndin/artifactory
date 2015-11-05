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

package org.artifactory.repo;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.HaAddon;
import org.artifactory.addon.LayoutsCoreAddon;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.repo.http.IdleConnectionMonitorService;
import org.artifactory.repo.http.IdleConnectionMonitorServiceImpl;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.test.ArtifactoryHomeStub;
import org.easymock.EasyMock;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Yoav Landman
 */
public class HttpRepoTest extends ArtifactoryHomeBoundTest {
    private InternalRepositoryService internalRepoService;
    private IdleConnectionMonitorService idleConnectionMonitorService;
    private ArtifactoryContext contextMock;

    @BeforeClass
    public void setup() {

        bindArtifactoryHome();

        getBound().setProperty(ConstantValues.idleConnectionMonitorInterval, "10");
        getBound().setProperty(ConstantValues.disableIdleConnectionMonitoring, "false");
        idleConnectionMonitorService = new IdleConnectionMonitorServiceImpl();

        internalRepoService = EasyMock.createMock(InternalRepositoryService.class);
        AddonsManager addonsManager = EasyMock.createMock(AddonsManager.class);
        LayoutsCoreAddon layoutsCoreAddon = EasyMock.createMock(LayoutsCoreAddon.class);
        HaAddon haAddon = EasyMock.createMock(HaAddon.class);
        EasyMock.expect(addonsManager.addonByType(LayoutsCoreAddon.class)).andReturn(layoutsCoreAddon);
        EasyMock.expect(addonsManager.addonByType(HaAddon.class)).andReturn(haAddon);
        contextMock = EasyMock.createMock(InternalArtifactoryContext.class);
        EasyMock.expect(contextMock.beanForType(AddonsManager.class)).andReturn(addonsManager).anyTimes();
        EasyMock.expect(contextMock.beanForType(IdleConnectionMonitorService.class))
                .andReturn(idleConnectionMonitorService).anyTimes();
        ArtifactoryContextThreadBinder.bind(contextMock);
        EasyMock.replay(contextMock, addonsManager);
    }

    @AfterClass
    public void tearDown() {
        bindArtifactoryHome();
        ArtifactoryContextThreadBinder.unbind();
    }

    @Test
    public void testProxyRemoteAuthAndMultihome() throws IOException {
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

        HttpRepo httpRepo = new HttpRepo(httpRepoDescriptor, internalRepoService, false, null);
        CloseableHttpClient client = httpRepo.createHttpClient();
        if(client!=null) client.close();

        //TODO: [by YS] implement test on httpclient4
        /*Credentials proxyCredentials = client.getState().getProxyCredentials(AuthScope.ANY);
        Assert.assertNotNull(proxyCredentials);
        Assert.assertTrue(proxyCredentials instanceof UsernamePasswordCredentials,
                "proxyCredentials are of the wrong class");
        Assert.assertEquals(((UsernamePasswordCredentials) proxyCredentials).getUserName(), "proxy-username");
        Assert.assertEquals(proxyCredentials.getPassword(), "proxy-password");

        Credentials repoCredentials = client.getState().getCredentials(
                new AuthScope("test", AuthScope.ANY_PORT, AuthScope.ANY_REALM));
        Assert.assertNotNull(repoCredentials);
        Assert.assertTrue(repoCredentials instanceof UsernamePasswordCredentials,
                "repoCredentials are of the wrong class");
        Assert.assertEquals(((UsernamePasswordCredentials) repoCredentials).getUserName(), "repo-username");
        Assert.assertEquals(repoCredentials.getPassword(), "repo-password");

        Assert.assertEquals(client.getHostConfiguration().getLocalAddress().getHostAddress(), "0.0.0.0");*/
    }
}