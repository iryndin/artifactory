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

package org.artifactory.rest.util;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.SearchResultBase;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yoavl
 */
public abstract class RestUtils {

    public static String getServletContextUrl(HttpServletRequest request) {
        return HttpUtils.getServletContextUrl(request);
    }

    public static String getRestApiUrl(HttpServletRequest request) {
        return getServletContextUrl(request) + "/" + RestConstants.PATH_API;
    }

    /**
     * Check if this request url is pointing to the rest build api and retrieving the build name element from the
     * request
     *
     * @param request
     * @return build name
     */
    public static String getBuildNameFromRequest(HttpServletRequest request) {
        String[] pathElements = getBuildRestUrlPathElements(request);
        return pathElements[0];
    }

    /**
     * Check if this request url is pointing to the rest build api and retrieving the build number element from the
     * request
     *
     * @param request
     * @return build number
     */
    public static long getBuildNumberFromRequest(HttpServletRequest request) {
        String[] pathElements = getBuildRestUrlPathElements(request);
        String buildNumber = pathElements[1];
        return Long.parseLong(buildNumber);
    }

    public static String toIsoDateString(long time) {
        return ISODateTimeFormat.dateTime().print(time);
    }

    private static String[] getBuildRestUrlPathElements(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String sUrl = url.toString();
        String apiUrl = getRestApiUrl(request) + "/" + BuildRestConstants.PATH_ROOT;
        if (!sUrl.startsWith(apiUrl)) {
            throw new IllegalArgumentException("This method should be called only on build rest request");
        }
        String[] pathElements = PathUtils.getPathElements(sUrl.substring(apiUrl.length()));
        return pathElements;
    }

    public static void sendNotFoundResponse(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpStatus.SC_NOT_FOUND, message);
    }

    public static void sendNotFoundResponse(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.SC_NOT_FOUND);
    }


    public static String buildStorageInfoUri(HttpServletRequest request, SearchResultBase result) {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(RestConstants.PATH_API).append("/").append(ArtifactRestConstants.PATH_ROOT);
        sb.append("/").append(result.getRepoKey()).append("/").append(result.getRelativePath());
        return sb.toString();
    }

}
