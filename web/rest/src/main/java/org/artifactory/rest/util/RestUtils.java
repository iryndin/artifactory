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

package org.artifactory.rest.util;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.rest.constant.SecurityRestConstants;
import org.artifactory.api.search.SearchResultBase;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author yoavl
 */
public abstract class RestUtils {

    private RestUtils() {
        // utility class
    }

    public static String getServletContextUrl(HttpServletRequest request) {
        return HttpUtils.getServletContextUrl(request);
    }

    public static String getRestApiUrl(HttpServletRequest request) {
        return getServletContextUrl(request) + "/" + RestConstants.PATH_API;
    }

    public static String toIsoDateString(long time) {
        return ISODateTimeFormat.dateTime().print(time);
    }

    public static long fromIsoDateString(String dateTime) {
        return ISODateTimeFormat.dateTime().parseMillis(dateTime);
    }

    public static void sendNotFoundResponse(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpStatus.SC_NOT_FOUND, message);
    }

    public static void sendNotFoundResponse(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.SC_NOT_FOUND);
    }

    public static String buildStorageInfoUri(HttpServletRequest request, SearchResultBase result) {
        return buildStorageInfoUri(request, result.getRepoKey(), result.getRelativePath());
    }

    public static String getBaseStorageInfoUri(HttpServletRequest request) {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(RestConstants.PATH_API).append("/").append(ArtifactRestConstants.PATH_ROOT);
        return sb.toString();
    }

    public static String buildStorageInfoUri(HttpServletRequest request, String repoKey, String relativePath) {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(RestConstants.PATH_API).append("/").append(ArtifactRestConstants.PATH_ROOT);
        sb.append("/").append(repoKey).append("/").append(relativePath);
        return sb.toString();
    }

    public static String buildDownloadUri(HttpServletRequest request, String repoKey, String relativePath) {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(repoKey).append("/").append(relativePath);
        return sb.toString();
    }

    public static String buildSecurityInfoUri(HttpServletRequest request, String entityType, String entityKey)
            throws UnsupportedEncodingException {
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        return new StringBuilder(servletContextUrl).append("/").append(RestConstants.PATH_API).append("/")
                .append(SecurityRestConstants.PATH_ROOT).append("/").append(entityType).append("/")
                .append(HttpUtils.encodeQuery(entityKey)).toString();
    }

    public static RepoPath calcRepoPathFromRequestPath(String path) {
        String repoKey = PathUtils.getFirstPathElement(path);
        String relPath = PathUtils.getRelativePath(repoKey, path);
        if (relPath.endsWith("/")) {
            int index = relPath.length() - 1;
            relPath = relPath.substring(0, index);
        }
        return InternalRepoPathFactory.create(repoKey, relPath);
    }

    public static String getBaseBuildsHref(HttpServletRequest request) {
        return RestUtils.getRestApiUrl(request) + "/" + BuildRestConstants.PATH_ROOT;
    }

    public static String getBuildRelativeHref(String buildName) throws UnsupportedEncodingException {
        return "/" + HttpUtils.encodeQuery(buildName);
    }

    public static String getBuildNumberRelativeHref(String buildNumber) throws UnsupportedEncodingException {
        return "/" + HttpUtils.encodeQuery(buildNumber);
    }

    public static String getBuildInfoHref(HttpServletRequest request, String buildName, String buildNumber)
            throws UnsupportedEncodingException {
        return getBaseBuildsHref(request) + getBuildRelativeHref(buildName) + getBuildNumberRelativeHref(buildNumber);
    }

    /**
     * For backward compatability, if the build info version is less or equals to 2.0.11
     * then we need to decode the request parameters since we used a different encoding technique in the past,
     * otherwise we simply let Jersey do the decoding for us
     *
     * @return True if we need to manually decode the request params, false otherwise
     */
    public static boolean shouldDecodeParams(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        // If the request didn't come from build info let Jersey do the work
        if (StringUtils.isBlank(userAgent) || !userAgent.startsWith("ArtifactoryBuildClient/")) {
            return false;
        }

        String buildInfoVersion = StringUtils.removeStart(userAgent, "ArtifactoryBuildClient/");
        boolean snapshotCondition = StringUtils.contains(buildInfoVersion, "SNAPSHOT");
        boolean newVersionCondition = new DefaultArtifactVersion("2.0.11").compareTo(
                new DefaultArtifactVersion(buildInfoVersion)) < 0;

        // Build info version is SNAPSHOT or newer than 2.0.11 we also let Jersey do the work
        if (snapshotCondition || newVersionCondition) {
            return false;
        }

        // If we got here it means client is using an old build-info (<= 2.0.11) we must manually decode the http params
        return true;
    }
}
