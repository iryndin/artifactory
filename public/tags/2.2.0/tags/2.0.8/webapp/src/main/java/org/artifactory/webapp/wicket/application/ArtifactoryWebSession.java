package org.artifactory.webapp.wicket.application;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.security.HttpAuthenticationDetails;
import org.artifactory.webapp.servlet.RequestUtils;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactoryWebSession extends AuthenticatedWebSession {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryWebSession.class);

    private AuthenticationManager authenticationManager;
    private Authentication authentication;
    private Roles roles;

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
        roles = null;
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        WebRequest webRequest = WebUtils.getWebRequest();
        HttpServletRequest servletRequest = webRequest.getHttpServletRequest();
        WebAuthenticationDetails details = new HttpAuthenticationDetails(servletRequest);
        authenticationToken.setDetails(details);
        boolean authenticated;
        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            authenticated = authentication.isAuthenticated();
            if (authenticated) {
                setWicketRoles(authentication);
                this.authentication = authentication;
                bindAuthentication();
                //Set a http session token so that we can reuse the login in direct repo browsing
                WebRequest request = WebUtils.getWebRequest();
                if (request != null) {
                    RequestUtils.setAuthentication(request.getHttpServletRequest(), authentication,
                            true);
                }
            }
        } catch (AuthenticationException e) {
            authenticated = false;
            if (log.isDebugEnabled()) {
                log.debug("Fail to authenticate " + username + "/" + DigestUtils.md5Hex(password), e);
            }
        }
        return authenticated;
    }

    private void setWicketRoles(Authentication authentication) {
        GrantedAuthority[] authorities = authentication.getAuthorities();
        String[] authorityRoles = new String[authorities.length];
        for (int i = 0; i < authorityRoles.length; i++) {
            GrantedAuthority authority = authorities[i];
            String role = authority.getAuthority();
            authorityRoles[i] = role;
        }
        roles = new Roles(authorityRoles);
    }

    @Override
    public void signOut() {
        super.signOut();
        roles = null;
        authentication = null;
        detach();
        //Reset the http session login token
        WebRequest request = WebUtils.getWebRequest();
        if (request != null) {
            RequestUtils.removeAuthentication(request.getHttpServletRequest());
        }
    }

    @Override
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

    private static class SecurityServiceLocator implements IProxyTargetLocator {
        public Object locateProxyTarget() {
            ArtifactoryContext context = ContextHelper.get();
            // get the "ui" authentication manager (no password encryption stuff)
            AuthenticationManager security = (AuthenticationManager) context.getBean("authenticationManager");
            return security;
        }
    }
}
