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

package org.artifactory.rest.resource.system;


import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.storage.StorageService;
import org.artifactory.backup.InternalBackupService;
import org.artifactory.common.StatusEntry;
import org.artifactory.descriptor.backup.BackupDescriptor;

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
    private InternalBackupService backupService;

    public StorageResource(StorageService storageService, InternalBackupService backupService,
            HttpServletResponse httpResponse) {
        this.storageService = storageService;
        this.httpResponse = httpResponse;
        this.backupService = backupService;
    }

    @POST
    @Path("compress")
    public Response compress() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.compress(statusHolder);
        return response(statusHolder);
    }

    @GET
    @Path("size")
    @Produces(MediaType.TEXT_PLAIN)
    public String size() {
        return storageService.getStorageSize() + "";
    }

    @POST
    @Path("exportds")
    @Deprecated
    public Response activateExport(@QueryParam("to") String destDir) {
        throw new IllegalStateException("Export data is no longer supported");
    }

    @POST
    @Path("gc")
    public Response activateGc() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.callManualGarbageCollect(statusHolder);
        return response(statusHolder);
    }

    @POST
    @Path("prune")
    public Response activatePruneEmptyDirs() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.pruneUnreferencedFileInDataStore(statusHolder);
        return response(statusHolder);
    }

    @POST
    @Path("convertActual")
    public Response activateConvertActual() {
        MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        storageService.convertActualChecksums(statusHolder);
        return response(statusHolder);
    }

    @POST
    @Path("backup")
    public Response activateBackup(@QueryParam("key") String backupKey) {
        final MultiStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        BackupDescriptor backupDescriptor = backupService.getBackup(backupKey);
        if (backupDescriptor != null) {
            if (backupDescriptor.isEnabled()) {
                backupService.scheduleImmediateSystemBackup(backupDescriptor, statusHolder);
                return response(statusHolder);
            } else {
                return Response.serverError().entity(
                        "Backup identified with key '" + backupKey + "' is disabled").build();
            }
        } else {
            return Response.serverError().entity("No backup identified with key '" + backupKey + "'").build();
        }
    }

    private Response response(MultiStatusHolder statusHolder) {
        StatusEntry lastError = statusHolder.getLastError();
        return lastError == null ? Response.ok().build() :
                Response.serverError().entity(lastError.getMessage()).build();
    }
}