package org.artifactory.cli.common;

/**
 * TODO: documentation
 *
 * @author Noam Tenne
 */
public class RemoteCommandException extends RuntimeException {
    public RemoteCommandException() {
    }

    public RemoteCommandException(String message) {
        super(message);
    }

    public RemoteCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteCommandException(Throwable cause) {
        super(cause);
    }
}
