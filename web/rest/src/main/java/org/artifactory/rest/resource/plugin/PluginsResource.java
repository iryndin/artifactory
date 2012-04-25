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

package org.artifactory.rest.resource.plugin;

import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.list.KeyValueList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.artifactory.api.rest.constant.PluginRestConstants.*;

/**
 * A resource for plugin execution
 *
 * @author Tomer Cohen
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_USER, AuthorizationService.ROLE_ADMIN})
public class PluginsResource {

    @Autowired
    AddonsManager addonsManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPluginInfo() {
        return addonsManager.addonByType(RestAddon.class).getUserPluginInfo();
    }

    @POST
    @Path(PATH_EXECUTE + "/{executionName: .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response execute(
            @PathParam("executionName") String executionName,
            @QueryParam(PARAM_PARAMS) KeyValueList paramsList,
            @QueryParam(PARAM_ASYNC) int async) throws Exception {
        Map<String, List<String>> params =
                paramsList != null ? paramsList.toStringMap() : Maps.<String, List<String>>newHashMap();
        ResponseCtx responseCtx =
                addonsManager.addonByType(RestAddon.class).runPluginExecution(executionName, params, async == 1);
        if (async == 1) {
            //Just return accepted (202)
            return Response.status(HttpStatus.SC_ACCEPTED).build();
        } else {
            Response.ResponseBuilder builder;
            int status = responseCtx.getStatus();
            if (status != ResponseCtx.UNSET_STATUS) {
                builder = Response.status(status);
            } else {
                builder = Response.ok();
            }
            String message = responseCtx.getMessage();
            if (message != null) {
                builder.entity(message);
            }
            return builder.build();
        }
    }
}
