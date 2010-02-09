package org.artifactory.webapp.servlet;

import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryFilter implements Filter {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryFilter.class);

    private CentralConfig cc;

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        cc = (CentralConfig) servletContext.getAttribute(CentralConfig.ATTR_CONFIG);
    }

    public void destroy() {
    }

    protected CentralConfig getCc() {
        return cc;
    }

    protected String getPathPrefix(String servletPath) {
        String pathPrefix = null;
        if (servletPath != null) {
            int pathPrefixEnd = servletPath.indexOf('/', 1);
            if (pathPrefixEnd > 0) {
                pathPrefix = servletPath.substring(1, pathPrefixEnd);
            }
        }
        return pathPrefix;
    }

    protected boolean isRepoRequest(String servletPath) {
        String pathPrefix = getPathPrefix(servletPath);
        return pathPrefix != null && pathPrefix.endsWith(CentralConfig.REPO_URL_PATH_PREFIX);
    }
}
