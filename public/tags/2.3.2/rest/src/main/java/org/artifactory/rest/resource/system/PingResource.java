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

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.VersionInfo;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.api.rest.search.result.VersionRestResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.List;

/**
 * Resource to get information about Artifactory current state.
 * The method will:
 * <ul><li>Check that JCR behaves correctly (do an actual JCR Node retrieve)</li>
 * <li>Check that the logger behaves correctly (read and/or write to log)</li></ul>
 * Upon results return:
 * <ul><li>Code 200 with text exactly equal to "OK" if all is good</li>
 * <li>Code 500 with failure description in the text if something wrong</li></ul>
 *
 * @author Fred Simon
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(SystemRestConstants.PATH_ROOT + "/" + SystemRestConstants.PATH_PING)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class PingResource {
    private static final Logger log = LoggerFactory.getLogger(PingResource.class);

    @Autowired
    private JcrService jcrService;

    @Autowired
    private AddonsManager addonsManager;
    private static final String PING_TEST_NODE = "pingTestNode";

    /**
     * @return The artifactory state "OK" or "Failure"
     */
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public Response pingArtifactory() {
        try {
            log.debug("Received ping call");
            // Check that addons are OK if needed
            if (addonsManager.lockdown()) {
                log.error("Ping failed due to unloaded addons");
                return Response.status(HttpStatus.SC_FORBIDDEN).entity("Addons unloaded").build();
            }
            ArtifactoryHome artifactoryHome = ArtifactoryHome.get();
            if (!artifactoryHome.getDataDir().canWrite() || !artifactoryHome.getLogDir().canWrite()) {
                log.error("Ping failed due to file system access to data or log dir failed");
                return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity("File system access failed").build();
            }
            JcrSession session = null;
            try {
                session = jcrService.getUnmanagedSession();
                Node trashNode = session.getNode(JcrPath.get().getTrashJcrRootPath());
                Node pingTestNode;
                if (!trashNode.hasNode(PING_TEST_NODE)) {
                    pingTestNode = trashNode.addNode(PING_TEST_NODE);
                } else {
                    pingTestNode = trashNode.getNode(PING_TEST_NODE);
                }
                pingTestNode.setProperty(Property.JCR_LAST_MODIFIED, Calendar.getInstance());
                // TODO: SearchManager exception are swallowed by Jackrabbit need to find a way to listen to JCR errors!
                session.save();
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } catch (Exception e) {
            log.error("Error during ping test", e);
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        log.debug("Ping successful");
        return Response.ok().entity("OK").build();
    }

}
