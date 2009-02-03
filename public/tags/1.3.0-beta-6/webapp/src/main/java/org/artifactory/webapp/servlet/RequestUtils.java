/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.servlet;


import org.artifactory.api.context.ContextHelper;
import org.artifactory.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: freds Date: Aug 13, 2008 Time: 10:56:25 AM
 */
public class RequestUtils {
    private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);

    private static final Set<String> NON_UI_PATH_PREFIXES = new HashSet<String>();
    private static final Set<String> UI_PATH_PREFIXES = new HashSet<String>();
    public static final String LAST_USER_KEY = "artifactory:lastUserId";
    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";
    private static boolean USE_PATH_INFO = false;

    public static void setNonUiPathPrefixes(Collection<String> uriPathPrefixes) {
        NON_UI_PATH_PREFIXES.clear();
        NON_UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    public static void setUiPathPrefixes(Collection<String> uriPathPrefixes) {
        UI_PATH_PREFIXES.clear();
        UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    public static String getContextPrefix(HttpServletRequest request) {
        String contextPrefix;
        String requestUri = request.getRequestURI();
        int contextPrefixEndIdx = requestUri.indexOf('/', 1);
        if (contextPrefixEndIdx > 0) {
            contextPrefix = requestUri.substring(1, contextPrefixEndIdx);
        } else {
            contextPrefix = "";
        }
        return contextPrefix;
    }

    protected static boolean isRepoRequest(String servletPath) {
        String pathPrefix = PathUtils.getPathFirstPart(servletPath);
        if (pathPrefix == null || pathPrefix.length() == 0) {
            return false;
        }
        if (UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        if (NON_UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        //Check that is a repository prefix with support for old repo sytax
        String repoPrefix = pathPrefix.endsWith("@repo") ?
                pathPrefix.substring(0, pathPrefix.length() - 5) : pathPrefix;
        List<String> allRepos = ContextHelper.get().getRepositoryService().getAllRepoKeys();
        if (!allRepos.contains(repoPrefix)) {
            log.error("Request " + servletPath + " should be a repo request and does not match any repo key");
            return false;
        }
        return true;
    }

    public static String getServletContextUrl(HttpServletRequest httpRequest) {
        return httpRequest.getScheme() + "://" +
                httpRequest.getServerName() + ":" +
                httpRequest.getServerPort() +
                httpRequest.getContextPath();
    }

    public static boolean isWebdavRequest(HttpServletRequest request) {
        String wagonProvider = request.getHeader("X-wagon-provider");
        return wagonProvider != null && wagonProvider.contains("webdav");
    }

    public static boolean isUiRequest(HttpServletRequest request) {
        if (isWebdavRequest(request)) {
            return false;
        }
        if (isWicketRequest(request)) {
            return true;
        }
        String pathPrefix = PathUtils.getPathFirstPart(getServletPathFromRequest(request));
        return isUiPathPrefix(pathPrefix);
    }

    public static boolean isUiPathPrefix(String pathPrefix) {
        if (UI_PATH_PREFIXES.contains(pathPrefix)) {
            return true;
        }
        if (NON_UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        return false;
    }

    public static boolean isWicketRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString != null && queryString.startsWith("wicket");
    }

    public static boolean isAuthHeaderPresent(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Basic ");
    }

    public static Authentication getAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (Authentication) session.getAttribute(LAST_USER_KEY);
    }

    public static boolean setAuthentication(HttpServletRequest request,
            Authentication authentication, boolean createSession) {
        HttpSession session = request.getSession(createSession);
        if (session == null) {
            return false;
        }
        session.setAttribute(LAST_USER_KEY, authentication);
        return true;
    }

    public static void removeAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(LAST_USER_KEY);
        }
    }

    /**
     * Sets the value which indicates if to use path info instead of servlet path in getServletPathFromRequest method
     *
     * @param usePathInfo True if to prefr path info over servlet path
     */
    public static void setUsePathInfo(boolean usePathInfo) {
        USE_PATH_INFO = usePathInfo;
    }

    /**
     * Returns the servlet path from the request, in accordance to the boolean value of USE_PATH_INFO which Decides if
     * to try and use getPathInfo() instead of getServletPath().
     *
     * @param req The recieved request
     * @return String - Servlet path
     */
    public static String getServletPathFromRequest(HttpServletRequest req) {
        if (USE_PATH_INFO) {
            //Websphere returns the path in the getPathInfo()
            String path = req.getPathInfo();
            //path == null so no Websphere
            if (path == null) {
                return req.getServletPath();
            }

            if (path.length() == 0) {
                path = "/" + WEBAPP_URL_PATH_PREFIX;
            }

            return path;
        } else {
            return req.getServletPath();
        }
    }
}
