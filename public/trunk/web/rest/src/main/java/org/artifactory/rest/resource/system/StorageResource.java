/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.rest.resource.system;


import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.storage.StorageService;
import org.artifactory.common.StatusEntry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author yoavl
 */
public class StorageResource {
    private HttpServletResponse httpResponse;
    private StorageService storageService;

    public StorageResource(StorageService storageService, HttpServletResponse httpResponse) {
        this.storageService = storageService;
        this.httpResponse = httpResponse;
    }

    @POST
    @Path("compress")
    public Response compress() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.compress(statusHolder);
        StatusEntry lastError = statusHolder.getLastError();
        return lastError == null ? Response.ok().build() :
                Response.serverError().entity(lastError.getMessage()).build();
    }

    @GET
    @Path("size")
    @Produces(MediaType.TEXT_PLAIN)
    public String size() {
        return storageService.getStorageSize() + "";
    }

    @POST
    @Path("exportds")
    public Response activateExport(@QueryParam("to") String destDir) {
        storageService.exportDbDataStore(destDir);
        return Response.noContent().build();
    }

    @POST
    @Path("gc")
    public Response activateGc() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.callManualGarbageCollect(statusHolder);
        StatusEntry lastError = statusHolder.getLastError();
        return lastError == null ? Response.ok().build() :
                Response.serverError().entity(lastError.getMessage()).build();
    }

    @POST
    @Path("prune")
    public Response activatePruneEmptyDirs() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.pruneUnreferencedFileInDataStore(statusHolder);
        StatusEntry lastError = statusHolder.getLastError();
        return lastError == null ? Response.ok().build() :
                Response.serverError().entity(lastError.getMessage()).build();
    }
}