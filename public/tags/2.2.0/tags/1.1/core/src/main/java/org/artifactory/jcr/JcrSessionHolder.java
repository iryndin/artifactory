package org.artifactory.jcr;

import org.apache.log4j.Logger;

/**
 * A thread local holder class for jcr sessions
 */
public class JcrSessionHolder {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrSessionHolder.class);

    private static final ThreadLocal<JcrSessionWrapper> current =
            new ThreadLocal<JcrSessionWrapper>();

    public static JcrSessionWrapper getSession() {
        return current.get();
    }

    public static void setSession(JcrSessionWrapper session) {
        current.set(session);
    }

    public static void reset() {
        current.remove();
    }
}
