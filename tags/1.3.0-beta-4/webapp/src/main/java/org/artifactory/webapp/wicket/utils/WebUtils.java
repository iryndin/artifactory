package org.artifactory.webapp.wicket.utils;

import org.apache.log4j.Logger;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.webapp.servlet.RepoFilter;
import org.artifactory.webapp.servlet.RequestUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class WebUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(WebUtils.class);

    public static String getWicketServletContextUrl() {
        WebRequest request = getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        return RequestUtils.getServletContextUrl(httpRequest);
    }

    public static WebRequest getWebRequest() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        if (webRequestCycle == null) {
            return null;
        }
        WebRequest request = webRequestCycle.getWebRequest();
        return request;
    }

    public static WebResponse getWebResponse() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        WebResponse response = webRequestCycle.getWebResponse();
        return response;
    }


    public static RepoPath getRepoPath(WebRequest request) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath repoPath = (RepoPath) httpServletRequest.getAttribute(
                RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        return repoPath;
    }

    public static void removeRepoPath(WebRequest request, boolean storeAsRemoved) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath removedRepoPath = getRepoPath(request);
        httpServletRequest.removeAttribute(RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (removedRepoPath != null && storeAsRemoved) {
            httpServletRequest.setAttribute(
                    RepoFilter.ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH, removedRepoPath);
        }
    }

    public static boolean isAuthPresent(WebRequest request) {
        return RequestUtils.isAuthHeaderPresent(request.getHttpServletRequest());
    }

}
