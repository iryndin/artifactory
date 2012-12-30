package org.artifactory.rest.resource.nuget;

import com.sun.jersey.multipart.FormDataMultiPart;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.NuGetAddon;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.NuGetResourceConstants;
import org.artifactory.api.security.AuthorizationService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * NuGet resource
 *
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(NuGetResourceConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class NuGetResource {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    /**
     * Handles the tool's test request (simple GET request made to the NuGet repo)
     *
     * @param repoKey Key of NuGet supporting repository
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response test(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetTestRequest(repoKey);
    }

    /**
     * Returns the NuPkg metadata descriptor XML (static XML file)
     *
     * @param repoKey Key of NuGet supporting repository
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}/{metadataParam: \\$metadata}")
    @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getMetadataDescriptor(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetMetadataDescriptorRequest(repoKey);
    }

    /**
     * Handles query requests
     *
     * @param repoKey     Key of NuGet supporting repository
     * @param actionParam Optional sub-action parameter ($count)
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}/Search(){separator: [/]*}{actionParam: .*}")
    @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response query(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey,
            @Nullable @PathParam("actionParam") String actionParam) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetQueryRequest(request, repoKey, actionParam);
    }

    /**
     * Handles packages requests
     *
     * @param repoKey Key of NuGet supporting repository
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}/Packages()")
    @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getPackages(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetPackagesRequest(request, repoKey);
    }

    /**
     * Handles search requests for packages by ID
     *
     * @param repoKey Key of NuGet supporting repository
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}/FindPackagesById()")
    @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response findPackagesById(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleFindPackagesByIdRequest(request, repoKey);
    }

    /**
     * Handles search requests for package updates
     *
     * @param repoKey     Key of NuGet supporting repository
     * @param actionParam Optional sub-action parameter ($count)
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}/GetUpdates(){separator: [/]*}{actionParam: .*}")
    @Produces({MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getUpdates(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey,
            @Nullable @PathParam("actionParam") String actionParam) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleGetUpdatesRequest(request, repoKey, actionParam);
    }

    /**
     * Handles download requests
     *
     * @param repoKey        Key of NuGet supporting repository
     * @param packageId      ID of requested package
     * @param packageVersion Version of requested package
     * @return Response
     */
    @GET
    @Path("{repoKey: [^/]+}/{downloadIdentifier: [Dd]ownload|package}/{packageId: .+}/{packageVersion: .+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey,
            @PathParam(NuGetAddon.PACKAGE_ID_PARAM) String packageId,
            @PathParam(NuGetAddon.PACKAGE_VERSION_PARAM) String packageVersion) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetDownloadRequest(response, repoKey, packageId,
                packageVersion);
    }

    /**
     * Handles delete requests
     *
     * @param repoKey        Key of NuGet supporting repository
     * @param packageId      ID of package to delete
     * @param packageVersion Version of package to delete
     * @return Response
     */
    @DELETE
    @Path("{repoKey: [^/]+}/{packageId: .+}/{packageVersion: .+}")
    @Produces(MediaType.TEXT_HTML)
    public Response delete(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey,
            @PathParam(NuGetAddon.PACKAGE_ID_PARAM) String packageId,
            @PathParam(NuGetAddon.PACKAGE_VERSION_PARAM) String packageVersion) {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetDeleteRequest(repoKey, packageId,
                packageVersion);
    }

    /**
     * Handles publish request
     *
     * @param repoKey Key of NuGet supporting repository
     * @param data    Request form multipart data
     * @return Response
     */
    @PUT
    @Path("{repoKey: [^/]+}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response publish(@PathParam(NuGetAddon.REPO_KEY_PARAM) String repoKey, FormDataMultiPart data)
            throws IOException {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        return addonsManager.addonByType(RestAddon.class).handleNuGetPublishRequest(repoKey, data);
    }
}