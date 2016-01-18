package org.artifactory.webapp.servlet.authentication;
import org.artifactory.security.props.auth.ApiKeyManager;
import org.artifactory.security.props.auth.OauthManager;
import org.artifactory.security.props.auth.model.TokenKeyValue;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Chen Keinan
 */
public class PropsAuthenticationHelper {

    public static TokenKeyValue getTokenKeyValueFromHeader(HttpServletRequest request) {
        TokenKeyValue tokenKeyValue;
        // 1st check if api key token exist
        if ((tokenKeyValue = getApiKeyTokenKeyValue(request)) != null) {
            return tokenKeyValue;
            // 2nd check if oauth token exist
        }
        if ((tokenKeyValue = getOauthTokenKeyValue(request)) != null) {
            return tokenKeyValue;
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
        String apiKeyValue = request.getHeader(ApiKeyManager.API_KEY_HEADER);
        if (apiKeyValue != null) {
            return new TokenKeyValue(ApiKeyManager.API_KEY, apiKeyValue);
        }
        return null;
    }

    /**
     * check weather oauth key is found on request
     *
     * @param request - http servlet request
     * @return Token key value
     */
    private static TokenKeyValue getOauthTokenKeyValue(HttpServletRequest request) {
        String oauthToken = request.getHeader(OauthManager.AUTHORIZATION_HEADER);
        if (oauthToken != null && oauthToken.startsWith(OauthManager.OAUTH_TOKEN_PREFIX) && oauthToken.length() > 8) {
            oauthToken = oauthToken.substring(7);
            return new TokenKeyValue(OauthManager.OAUTH_KEY, oauthToken);
        }
        return null;
    }
}
