/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.artifactory.common.StatusHolder;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author yoavl
 */
public class StorageResource {
    private StorageService storageService;

    public StorageResource(StorageService storageService) {
        this.storageService = storageService;
    }

    @POST
    @Path("compress")
    @Produces(MediaType.APPLICATION_XML)
    public StatusHolder compress() {
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        storageService.compress(statusHolder);
        return statusHolder;
    }

    @GET
    @Path("size")
    @Produces(MediaType.TEXT_PLAIN)
    public String size() {
        return storageService.getStorageSize() + "";
    }
}