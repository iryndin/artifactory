package org.artifactory.security.props.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * @author Chen Keinan
 */
public class PropsAuthNotFoundException extends AuthenticationException {
    public PropsAuthNotFoundException(String msg) {
        super(msg);
    }
}
