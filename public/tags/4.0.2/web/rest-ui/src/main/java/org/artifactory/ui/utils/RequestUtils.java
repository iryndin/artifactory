package org.artifactory.ui.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;

/**
 * @author Chen keinan
 */
public class RequestUtils {

    public static final String REPO_KEY_PARAM = "repoKey";
    public static final String PATH_PARAM = "path";

    /**
     * return request headers map
     *
     * @param request - http servlet request
     * @return - map of request headers
     */
    public static Map<String, String> getHeadersMap(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        if (request != null) {
            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                map.put(headerName.toUpperCase(), request.getHeader(headerName));
            }
        }
        return map;
    }

    public static RepoPath getPathFromRequest(ArtifactoryRestRequest request) {
        return InternalRepoPathFactory.create(request.getQueryParamByKey(REPO_KEY_PARAM),
                request.getQueryParamByKey(PATH_PARAM));
    }

    public static String getRepoKeyFromRequest(ArtifactoryRestRequest request) {
        return request.getQueryParamByKey(REPO_KEY_PARAM);
    }
}
