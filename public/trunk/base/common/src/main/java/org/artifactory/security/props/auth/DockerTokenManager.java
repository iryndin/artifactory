package org.artifactory.security.props.auth;

import org.springframework.stereotype.Component;

/**
 * @author Chen Keinan
 */
@Component
public class DockerTokenManager extends PropsTokenManager {

    public static final String DOCKER_TOKEN_KEY = "docker.basictoken";

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String OAUTH_TOKEN_PREFIX = "Bearer ";

    @Override
    protected String getPropKey() {
        return DOCKER_TOKEN_KEY;
    }
}
