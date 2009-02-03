package org.artifactory.spring;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryContextThreadBinder {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryContextThreadBinder.class);

    private static final ThreadLocal<ArtifactoryContext> current =
            new ThreadLocal<ArtifactoryContext>();

    static ArtifactoryContext getArtifactoryContext() {
        return current.get();
    }

    public static void bind(ArtifactoryContext context) {
        current.set(context);
    }

    public static void unbind() {
        current.remove();
    }
}
