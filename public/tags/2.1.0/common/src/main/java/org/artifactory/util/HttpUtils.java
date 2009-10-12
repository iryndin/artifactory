/*
 * This file is part of Artifactory.
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

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

/**
 * @author yoavl
 */
public abstract class HttpUtils {

    private static String userAgent;

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

    public static String getServletContextUrl(HttpServletRequest httpRequest) {
        String url;

        CentralConfigService centralConfigService = ContextHelper.get().getCentralConfig();
        String baseUrl = centralConfigService.getDescriptor().getUrlBase();

        if (!StringUtils.isEmpty(baseUrl)) {
            url = baseUrl;
        } else {
            int port = httpRequest.getServerPort();
            String scheme = httpRequest.getScheme();
            String portString = isDefaultPort(scheme, port) ? "" : ":" + port;
            url = scheme + "://" +
                    httpRequest.getServerName() + portString +
                    httpRequest.getContextPath();
        }

        return url;
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

    public static String getContextId(ServletContext servletContext) {
        String contextUniqueName = PathUtils.trimLeadingSlashes(servletContext.getContextPath());
        contextUniqueName = StringUtils.capitalize(contextUniqueName);
        return contextUniqueName;
    }
}
