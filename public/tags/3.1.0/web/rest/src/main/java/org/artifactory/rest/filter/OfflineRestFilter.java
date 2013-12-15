package org.artifactory.rest.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.context.ContextHelper;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * author: gidis
 * Block Rest request during offline state
 */

public class OfflineRestFilter implements ContainerRequestFilter {

    @Context
    HttpServletResponse response;

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        // Filter out all events in case of offline mode
        if (ContextHelper.get().isOffline()) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Artifactory API\"");
            try {
                response.sendError(HttpStatus.SC_FORBIDDEN);
            } catch (IOException e) {
                throw new WebApplicationException(HttpStatus.SC_FORBIDDEN);
            }
        }
        return containerRequest;
    }
}
