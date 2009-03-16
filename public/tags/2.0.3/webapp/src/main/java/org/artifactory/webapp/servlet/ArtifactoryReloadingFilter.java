package org.artifactory.webapp.servlet;

import org.apache.wicket.application.ReloadingClassLoader;
import org.apache.wicket.protocol.http.ReloadingWicketFilter;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryReloadingFilter extends ReloadingWicketFilter {

    static {
        ReloadingClassLoader.includePattern("org.artifactory.webapp.*");
    }
}

