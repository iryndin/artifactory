package org.artifactory.api.repo.exception;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class RepositoryRuntimeException extends RuntimeException {

    public RepositoryRuntimeException(String message) {
        super(message);
    }

    public RepositoryRuntimeException(Throwable cause) {
        super(cause);
    }

    public RepositoryRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
