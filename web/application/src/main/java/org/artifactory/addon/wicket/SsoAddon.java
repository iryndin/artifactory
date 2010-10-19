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

package org.artifactory.addon.wicket;

import org.artifactory.addon.sso.ArtifactoryCrowdAuthenticator;
import org.artifactory.common.wicket.model.sitemap.MenuNode;
import org.artifactory.descriptor.security.sso.CrowdSettings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.rmi.RemoteException;

/**
 * Addon for HTTP SSO capabilities
 *
 * @author Noam Tenne
 */
public interface SsoAddon extends ArtifactoryCrowdAuthenticator {

    String DEFAULT_REQUEST_VARIABLE = "REMOTE_USER";

    String REALM = "http-sso";

    String CROWD_ERROR_MESSAGE = "Unable to authenticate with Atlassian crowd.";

    /**
     * Returns the HTTP SSO site map builder menu node
     *
     * @param nodeName Name to give to menu node
     * @return Real menu node if addon is enabled. Disabled if not
     */
    MenuNode getHttpSsoMenuNode(String nodeName);

    /**
     * Returns the Crowd SSO site map builder menu node
     *
     * @param nodeName Name to give to menu node
     * @return Real menu node if addon is enabled. Disabled if not
     */
    MenuNode getCrowdAddonMenuNode(String nodeName);

    /**
     * Calls for a Crowd connection testing
     *
     * @param crowdSettings Settings to test
     * @throws Exception Thrown if the test failed in any way
     */
    void testCrowdConnection(CrowdSettings crowdSettings) throws Exception;

    void logOffSso(HttpServletRequest request, HttpServletResponse response) throws RemoteException;
}
