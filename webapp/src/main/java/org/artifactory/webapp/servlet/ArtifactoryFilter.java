package org.artifactory.webapp.servlet;

import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ArtifactoryContextThreadBinder;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class ArtifactoryFilter implements Filter {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactoryFilter.class);

    public static final String WEBAPP_URL_PATH_PREFIX = "webapp";

    public static final Set<String> WEBAPP_URL_PATH_PREFIXES = new HashSet<String>();

    static {
        WEBAPP_URL_PATH_PREFIXES.add(WEBAPP_URL_PATH_PREFIX);
        WEBAPP_URL_PATH_PREFIXES.add("skins");
        WEBAPP_URL_PATH_PREFIXES.add("images");
        WEBAPP_URL_PATH_PREFIXES.add("js");
        WEBAPP_URL_PATH_PREFIXES.add("css");
    }

    private ArtifactoryContext context;

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context = (ArtifactoryContext) WebApplicationContextUtils
                .getRequiredWebApplicationContext(servletContext);
    }

    public void destroy() {
    }

    protected ArtifactoryContext getContext() {
        return context;
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
        return pathPrefix != null && pathPrefix.length() > 0 &&
                !WEBAPP_URL_PATH_PREFIXES.contains(pathPrefix);
    }

    public final void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        ArtifactoryContextThreadBinder.bind(context);
        doFilterInternal(req, resp, chain);
        ArtifactoryContextThreadBinder.unbind();
    }

    protected abstract void doFilterInternal(
            ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException;
}
