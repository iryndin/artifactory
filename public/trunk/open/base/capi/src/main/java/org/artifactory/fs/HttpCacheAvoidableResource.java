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

package org.artifactory.fs;

/**
 * Marks that a resource may avoid the HTTP cache by adding the headers:
 * <br/>
 * Expires: Thu, 01 Jan 1970 00:00:00 GMT
 * <br/>
 * Pragma: no-cache
 * <br/>
 * Cache-Control: no-cache, no-store
 * <br/>
 * To the resources response
 *
 * @author Noam Y. Tenne
 */
public interface HttpCacheAvoidableResource {

    /**
     * Should the resource be sent with headers to avoid HTTP caching
     *
     * @return True if caching should be avoided
     */
    boolean avoidHttpCaching();

    void setAvoidHttpCaching(boolean avoidHttpCaching);
}
