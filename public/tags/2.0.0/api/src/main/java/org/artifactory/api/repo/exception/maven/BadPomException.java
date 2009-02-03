package org.artifactory.api.repo.exception.maven;

/**
 * @author yoavl
 */
public class BadPomException extends Exception {

    public BadPomException(String message) {
        super(message);
    }

    public BadPomException(Throwable cause) {
        super(cause);
    }

    public BadPomException(String message, Throwable cause) {
        super(message, cause);
    }
}