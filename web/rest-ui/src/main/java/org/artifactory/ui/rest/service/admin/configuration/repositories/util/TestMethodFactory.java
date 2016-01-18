package org.artifactory.ui.rest.service.admin.configuration.repositories.util;


import com.google.common.base.Charsets;
import com.sun.istack.internal.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;

import java.net.URISyntaxException;
import java.util.List;

import static org.artifactory.request.ArtifactoryRequest.ARTIFACTORY_ORIGINATED;

/**
 * Create Http request for test replication button
 *
 * @author Aviad Shikloshi
 */
public class TestMethodFactory {

    public static HttpRequestBase createTestMethod(String repoUrl, RepoType repoType, @Nullable String queryParams) {

        if (repoType == null) {
            throw new RuntimeException("Missing repository type");
        }
        HttpRequestBase request;
        switch (repoType) {
            case NuGet:
                request = createNuGetTestMethod(repoUrl, queryParams);
                break;
            case Gems:
                request = createGemsTestMethod(repoUrl);
                break;
            case Docker:
                request = createDockerTestMethod(repoUrl);
                break;
            default:
                request = new HttpHead(HttpUtils.encodeQuery(repoUrl));
                //Add the current requester host id
        }
        addOriginatedHeader(request);
        return request;
    }

    /**
     * add originated header to request
     *
     * @param request - http servlet request
     */
    public static void addOriginatedHeader(HttpRequestBase request) {
        String hostId = ContextHelper.get().beanForType(AddonsManager.class).addonByType(HaCommonAddon.class).getHostId();
        request.addHeader(ARTIFACTORY_ORIGINATED, hostId);
    }

    private static HttpRequestBase createGemsTestMethod(String repoUrl) {
        String path = repoUrl;
        if (path.endsWith("/")) {
            path = PathUtils.trimTrailingSlashes(path);
        }
        path += "/api/v1/dependencies";
        return new HttpGet(path);
    }

    private static HttpRequestBase createNuGetTestMethod(String repoUrl, String queryParams) {
        try {
            URIBuilder uriBuilder = new URIBuilder(repoUrl);
            HttpRequestBase request = new HttpGet();
            if(StringUtils.isNotBlank(queryParams)) {
                List<NameValuePair> queryParamsMap = URLEncodedUtils.parse(queryParams, Charsets.UTF_8);
                uriBuilder.setParameters(queryParamsMap);
            }
            request.setURI(uriBuilder.build());
            return request;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to build test URI", e);
        }
    }

    private static HttpRequestBase createDockerTestMethod(String repoUrl) {
        String path = repoUrl;
        if (path.endsWith("/")) {
            path = PathUtils.trimTrailingSlashes(path);
        }
        path += "/v2/";
        return new HttpGet(path);
    }

    private TestMethodFactory() {
    }

}
