package org.artifactory.util.bearer;

import java.util.Map;

/**
 * Provides a bearer token to be used by {@link BearerScheme}
 *
 * @author Shay Yaakov
 */
public interface TokenProvider {

    String getToken(Map<String, String> challengeParams, String method, String uri, String repoKey);
}
