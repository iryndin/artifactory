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

package org.artifactory.rest.resource.support;

import com.google.common.collect.Lists;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.support.SupportAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.support.config.bundle.BundleConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.FileNotFoundException;
import java.util.List;

import static org.artifactory.api.rest.constant.ArtifactRestConstants.SUPPORT_ROOT;
import static org.artifactory.api.rest.constant.ArtifactRestConstants.SUPPORT_BUNDLES_PATH;

/**
 * Provides support content generation services
 *
 * @author Michael Pasternak
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(SUPPORT_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN})
public class SupportResource {
    private static final Logger log = LoggerFactory.getLogger(SupportResource.class);
    private static final String API_SUPPORT_DOWNLOAD_BUNDLE =
            "/api/"+SUPPORT_ROOT+"/"+SUPPORT_BUNDLES_PATH+"/";

    @Autowired
    private AuthorizationService authorizationService;

    @Context
    private HttpServletRequest httpServletRequest;

    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces(MediaType.APPLICATION_JSON)
    @Path(SUPPORT_BUNDLES_PATH)
    @POST
    public Response generateBundle(BundleConfigurationImpl bundleConfiguration) {
        log.debug("Producing support bundle according to configuration: {}", bundleConfiguration);

        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return unauthorized();
        }

        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SupportAddon supportAddon = addonsManager.addonByType(SupportAddon.class);

        List<String> bundles = supportAddon.generate(bundleConfiguration);

        if(bundles != null)
            return Response.ok().entity(wrapResponse(bundles)).build();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Support content collection has failed, see 'support.log' for more details")
                .build();
    }

    /**
     * Wraps generated bundles to relative links
     *
     * @param bundles bundle names
     *
     * @return {@link SupportRequestContent}
     */
    private SupportRequestContent wrapResponse(List<String> bundles) {
        if (bundles.size() > 0) {
            List<String> links = Lists.newArrayList();
            for(String item : bundles) {
                links.add(httpServletRequest.getContextPath() + API_SUPPORT_DOWNLOAD_BUNDLE + item);
            }
            return new SupportRequestContent(links);
        }
        return null;
    }

    @Path(SUPPORT_BUNDLES_PATH + "/{archive: .+}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadBundle(@PathParam("archive") String archive) {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return unauthorized();
        }

        log.debug("Downloading bundle: {}", archive);
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SupportAddon supportAddon = addonsManager.addonByType(SupportAddon.class);
        try {
            return Response.ok().entity(supportAddon.download(archive)).build();
        } catch (FileNotFoundException e) {
            log.debug("Support bundle \"" + archive + "\" does not exist");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Path(SUPPORT_BUNDLES_PATH)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBundles() {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return unauthorized();
        }

        log.debug("Listing all bundles");
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SupportAddon supportAddon = addonsManager.addonByType(SupportAddon.class);
        return Response.ok().entity(wrapResponse(supportAddon.list())).build();
    }

    @Path(SUPPORT_BUNDLES_PATH + "/{archive: .+}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBundle(@PathParam("archive") String archive) {
        if (authorizationService.isAnonymous() || !authorizationService.isAdmin()) {
            return unauthorized();
        }

        log.debug("Deleting bundle: {}", archive);
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SupportAddon supportAddon = addonsManager.addonByType(SupportAddon.class);
        try {
            supportAddon.delete(archive, true); // async operation
            return Response.status(Response.Status.ACCEPTED).build();

        } catch (FileNotFoundException e) {
            log.debug("Support bundle \"" + archive + "\" does not exist");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }
}
