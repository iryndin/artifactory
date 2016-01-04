/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 */
package org.artifactory.webapp.servlet.authentication;

import org.artifactory.security.props.auth.PropsAuthenticationToken;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Chen Keinan
 */
public class PropsAuthenticationFilter implements ArtifactoryAuthenticationFilter {
    private static final Logger log = LoggerFactory.getLogger(PropsAuthenticationFilter.class);

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private AuthenticationProvider authenticationProvider;
    private BasicAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * @deprecated Use constructor injection
     */
    @Deprecated
    public PropsAuthenticationFilter() {
    }

    public PropsAuthenticationFilter(AuthenticationProvider authenticationprovider,
                                     BasicAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationProvider = authenticationprovider;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    public boolean requiresReAuthentication(ServletRequest request, Authentication authentication) {
        boolean requireAuthentication = true;
        HttpServletRequest req = (HttpServletRequest) request;
        TokenKeyValue tokenKeyValue = PropsAuthenticationHelper.getTokenKeyValueFromHeader(req);
        if (tokenKeyValue != null && authentication != null) {
            try {
                // try authenticate via token
                Authentication authFoundOnDB = tryAuthenticate(req, tokenKeyValue);
                if (authFoundOnDB != null) {
                    if (authFoundOnDB.getPrincipal().equals(authentication.getPrincipal())) {
                        requireAuthentication = false;
                    }
                }
            } catch (Exception e) {
                log.debug("re-authentication failed", e);
            }
            return requireAuthentication;
        }
        return acceptFilter(request);
    }

    @Override
    public boolean acceptFilter(ServletRequest request) {
        return PropsAuthenticationHelper.getTokenKeyValueFromHeader((HttpServletRequest) request) != null;
    }

    @Override
    public boolean acceptEntry(ServletRequest request) {
        return false;
    }

    @Override
    public String getCacheKey(ServletRequest request) {
        TokenKeyValue tokenKeyValue = PropsAuthenticationHelper.getTokenKeyValueFromHeader((HttpServletRequest) request);
        if (tokenKeyValue != null) {
            return tokenKeyValue.getToken();
        }
        return null;
    }

    /**
     * @param request        The http request
     * @return Login identifier such as user, sessionId, apiKey, etc.
     */
    @Override
    public String getLoginIdentifier(ServletRequest request) {
        TokenKeyValue tokenKeyValue = PropsAuthenticationHelper.getTokenKeyValueFromHeader((HttpServletRequest) request);
        return tokenKeyValue != null ? tokenKeyValue.getToken() : getCacheKey(request);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        try {
            // try authenticate
            TokenKeyValue tokenKeyValue = PropsAuthenticationHelper.getTokenKeyValueFromHeader(request);
            if (tokenKeyValue != null) {
                log.trace("try authenticate with {} : {}", tokenKeyValue.getKey(), tokenKeyValue.getToken());
                // try authenticate with prop token
                Authentication authResult = tryAuthenticate(request, tokenKeyValue);
                // update security context with new authentication
                updateContext(request, response, authResult);
                log.trace("authentication with props token {} : {} succeeded", tokenKeyValue.getKey()
                        , tokenKeyValue.getToken());
            }
        } catch (AuthenticationException failed) {
            // clear security context
            clearContext(request, response, failed);
            authenticationEntryPoint.commence(request, response, failed);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * clear security context from authentication data
     *
     * @param request  - http servlet request
     * @param response - http servlet response
     * @param failed   - authentication run time exception
     */
    private void clearContext(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        SecurityContextHolder.clearContext();
        log.debug("Authentication request for failed: " + failed);
        rememberMeServices.loginFail(request, response);
    }

    /**
     * update security context with new authentication
     *
     * @param request    - http servlet request
     * @param response   - http servlet response
     * @param authResult - new authentication
     */
    private void updateContext(HttpServletRequest request, HttpServletResponse response, Authentication authResult) {
        SecurityContextHolder.getContext().setAuthentication(authResult);
        rememberMeServices.loginSuccess(request, response, authResult);
    }


    /**
     * create pre authentication instance
     *
     * @param request - http servlet request
     * @param tokenKeyValue  - token key and value
     * @return authentication
     */
    private Authentication tryAuthenticate(HttpServletRequest request, TokenKeyValue tokenKeyValue) {
        // create pre authentication
        PropsAuthenticationToken authentication = new PropsAuthenticationToken(tokenKeyValue.getKey(),
                tokenKeyValue.getToken(), null);
        authentication.setDetails(authenticationDetailsSource.buildDetails(request));
        // try authenticate via token
        Authentication authResult = authenticationProvider.authenticate(authentication);
        return authResult;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
    }
}
