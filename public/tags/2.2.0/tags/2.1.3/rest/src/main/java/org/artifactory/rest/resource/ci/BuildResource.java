package org.artifactory.rest.resource.ci;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.rest.BuildRestConstants;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildService;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

/**
 * A resource to manage the build actions
 *
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path("/" + BuildRestConstants.BUILD_PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class BuildResource {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private BuildService buildService;

    @Context
    private HttpServletResponse httpResponse;

    private static final Logger log = LoggerFactory.getLogger(BuildResource.class);

    /**
     * Adds the given build information to the DB
     *
     * @param build Build to add
     */
    @PUT
    @Consumes("application/vnd.org.jfrog.artifactory+json")
    public void addBuild(Build build) throws Exception {
        log.debug("Received a request to add the build '{}'", build.getName());

        if (!authorizationService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            httpResponse.sendError(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        buildService.addBuild(build);
    }
}