package org.artifactory.webapp.servlet.authentication;

import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.model.TokenKeyValue;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Chen Keinan
 */
public class PropsAuthenticationHelper {
    public static String API_KEY_HEADER = "X-Api-Key";


    public static TokenKeyValue getTokenKeyValue(HttpServletRequest request) {
        TokenKeyValue apiKeyValue;
        // 1st check if api key token exist
        if ((apiKeyValue = getApiKeyTokenKeyValue(request)) != null) {
            return apiKeyValue;
        }
        return null;
    }

    /**
     * check weather api key is found on request
     *
     * @param request - http servlet request
     * @return Token key value
     */
    private static TokenKeyValue getApiKeyTokenKeyValue(HttpServletRequest request) {
        String apiKeyValue = request.getHeader(API_KEY_HEADER);
        if (apiKeyValue != null) {
            return new TokenKeyValue(ApiKeyManager.API_KEY, apiKeyValue);
        }
        return null;
    }
}
