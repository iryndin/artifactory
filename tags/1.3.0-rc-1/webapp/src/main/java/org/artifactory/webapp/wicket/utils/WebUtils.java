package org.artifactory.webapp.wicket.utils;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.WebResponse;
import org.artifactory.api.repo.RepoPath;
import static org.artifactory.webapp.servlet.RepoFilter.ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH;
import static org.artifactory.webapp.servlet.RepoFilter.ATTR_ARTIFACTORY_REPOSITORY_PATH;
import org.artifactory.webapp.servlet.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class WebUtils {

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
        return webRequestCycle.getWebRequest();
    }

    public static WebResponse getWebResponse() {
        WebRequestCycle webRequestCycle = (WebRequestCycle) RequestCycle.get();
        return webRequestCycle.getWebResponse();
    }

    public static Map<String, String> getHeadersMap() {
        Map<String, String> map = new HashMap<String, String>();
        HttpServletRequest request = getWebRequest().getHttpServletRequest();
        if (request != null) {
            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                map.put(headerName.toUpperCase(), request.getHeader(headerName));
            }
        }
        return map;
    }

    public static RepoPath getRepoPath(WebRequest request) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        return (RepoPath) httpServletRequest.getAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH);
    }

    public static void removeRepoPath(WebRequest request, boolean storeAsRemoved) {
        HttpServletRequest httpServletRequest = request.getHttpServletRequest();
        RepoPath removedRepoPath = getRepoPath(request);
        httpServletRequest.removeAttribute(ATTR_ARTIFACTORY_REPOSITORY_PATH);
        if (removedRepoPath != null && storeAsRemoved) {
            httpServletRequest.setAttribute(ATTR_ARTIFACTORY_REMOVED_REPOSITORY_PATH, removedRepoPath);
        }
    }

    public static boolean isAuthPresent(WebRequest request) {
        return RequestUtils.isAuthHeaderPresent(request.getHttpServletRequest());
    }

}
