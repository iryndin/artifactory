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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.common.ConstantValues;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.rest.ErrorResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.dgc.VMID;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

/**
 * @author yoavl
 */
public abstract class HttpUtils {
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

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

    /**
     * Reset the cached Artifactory user agent string (required after upgrade)
     */
    public static void resetArtifactoryUserAgent() {
        userAgent = null;
    }

    @SuppressWarnings({"IfMayBeConditional"})
    public static String getRemoteClientAddress(HttpServletRequest request) {
        String remoteAddress;
        //Check if there is a remote address coming from a proxied request
        //(http://httpd.apache.org/docs/2.2/mod/mod_proxy.html#proxypreservehost)
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(header)) {
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

    public static String getRestApiUrl(HttpServletRequest request) {
        return getServletContextUrl(request) + "/" + RestConstants.PATH_API;
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
     * @param status The (http based) response code
     * @return True if the code symbols a successful request cycle (i.e., in the 300-30x range)
     */
    public static boolean isRedirectionResponseCode(int status) {
        return HttpStatus.SC_MULTIPLE_CHOICES <= status && status <= HttpStatus.SC_TEMPORARY_REDIRECT;
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

    /**
     * Returns a gzip aware input stream
     *
     * @param method The method to read the response from
     * @return Returns a gzip aware input stream
     * @throws IOException
     */
    public static InputStream getGzipAwareResponseStream(HttpMethodBase method) throws IOException {
        InputStream is = method.getResponseBodyAsStream();
        Header[] contentEncodings = method.getResponseHeaders("Content-Encoding");
        for (int i = 0, n = contentEncodings.length; i < n; i++) {
            if ("gzip".equalsIgnoreCase(contentEncodings[i].getValue())) {
                return new GZIPInputStream(is);
            }
        }
        return is;
    }

    public static String encodeQuery(String unescaped) {
        try {
            return URIUtil.encodeQuery(unescaped, "UTF-8");
        } catch (URIException e) {
            // Nothing to do here, we will return the un-escaped value.
            log.warn("Could not encode path '{}' with UTF-8 charset, returning the un-escaped value.", unescaped);
        }
        return unescaped;
    }

    public static String decodeUri(String encodedUri) {
        try {
            return URIUtil.decode(encodedUri, "UTF-8");
        } catch (URIException e) {
            // Nothing to do here, we will return the un-escaped value.
            log.warn("Could not decode uri '{}' with UTF-8 charset, returning the encoded value.", encodedUri);
        }
        return encodedUri;
    }

    /**
     * Removes the query parameters from the given url
     *
     * @param url URL string with query parameters, e.g. "http://hello/world?lang=java&run=1"
     * @return new string object without the query parameters, e.g. "http://hello/world". If no query elements found the
     * original string is returned.
     */
    public static String stripQuery(String url) {
        int i = url.indexOf("?");
        if (i > -1) {
            return url.substring(0, i);
        } else {
            return url;
        }
    }

    public static String adjustRefererValue(Map<String, String> headersMap, String headerVal) {
        //Append the artifactory user agent to the referer
        if (headerVal == null) {
            //Fallback to host
            headerVal = headersMap.get("HOST");
            if (headerVal == null) {
                //Fallback to unknown
                headerVal = "UNKNOWN";
            }
        }
        if (!headerVal.startsWith("http")) {
            headerVal = "http://" + headerVal;
        }
        try {
            java.net.URL uri = new java.net.URL(headerVal);
            //Only use the uri up to the path part
            headerVal = uri.getProtocol() + "://" + uri.getAuthority();
        } catch (MalformedURLException e) {
            //Nothing
        }
        headerVal += "/" + HttpUtils.getArtifactoryUserAgent();
        return headerVal;
    }

    /**
     * Extracts the content length from the response header, or return -1 if the content-length field was not found.
     *
     * @param method
     * @return
     */
    public static long getContentLength(HttpMethod method) {
        Header contentLengthHeader = method.getResponseHeader("Content-Length");
        if (contentLengthHeader == null) {
            return -1;
        }
        String contentLengthString = contentLengthHeader.getValue();
        return Long.parseLong(contentLengthString);
    }

    public static void sendErrorResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(statusCode);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        ErrorResponse errorResponse = new ErrorResponse(statusCode, message);
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }
}
