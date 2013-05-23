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

package org.artifactory.traffic;

import org.artifactory.traffic.entry.RequestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger class for general web requests
 *
 * @author Noam Tenne
 */
public abstract class RequestLogger {
    private static final Logger log = LoggerFactory.getLogger(RequestLogger.class);

    private RequestLogger() {
        // utility class
    }

    /**
     * Logs a web request
     *
     * @param userAddress   Address of client machine
     * @param username      Client Artifactory username
     * @param method        HTTP Request method
     * @param path          Request path
     * @param protocol      Request protocol
     * @param returnCode    Response status code
     * @param contentLength Response body size
     */
    public static void request(String userAddress, String username, String method, String path, String protocol,
            int returnCode, long contentLength, long duration) {
        RequestEntry requestEntry = new RequestEntry(userAddress, username, method, path, protocol, returnCode,
                contentLength, duration);
        log.info(requestEntry.toString());
    }
}