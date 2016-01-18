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

package org.artifactory.rest.common.exception;

import com.sun.jersey.api.Responses;
import org.artifactory.exception.InvalidNameException;
import org.artifactory.rest.ErrorResponse;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Michael Pasternak
 */
@Component
@Provider
public class InvalidNameExceptionMapper implements ExceptionMapper<InvalidNameException> {

    @Override
    public Response toResponse(InvalidNameException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                exception.getMessage()
        );
        return Responses.notFound().type(MediaType.APPLICATION_JSON).entity(errorResponse).build();
    }
}
