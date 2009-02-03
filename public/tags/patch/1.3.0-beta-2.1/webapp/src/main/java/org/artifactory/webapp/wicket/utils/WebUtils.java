package org.artifactory.webapp.wicket.utils;

import org.apache.log4j.Logger;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.artifactory.repo.RepoPath;
import org.artifactory.webapp.servlet.RepoFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class WebUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(WebUtils.class);

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static String getWicketServletContextUrl() {
        WebRequest request = getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        return getServletContextUrl(httpRequest);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static String getServletContextUrl(HttpServletRequest httpRequest) {
        final String url = httpRequest.getScheme() + "://" +
                httpRequest.getServerName() + ":" +
                httpRequest.getServerPort() +
                httpRequest.getContextPath();
        return url;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static WebRequest getWebRequest() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        WebRequest request = webRequestCycle.getWebRequest();
        return request;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static WebResponse getWebResponse() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        WebResponse response = webRequestCycle.getWebResponse();
        return response;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static RepoPath getRepoPath(WebRequest request) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath repoPath = (RepoPath) httpServletRequest.getAttribute(
                RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        return repoPath;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static void removeRepoPath(WebRequest request, boolean storeAsRemoved) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath removedRepoPath = getRepoPath(request);
        httpServletRequest.removeAttribute(RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (removedRepoPath != null && storeAsRemoved) {
            httpServletRequest.setAttribute(
                    RepoFilter.ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH, removedRepoPath);
        }
    }

    public static boolean isWicketRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString != null && queryString.startsWith("wicket");
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static boolean isAuthPresent(WebRequest request) {
        return isAuthPresent(request.getHttpServletRequest());
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static boolean isAuthPresent(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        boolean authExists = header != null && header.startsWith("Basic ");
        return authExists;
    }
}
