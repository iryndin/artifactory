package org.artifactory.security.props.auth;

import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
public class SshTokenManager extends PropsTokenManager {

    public static final String SSH_KEY = "ssh.basictoken";

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String OAUTH_TOKEN_PREFIX = "Bearer ";

    @Override
    protected String getPropKey() {
        return SSH_KEY;
    }
}
