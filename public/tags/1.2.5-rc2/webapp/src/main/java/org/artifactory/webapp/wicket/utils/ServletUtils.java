package org.artifactory.webapp.wicket.utils;

import org.apache.log4j.Logger;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.artifactory.security.RepoPath;
import org.artifactory.webapp.servlet.RepoFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ServletUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ServletUtils.class);

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static String getServletContextUrl() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        WebRequest request = webRequestCycle.getWebRequest();
        HttpServletRequest httpRequest = request.getHttpServletRequest();
        final String url = httpRequest.getScheme() + "://" +
                httpRequest.getServerName() + ":" +
                httpRequest.getServerPort() +
                httpRequest.getContextPath();
        return url;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static RepoPath getRepoPath(WebRequest request) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath repoPath = (RepoPath) httpServletRequest.getAttribute(
                RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
        return repoPath;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static void removeRepoPath(WebRequest request) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        httpServletRequest.removeAttribute(RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH);
    }
}
