/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.servlet;


import org.apache.wicket.protocol.http.WebRequest;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.webdav.WebdavService;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * User: freds Date: Aug 13, 2008 Time: 10:56:25 AM
 */
public abstract class RequestUtils {
    private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);

    private static final Set<String> NON_UI_PATH_PREFIXES = new HashSet<String>();
    private static final Set<String> UI_PATH_PREFIXES = new HashSet<String>();
    public static final String LAST_USER_KEY = "artifactory:lastUserId";
    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";
    private static boolean USE_PATH_INFO = false;
    private static final String DEFAULT_ENCODING = "utf-8";

    private RequestUtils() {
        // utility class
    }

    public static void setNonUiPathPrefixes(Collection<String> uriPathPrefixes) {
        NON_UI_PATH_PREFIXES.clear();
        NON_UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    public static void setUiPathPrefixes(Collection<String> uriPathPrefixes) {
        UI_PATH_PREFIXES.clear();
        UI_PATH_PREFIXES.addAll(uriPathPrefixes);
    }

    @SuppressWarnings({"IfMayBeConditional"})
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

    public static boolean isRepoRequest(HttpServletRequest request) {
        String servletPath = getServletPathFromRequest(request);
        String pathPrefix = PathUtils.getFirstPathElement(servletPath);
        if (pathPrefix == null || pathPrefix.length() == 0) {
            return false;
        }
        if (ArtifactoryRequest.LIST_BROWSING_PATH.equals(pathPrefix)) {
            pathPrefix = PathUtils.getFirstPathElement(servletPath.substring("list/".length()));
        }
        if (UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        if (NON_UI_PATH_PREFIXES.contains(pathPrefix)) {
            return false;
        }
        //Check that is a repository prefix with support for old repo sytax
        String repoKey = pathPrefix.endsWith("@repo") ?
                pathPrefix.substring(0, pathPrefix.length() - 5) : pathPrefix;
        //Support repository-level metadata requests
        repoKey = NamingUtils.stripMetadataFromPath(repoKey);
        //Strip any matrix params
        int paramsIdx = repoKey.indexOf(Properties.MATRIX_PARAMS_SEP);
        if (paramsIdx > 0) {
            repoKey = repoKey.substring(0, paramsIdx);
        }
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        Set<String> allRepos = repositoryService.getAllRepoKeys();
        if (!allRepos.contains(repoKey)) {
            log.warn("Request " + servletPath + " should be a repo request and does not match any repo key");
            return false;
        }
        return true;
    }

    public static boolean isWebdavRequest(HttpServletRequest request) {
        if (!isRepoRequest(request)) {
            return false;
        }
        if (WebdavService.WEBDAV_METHODS.contains(request.getMethod().toLowerCase(Locale.ENGLISH))) {
            return true;
        }
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
        String pathPrefix = PathUtils.getFirstPathElement(getServletPathFromRequest(request));
        return isUiPathPrefix(pathPrefix);
    }

    public static boolean isWicketRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString != null && queryString.startsWith("wicket");
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

    public static boolean isReservedName(String pathPrefix) {
        return UI_PATH_PREFIXES.contains(pathPrefix) || NON_UI_PATH_PREFIXES.contains(pathPrefix) ||
                "list".equals(pathPrefix);
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

    public static boolean setAuthentication(HttpServletRequest request, Authentication authentication,
            boolean createSession) {
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
     * @param usePathInfo True if to prefer path info over servlet path
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
            String uri = req.getRequestURI();
            String path = req.getServletPath();
            int idx = uri.lastIndexOf(path);
            if (idx > 0) {
                path = uri.substring(idx);
            }
            return path;
        }
    }

    /**
     * @param servletContext The servlet context
     * @return The artifactory spring context
     */
    public static ArtifactoryContext getArtifactoryContext(ServletContext servletContext) {
        return (ArtifactoryContext) servletContext.getAttribute(ArtifactoryContext.APPLICATION_CONTEXT_KEY);
    }

    public static RepoPath getRepoPath(WebRequest request) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        return (RepoPath) httpServletRequest
                .getAttribute(org.artifactory.webapp.servlet.RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
    }

    /**
     * Calculates a repoPath based on the given servlet path (path after the context root, including the repo prefix).
     */
    public static RepoPath calculateRepoPath(String requestPath) {
        String repoKey = PathUtils.getFirstPathElement(requestPath);
        String path = PathUtils.stripFirstPathElement(requestPath);
        return new RepoPathImpl(repoKey, path);
    }


    public static void removeRepoPath(WebRequest request, boolean storeAsRemoved) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath removedRepoPath = getRepoPath(request);
        httpServletRequest.removeAttribute(org.artifactory.webapp.servlet.RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (removedRepoPath != null && storeAsRemoved) {
            httpServletRequest
                    .setAttribute(org.artifactory.webapp.servlet.RepoFilter.ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH,
                            removedRepoPath);
        }
    }

    /**
     * Extract the username out of the request, by checking the the header for the {@code Authorization} and then if it
     * starts with {@code Basic} get it as a base 64 token and decode it.
     *
     * @param request The request to examine
     * @return The extracted username
     * @throws UnsupportedEncodingException If UTF-8 is not supported.
     */
    public static String extractUsernameFromRequest(ServletRequest request)
            throws UnsupportedEncodingException {
        String header = ((HttpServletRequest) request).getHeader("Authorization");
        if ((header != null) && header.startsWith("Basic ")) {
            String token;
            byte[] base64Token;
            try {
                base64Token = header.substring(6).getBytes(DEFAULT_ENCODING);
                token = new String(org.apache.commons.codec.binary.Base64.decodeBase64(base64Token), DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                log.info("the encoding is not supported");
                return EMPTY;
            }
            String username = EMPTY;
            int delim = token.indexOf(':');
            if (delim != -1) {
                username = token.substring(0, delim);
            }
            return username;
        }
        return EMPTY;
    }

    public static boolean isAuthPresent(WebRequest request) {
        return isAuthHeaderPresent(request.getHttpServletRequest());
    }

    public static String getWicketServletContextUrl() {
        WebRequest request = WicketUtils.getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        return HttpUtils.getServletContextUrl(httpRequest);
    }
}
