package org.artifactory.jcr;

import org.apache.log4j.Logger;

/**
 * A thread local holder class for jcr sessions
 */
class JcrSessionThreadBinder {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrSessionThreadBinder.class);

    private static final ThreadLocal<JcrSessionWrapper> current =
            new ThreadLocal<JcrSessionWrapper>();

    public static JcrSessionWrapper getSession() {
        return current.get();
    }

    public static void bind(JcrSessionWrapper session) {
        current.set(session);
    }

    public static void unbind() {
        current.remove();
    }
}
