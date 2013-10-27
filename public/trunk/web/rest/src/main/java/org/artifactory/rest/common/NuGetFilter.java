package org.artifactory.rest.common;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * TODO: Documentation
 *
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class NuGetFilter implements ContainerRequestFilter {

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        try {
            /*String path = request.getPath();
            if ("GET".equals(request.getMethod()) && path.startsWith(NuGetResourceConstants.PATH_ROOT)) {
                if (!path.matches(".+/[Dd]ownload.+")) {
                    String pathWithoutNuGetPrefix = PathUtils.stripFirstPathElement(path);
                    String repoKey = PathUtils.getFirstPathElement(pathWithoutNuGetPrefix);
                    String pathWithoutRepoKey = PathUtils.stripFirstPathElement(pathWithoutNuGetPrefix);
                    UriBuilder uriBuilder = UriBuilder.fromUri(request.getBaseUri()).path(pathWithoutRepoKey);
                    MultivaluedMap<String, String> queryParameters = request.getQueryParameters();
                    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                        String key = entry.getKey();
                        List<String> values = entry.getValue();
                        for (String value : values) {
                            if (StringUtils.equals(value, "IsLatestVersion")) {
                                value = "IsLatestVersion eq true";
                            } else if (StringUtils.equals(value, "IsAbsoluteLatestVersion")) {
                                value = "IsAbsoluteLatestVersion eq true";
                            }
                            uriBuilder.queryParam(key, value);
                        }
                    }
                    request.setUris(request.getBaseUri(), uriBuilder.build());
                    MultivaluedMap<String, String> requestHeaders = request.getRequestHeaders();
                    requestHeaders.add("repoKey", repoKey);
                    request.setHeaders((InBoundHeaders) requestHeaders);
                }
            }*/
            return request;
        } catch (Exception e) {
            // add proper handling
        }

        return null;
    }
}
