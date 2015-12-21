package org.artifactory.security.props.auth;

import org.springframework.security.core.AuthenticationException;

/**
 * @author Chen Keinan
 */
public class BadPropsAuthException extends AuthenticationException {
    public BadPropsAuthException(String msg) {
        super(msg);
    }
}
