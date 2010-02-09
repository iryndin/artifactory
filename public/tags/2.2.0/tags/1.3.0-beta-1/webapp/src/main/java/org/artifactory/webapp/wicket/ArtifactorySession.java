/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket;

import org.apache.log4j.Logger;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.WebAuthenticationDetails;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySession extends AuthenticatedWebSession {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactorySession.class);

    private AuthenticationManager authenticationManager;
    private Authentication authentication;
    private Roles roles;
    public final static String COOKIE_LAST_USER_ID = "artifactory:lastUserId";

    public static ArtifactorySession get() {
        return (ArtifactorySession) Session.get();
    }

    public ArtifactorySession(Request request) {
        super(request);
        authenticationManager = (AuthenticationManager) LazyInitProxyFactory.createProxy(
                AuthenticationManager.class, new AuthenticationManagerLocator());
    }

    public boolean authenticate(final String username, final String password) {
        roles = null;
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        WebRequest webRequest = WebUtils.getWebRequest();
        HttpServletRequest servletRequest = webRequest.getHttpServletRequest();
        WebAuthenticationDetails details = new WebAuthenticationDetails(servletRequest);
        authenticationToken.setDetails(details);
        boolean authenticated;
        try {
            Authentication authentication =
                    authenticationManager.authenticate(authenticationToken);
            authenticated = authentication.isAuthenticated();
            if (authenticated) {
                GrantedAuthority[] authorities = authentication.getAuthorities();
                String[] authorityRoles = new String[authorities.length];
                for (int i = 0; i < authorityRoles.length; i++) {
                    GrantedAuthority authority = authorities[i];
                    String role = authority.getAuthority();
                    authorityRoles[i] = role;
                }
                roles = new Roles(authorityRoles);
                this.authentication = authentication;
                bindAuthentication();
            }
        } catch (AuthenticationException e) {
            authenticated = false;
        }
        //Update the last logged in user cookie
        WebResponse response = WebUtils.getWebResponse();
        Cookie cookie = new Cookie(COOKIE_LAST_USER_ID, username);
        if (authenticated && !ArtifactorySecurityManager.USER_ANONYMOUS.equals(username)) {
            response.addCookie(cookie);
        } else {
            response.clearCookie(cookie);
        }
        return authenticated;
    }

    public void signOut() {
        super.signOut();
        roles = null;
        authentication = null;
        detach();
        //Update the last logged in user cookie
        WebResponse response = WebUtils.getWebResponse();
        Cookie cookie = new Cookie(COOKIE_LAST_USER_ID, "");
        response.clearCookie(cookie);
    }

    public Roles getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.hasRole(role);
    }

    //Starting from wicket 1.3 this is being called lazily and cannot be reliably used
    @Override
    protected void attach() {
        super.attach();
        bindAuthentication();
    }

    @Override
    protected void detach() {
        SecurityContextHolder.clearContext();
        super.detach();
    }

    private void bindAuthentication() {
        //Add the authentication to the request thread
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

    private static class AuthenticationManagerLocator implements IProxyTargetLocator {
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        public Object locateProxyTarget() {
            ArtifactoryContext context = ContextHelper.get();
            ArtifactorySecurityManager securityHelper = context.getSecurity();
            AuthenticationManager authenticationManager =
                    securityHelper.getAuthenticationManager();
            return authenticationManager;
        }
    }
}





