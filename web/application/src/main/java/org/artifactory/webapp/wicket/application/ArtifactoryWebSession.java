/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.application;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.api.util.Pair;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.HttpAuthenticationDetails;
import org.artifactory.webapp.servlet.RequestUtils;
import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryWebSession extends AuthenticatedWebSession {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryWebSession.class);

    private AuthenticationManager authenticationManager;
    private Authentication authentication;
    private Roles roles;
    private Map<String, List<FileInfo>> results;
    private Pair<String, Long> lastLoginInfo;

    public static ArtifactoryWebSession get() {
        return (ArtifactoryWebSession) Session.get();
    }

    public ArtifactoryWebSession(Request request) {
        super(request);
        authenticationManager = (AuthenticationManager) LazyInitProxyFactory.createProxy(
                AuthenticationManager.class, new SecurityServiceLocator());
    }

    @Override
    public boolean authenticate(final String username, final String password) {
        if (authentication != null && authentication.isAuthenticated() &&
                ("" + authentication.getPrincipal()).equals(username)) {
            return true;
        }

        roles = null;
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        WebRequest webRequest = WicketUtils.getWebRequest();
        HttpServletRequest servletRequest = webRequest.getHttpServletRequest();
        WebAuthenticationDetails details = new HttpAuthenticationDetails(servletRequest);
        authenticationToken.setDetails(details);
        boolean authenticated;
        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            authenticated = authentication.isAuthenticated();
            if (authenticated) {
                setAuthentication(authentication);
            }
        } catch (AuthenticationException e) {
            authenticated = false;
            AccessLogger.loginDenied(authenticationToken);
            if (log.isDebugEnabled()) {
                log.debug("Fail to authenticate " + username + "/" + DigestUtils.md5Hex(password), e);
            }
        }
        return authenticated;
    }

    private void setWicketRoles(Authentication authentication) {
        Collection<GrantedAuthority> authorities = authentication.getAuthorities();
        String[] authorityRoles = new String[authorities.size()];
        int i = 0;
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            authorityRoles[i] = role;
            i++;
        }
        roles = new Roles(authorityRoles);
    }

    @Override
    public void signOut() {
        super.signOut();
        //Remove the authentication attribute early
        RequestUtils.removeAuthentication(WicketUtils.getWebRequest().getHttpServletRequest());
        // clear authentication and authorization data saved in this session
        // (this session will still be used in the logout page)
        roles = null;
        authentication = null;
        // invalidate the wicket and http session
        invalidate();
        // detach session and clear the security context - will call invalidateNow()
        detach();
    }

    void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
        if (authentication.isAuthenticated()) {
            setWicketRoles(authentication);
            //Log authentication if not anonymous
            if (!isAnonymous()) {
                AccessLogger.loggedIn(authentication);
            }
            //Set a http session token so that we can reuse the login in direct repo browsing
            WebRequest request = WicketUtils.getWebRequest();
            if (request != null) {
                HttpServletRequest httpServletRequest = request.getHttpServletRequest();
                RequestUtils.setAuthentication(httpServletRequest, authentication, true);

                ArtifactoryContext context = ContextHelper.get();
                SecurityService securityService = context.beanForType(SecurityService.class);

                Object principal = authentication.getPrincipal();
                if (principal != null) {

                    String username = principal.toString();
                    if (StringUtils.isNotBlank(username) && (!username.equals(UserInfo.ANONYMOUS))) {

                        //Save the user's last login info in the web session so we can display it in the welcome page
                        Pair<String, Long> lastLoginInfo = securityService.getUserLastLoginInfo(username);
                        ArtifactoryWebSession.get().setLastLoginInfo(lastLoginInfo);

                        //Update the user's current login info in the database
                        String remoteAddress = new HttpAuthenticationDetails(httpServletRequest).getRemoteAddress();
                        securityService.updateUserLastLogin(username, remoteAddress, System.currentTimeMillis());
                    }
                }
            }
            //Update the spring  security context
            bindAuthentication();
        }
        dirty();
    }

    /**
     * @return True is anonymous user is logged in to this session.
     */
    boolean isAnonymous() {
        return authentication != null && UserInfo.ANONYMOUS.equals(authentication.getPrincipal().toString());
    }

    @Override
    public Roles getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.hasRole(role);
    }

    public void setResults(String resultsName, List<FileInfo> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return;
        }

        AuthorizationService authService = ContextHelper.get().getAuthorizationService();
        if (authService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            getResults().put(resultsName, artifacts);
        }
    }

    public List<FileInfo> getResults(String resultsName) {
        if (results != null) {
            return getResults().get(resultsName);
        } else {
            return null;
        }
    }

    public void removeResult(String resultsName) {
        if (results != null) {
            getResults().remove(resultsName);
        }
    }

    public Set<String> getResultNames() {
        if (results == null) {
            return Collections.emptySet();
        }
        return getResults().keySet();
    }

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    public Pair<String, Long> getLastLoginInfo() {
        return lastLoginInfo;
    }

    public void setLastLoginInfo(Pair<String, Long> lastLoginInfo) {
        this.lastLoginInfo = lastLoginInfo;
    }

    private Map<String, List<FileInfo>> getResults() {
        if (results == null) {
            results = new HashMap<String, List<FileInfo>>();
        }
        return results;
    }

    @Override
    protected void detach() {
        SecurityContextHolder.clearContext();
        super.detach();
    }

    void bindAuthentication() {
        //Add the authentication to the request thread
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }

    private static class SecurityServiceLocator implements IProxyTargetLocator {
        public Object locateProxyTarget() {
            ArtifactoryContext context = ContextHelper.get();
            // get the "ui" authentication manager (no password encryption stuff)
            AuthenticationManager security = (AuthenticationManager) context.getBean("authenticationManager");
            return security;
        }
    }
}