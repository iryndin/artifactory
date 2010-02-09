package org.artifactory.webapp.servlet;

import org.apache.log4j.Logger;
import org.apache.wicket.application.ReloadingClassLoader;
import org.apache.wicket.protocol.http.ReloadingWicketFilter;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryReloadingFilter extends ReloadingWicketFilter {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryReloadingFilter.class);

    static {
        ReloadingClassLoader.includePattern("org.artifactory.webapp.*");
    }
}

