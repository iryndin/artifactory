package org.artifactory.security.props.auth;

import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
public class ApiKeyManager extends PropsTokenManager {

    public static final String API_KEY = "apiKey";

    public static final String API_KEY_HEADER = "X-Api-Key";

    @Override
    protected String getPropKey() {
        return API_KEY;
    }
}
