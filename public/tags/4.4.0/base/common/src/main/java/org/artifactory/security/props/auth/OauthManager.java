package org.artifactory.security.props.auth;

import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
public class OauthManager extends PropsTokenManager {

    public static final String OAUTH_KEY = "basictoken";

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String OAUTH_TOKEN_PREFIX = "Bearer ";

    @Override
    protected String getPropKey() {
        return OAUTH_KEY;
    }
}
