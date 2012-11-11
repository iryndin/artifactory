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

package org.artifactory.rest.resource.nuget;

import com.sun.jersey.multipart.FormDataMultiPart;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.spring.InternalArtifactoryContext;
import org.easymock.EasyMock;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;

/**
 * A dry test for the resource to make sure that the call is delegated to the rest addon
 *
 * @author Noam Y. Tenne
 */
@Test
public class NuGetResourceTest {

    private RestAddon restAddonMock;
    private AddonsManager addonsManagerMock;
    private InternalArtifactoryContext contextMock;

    @BeforeClass
    public void setupMocks() {
        restAddonMock = EasyMock.createMock(RestAddon.class);
        EasyMock.expect(restAddonMock.handleNuGetTestRequest(EasyMock.eq("repoKey")))
                .andReturn(Response.ok().entity("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleNuGetMetadataDescriptorRequest(EasyMock.eq("repoKey")))
                .andReturn(Response.ok().entity("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleNuGetQueryRequest(EasyMock.isA(HttpServletRequest.class),
                EasyMock.eq("repoKey"), EasyMock.eq("actionParam"))).andReturn(Response.ok("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleNuGetPackagesRequest(EasyMock.isA(HttpServletRequest.class),
                EasyMock.eq("repoKey"))).andReturn(Response.ok("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleFindPackagesByIdRequest(EasyMock.isA(HttpServletRequest.class),
                EasyMock.eq("repoKey"))).andReturn(Response.ok("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleGetUpdatesRequest(EasyMock.isA(HttpServletRequest.class),
                EasyMock.eq("repoKey"), EasyMock.eq("actionParam"))).andReturn(Response.ok("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleNuGetDownloadRequest(EasyMock.isA(HttpServletResponse.class),
                EasyMock.eq("repoKey"), EasyMock.eq("packageId"), EasyMock.eq("packageVersion")))
                .andReturn(Response.ok("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleNuGetDeleteRequest(
                EasyMock.eq("repoKey"), EasyMock.eq("packageId"), EasyMock.eq("packageVersion")))
                .andReturn(Response.ok("DAMN STRAIGHT!").build());

        EasyMock.expect(restAddonMock.handleNuGetPublishRequest(EasyMock.eq("repoKey"),
                EasyMock.isA(FormDataMultiPart.class))).andReturn(Response.ok("DAMN STRAIGHT!").build());

        addonsManagerMock = EasyMock.createMock(AddonsManager.class);
        EasyMock.expect(addonsManagerMock.addonByType(RestAddon.class)).andReturn(restAddonMock).anyTimes();

        contextMock = EasyMock.createMock(InternalArtifactoryContext.class);
        EasyMock.expect(contextMock.beanForType(AddonsManager.class)).andReturn(addonsManagerMock).anyTimes();

        EasyMock.replay(contextMock, addonsManagerMock, restAddonMock);
        ArtifactoryContextThreadBinder.bind(contextMock);
    }

    @AfterClass
    public void verifyMocks() {
        EasyMock.verify(contextMock, addonsManagerMock, restAddonMock);
    }

    @Test
    public void testNuGetTestMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.test("repoKey");
        verifyResponse(response);
    }

    @Test
    public void testMetadataDescriptorMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.getMetadataDescriptor("repoKey");
        verifyResponse(response);
    }

    @Test
    public void testQueryMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.query("repoKey", "actionParam");
        verifyResponse(response);
    }

    @Test
    public void testPackagesQueryMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.getPackages("repoKey");
        verifyResponse(response);
    }

    @Test
    public void testFindPackagesByIdQueryMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.findPackagesById("repoKey");
        verifyResponse(response);
    }

    @Test
    public void testGetUpdatesQueryMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.getUpdates("repoKey", "actionParam");
        verifyResponse(response);
    }

    @Test
    public void testDownloadMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.download("repoKey", "packageId", "packageVersion");
        verifyResponse(response);
    }

    @Test
    public void testDeleteMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.delete("repoKey", "packageId", "packageVersion");
        verifyResponse(response);
    }

    @Test
    public void testPublishMethodCallsAddon() throws Exception {
        NuGetResource nuGetResource = getNuGetResourceInstance();
        Response response = nuGetResource.publish("repoKey", new FormDataMultiPart());
        verifyResponse(response);
    }

    @Test
    public void testTestMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("test", String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}", "Unexpected resource path pattern.");
    }

    @Test
    public void testMetadataDescriptorMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("getMetadataDescriptor", String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}/{metadataParam: \\$metadata}",
                "Unexpected resource path pattern.");
    }

    @Test
    public void testQueryMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("query", String.class, String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}/Search(){separator: [/]*}{actionParam: .*}",
                "Unexpected resource path pattern.");
    }

    @Test
    public void testPackagesMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("getPackages", String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}/Packages()", "Unexpected resource path pattern.");
    }

    @Test
    public void tesFindPackagesByIdMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("findPackagesById", String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}/FindPackagesById()",
                "Unexpected resource path pattern.");
    }

    @Test
    public void testGetUpdatesMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("getUpdates", String.class, String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}/GetUpdates(){separator: [/]*}{actionParam: .*}",
                "Unexpected resource path pattern.");
    }

    @Test
    public void testDownloadMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("download", String.class, String.class, String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(),
                "{repoKey: [^/]+}/{downloadIdentifier: [Dd]ownload|package}/{packageId: .+}/{packageVersion: .+}",
                "Unexpected resource path pattern.");
    }

    @Test
    public void testDeleteMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("delete", String.class, String.class, String.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}/{packageId: .+}/{packageVersion: .+}",
                "Unexpected resource path pattern.");
    }

    @Test
    public void testPublishMethodPathAnnotation() throws Exception {
        Method restMethod = NuGetResource.class.getDeclaredMethod("publish", String.class, FormDataMultiPart.class);
        Path pathAnnotation = restMethod.getAnnotation(Path.class);
        assertEquals(pathAnnotation.value(), "{repoKey: [^/]+}", "Unexpected resource path pattern.");
    }

    private NuGetResource getNuGetResourceInstance() throws NoSuchFieldException, IllegalAccessException {
        NuGetResource nuGetResource = new NuGetResource();
        Field request = NuGetResource.class.getDeclaredField("request");
        Field response = NuGetResource.class.getDeclaredField("response");
        request.setAccessible(true);
        response.setAccessible(true);
        request.set(nuGetResource, EasyMock.createMock(HttpServletRequest.class));
        response.set(nuGetResource, EasyMock.createMock(HttpServletResponse.class));
        return nuGetResource;
    }

    private void verifyResponse(Response response) {
        assertEquals(response.getStatus(), HttpStatus.SC_OK, "Unexpected response status.");
        assertEquals(response.getEntity(), "DAMN STRAIGHT!", "Unexpected response entity.");
    }
}
