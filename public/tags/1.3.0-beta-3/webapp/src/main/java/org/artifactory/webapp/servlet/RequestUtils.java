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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.utils.PathUtils;
import org.artifactory.webapp.wicket.ArtifactoryWebSession;
import org.springframework.security.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: freds Date: Aug 13, 2008 Time: 10:56:25 AM
 */
public class RequestUtils {
    private static final Logger LOGGER =
            LogManager.getLogger(RequestUtils.class);

    public static final Set<String> REPO_PATH_PREFIXES = new HashSet<String>();
    public static final Set<String> NON_UI_PATH_PREFIXES = new HashSet<String>();
    public static final Set<String> UI_PATH_PREFIXES = new HashSet<String>();

    public static void addRepoPathPrefixes(Collection<String> uriPathPrefixes) {
        REPO_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    public static void addNonUiPathPrefixes(Collection<String> uriPathPrefixes) {
        NON_UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    public static void addUiPathPrefixes(Collection<String> uriPathPrefixes) {
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
        String pathPrefix = PathUtils.getPathPrefix(servletPath);
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
        if (!REPO_PATH_PREFIXES.contains(repoPrefix)) {
            LOGGER.error("Request " + servletPath +
                    " should be a repo request and does not match any repo key");
            return false;
        }
        return true;
    }

    public static String getServletContextUrl(HttpServletRequest httpRequest) {
        final String url = httpRequest.getScheme() + "://" +
                httpRequest.getServerName() + ":" +
                httpRequest.getServerPort() +
                httpRequest.getContextPath();
        return url;
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
        String pathPrefix = PathUtils.getPathPrefix(request.getServletPath());
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
        boolean authExists = header != null && header.startsWith("Basic ");
        return authExists;
    }

    public static Authentication getAuthentication(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Authentication authentication =
                (Authentication) session.getAttribute(ArtifactoryWebSession.LAST_USER_KEY);
        return authentication;
    }
}
