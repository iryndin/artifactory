/*
 * This file is part of Artifactory.
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

package org.artifactory.rest.common;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

/**
 * A wrapper provider that makes it simpler to configure providers in one place. <br/> (This is more crucial for WAS,
 * where discovery is done by direct jar reference).
 *
 * @author Yoav Landman
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "application/vnd.org.jfrog.artifactory+json"})
@Produces({MediaType.APPLICATION_JSON, "application/vnd.org.jfrog.artifactory+json"})
public class JsonProvider extends JacksonJsonProvider {
}
