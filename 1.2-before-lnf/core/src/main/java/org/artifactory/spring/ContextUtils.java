package org.artifactory.spring;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ContextUtils {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ContextUtils.class);

    public static ArtifactoryContext getArtifactoryContext() {
        return ArtifactoryContextThreadBinder.getArtifactoryContext();
    }
}
