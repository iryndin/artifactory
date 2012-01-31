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

package org.artifactory.util;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.request.ArtifactoryRequest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.rmi.dgc.VMID;
import java.util.StringTokenizer;

/**
 * @author yoavl
 */
public abstract class HttpUtils {

    private static String userAgent;

    //Indicates whether Artifactory is working with servlet API v2.4
    private static final boolean SERVLET_24;

    private static String VM_HOST_ID;
    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";

    /**
     * Determine if we are running with servlet API v2.4 or v2.5
     */
    static {
        //Check the availability of javax.servlet.ServletContext.getContextPath()
        Method contextPathGetter = null;
        try {
            contextPathGetter = ServletContext.class.getMethod("getContextPath", new Class[0]);
        } catch (NoSuchMethodException e) {
        } finally {
            /**
             * Indicate whether we are running v2.4 or v2.5 by checking if javax.servlet.ServletContext.getContextPath()
             * is available (introduced in v2.5)
             */
            SERVLET_24 = (contextPathGetter == null);
        }
    }

    private HttpUtils() {
        // utility class
    }

    public static String getArtifactoryUserAgent() {
        if (userAgent == null) {
            String artifactoryVersion = ConstantValues.artifactoryVersion.getString();
            if (artifactoryVersion.startsWith("$") || artifactoryVersion.endsWith("SNAPSHOT")) {
                artifactoryVersion = "development";
            }
            userAgent = "Artifactory/" + artifactoryVersion;
        }
        return userAgent;
    }

    @SuppressWarnings({"IfMayBeConditional"})
    public static String getRemoteClientAddress(HttpServletRequest request) {
        String remoteAddress;
        //Check if there is a remote address coming from a proxied request
        //(http://httpd.apache.org/docs/2.2/mod/mod_proxy.html#proxypreservehost)
        String header = request.getHeader("X-Forwarded-For");
        if (header != null) {
            //Might contain multiple entries - take the first
            remoteAddress = new StringTokenizer(header, ",").nextToken();
        } else {
            //Take it the standard way
            remoteAddress = request.getRemoteAddr();
        }
        return remoteAddress;
    }

    public static String getWebappContextUrl(HttpServletRequest httpRequest) {
        String servletContextUrl = getServletContextUrl(httpRequest);
        if (!servletContextUrl.endsWith("/")) {
            servletContextUrl += "/";
        }
        return servletContextUrl + WEBAPP_URL_PATH_PREFIX + "/";
    }

    public static String getServletContextUrl(HttpServletRequest httpRequest) {
        CentralConfigService centralConfigService = ContextHelper.get().getCentralConfig();
        String baseUrl = centralConfigService.getDescriptor().getUrlBase();
        if (!StringUtils.isEmpty(baseUrl)) {
            String scheme = httpRequest.getScheme();
            if (baseUrl.startsWith(scheme)) {
                return baseUrl;
            } else {
                int idx = baseUrl.indexOf("://");
                if (idx > 0) {
                    return scheme + "://" + baseUrl.substring(idx + 3);
                } else {
                    return scheme + "://" + baseUrl;
                }
            }
        }
        return getServerUrl(httpRequest) + httpRequest.getContextPath();
    }

    public static String getServerUrl(HttpServletRequest httpRequest) {
        int port = httpRequest.getServerPort();
        String scheme = httpRequest.getScheme();
        if (isDefaultPort(scheme, port)) {
            return scheme + "://" + httpRequest.getServerName();
        }
        return scheme + "://" + httpRequest.getServerName() + ":" + port;
    }

    public static boolean isDefaultPort(String scheme, int port) {
        switch (port) {
            case 80:
                return "http".equalsIgnoreCase(scheme);
            case 443:
                return "https".equalsIgnoreCase(scheme);
            default:
                return false;
        }
    }

    public static String getSha1Checksum(ArtifactoryRequest request) {
        return request.getHeader(ArtifactoryRequest.CHECKSUM_SHA1);
    }

    public static boolean isExpectedContinue(ArtifactoryRequest request) {
        String expectHeader = request.getHeader("Expect");
        if (StringUtils.isBlank(expectHeader)) {
            return false;
        }
        // some clients make the C lowercase even when passed uppercase
        return expectHeader.contains("100-continue") || expectHeader.contains("100-Continue");
    }

    public static String getMd5Checksum(ArtifactoryRequest request) {
        return request.getHeader(ArtifactoryRequest.CHECKSUM_MD5);
    }

    public synchronized static String getContextId(ServletContext servletContext) {
        //If running servlet API 2.4, just return the servlet context name
        if (SERVLET_24) {
            return servletContext.getServletContextName();
        }

        //If running v2.5, return proper context path
        String contextUniqueName = PathUtils.trimLeadingSlashes(servletContext.getContextPath());
        contextUniqueName = StringUtils.capitalize(contextUniqueName);
        return contextUniqueName;
    }

    /**
     * @param status The (http based) response code
     * @return True if the code symbols a successful request cycle (i.e., in the 200-20x range)
     */
    public static boolean isSuccessfulResponseCode(int status) {
        return HttpStatus.SC_OK <= status && status <= HttpStatus.SC_MULTI_STATUS;
    }

    /**
     * Calculate a unique id for the VM to support Artifactories with the same ip (e.g. accross NATs)
     */
    public static String getHostId() {
        if (StringUtils.isNotBlank(ConstantValues.hostId.getString())) {
            return ConstantValues.hostId.getString();
        }
        if (VM_HOST_ID == null) {
            VMID vmid = new VMID();
            VM_HOST_ID = vmid.toString();
        }
        return VM_HOST_ID;
    }
}
