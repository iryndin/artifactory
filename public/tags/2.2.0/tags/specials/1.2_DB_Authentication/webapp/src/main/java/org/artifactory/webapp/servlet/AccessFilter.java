package org.artifactory.webapp.servlet;

import org.acegisecurity.InsufficientAuthenticationException;
import org.acegisecurity.ui.AuthenticationEntryPoint;
import org.acegisecurity.ui.digestauth.DigestProcessingFilter;
import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AccessFilter extends ArtifactoryFilter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(AccessFilter.class);

    private DigestProcessingFilter authFilter;

    @SuppressWarnings({"unchecked"})
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ArtifactoryContext context = getContext();
        //TODO: [by yl] Encode the pw. Use DigestUtils.md5Hex() to validate
        authFilter = context.beanForType(DigestProcessingFilter.class);
    }

    public void destroy() {
        authFilter.destroy();
        super.destroy();
    }

    public void doFilterInternal(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        final String servletPath = request.getServletPath();
        boolean repoRequest = isRepoRequest(servletPath);
        //If we are getting a get request check if anonymous downloads are allowed
        ArtifactoryContext context = getContext();
        boolean anonDownloadsAllowed = context.getCentralConfig().isAnonDownloadsAllowed();
        String method = request.getMethod();
        boolean get = "get".equalsIgnoreCase(method);
        boolean allowAnonymousGet = get && anonDownloadsAllowed;
        if (repoRequest && !allowAnonymousGet) {
            //If there is no authentication info on the request, ask for it and return
            String header = request.getHeader("Authorization");
            boolean authExists = header != null && header.startsWith("Digest ");
            if (!authExists) {
                AuthenticationEntryPoint point = authFilter.getAuthenticationEntryPoint();
                point.commence(req, resp,
                        new InsufficientAuthenticationException(
                                "Digest authentication is required."));
            } else {
                authFilter.doFilter(req, resp, chain);
            }
        } else {
            chain.doFilter(req, resp);
        }
    }
}
