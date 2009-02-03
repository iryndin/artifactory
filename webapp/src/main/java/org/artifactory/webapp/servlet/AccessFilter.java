package org.artifactory.webapp.servlet;

import org.acegisecurity.InsufficientAuthenticationException;
import org.acegisecurity.ui.AuthenticationEntryPoint;
import org.acegisecurity.ui.digestauth.DigestProcessingFilter;
import org.acegisecurity.ui.digestauth.DigestProcessingFilterEntryPoint;
import org.acegisecurity.userdetails.UserDetailsService;
import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;

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
        CentralConfig cc = getCc();
        authFilter = new DigestProcessingFilter();
        //TODO: [by yl] Encode the pw. Use DigestUtils.md5Hex() to validate
        authFilter.setPasswordAlreadyEncoded(false);
        UserDetailsService userDetailsService = cc.getSecurity().getUserDetailsService();
        authFilter.setUserDetailsService(userDetailsService);
        DigestProcessingFilterEntryPoint entryPoint = new DigestProcessingFilterEntryPoint();
        entryPoint.setRealmName("Artifactory Realm");
        entryPoint.setNonceValiditySeconds(10);
        entryPoint.setKey("kofifo");
        try {
            entryPoint.afterPropertiesSet();
            authFilter.setAuthenticationEntryPoint(entryPoint);
            authFilter.afterPropertiesSet();
        } catch (Exception e) {
            throw new ServletException("Failed to set up HTTP authentication filtering.");
        }
    }

    public void destroy() {
        authFilter.destroy();
        super.destroy();
    }

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        final String servletPath = request.getServletPath();
        if (isRepoRequest(servletPath)) {
            //If there is no authentication info on the request, ask for it and return
            if (isAuthRequired(req)) {
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

    private boolean isAuthRequired(ServletRequest req) {
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        String header = httpRequest.getHeader("Authorization");
        boolean authExists = header != null && header.startsWith("Digest ");
        return !authExists;
    }
}
