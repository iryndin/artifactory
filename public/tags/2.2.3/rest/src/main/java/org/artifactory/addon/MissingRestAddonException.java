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

package org.artifactory.addon;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Yoav Landman
 */
public class MissingRestAddonException extends WebApplicationException {

    private static final String POWERPACK_MESSAGE =
            "This API is available only in the Artifactory Add-ons Power Pack (see: " +
                    "http://www.jfrog.org/addons.php). If you are already running Artifactory " +
                    "with the Add-ons Power Pack please make sure your server was activated with " +
                    "a valid server ID.\n";

    public MissingRestAddonException() {
        super(Response.status(Response.Status.BAD_REQUEST).entity(POWERPACK_MESSAGE)
                .type(MediaType.TEXT_PLAIN).build());
    }
}