package org.artifactory.webapp.wicket;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.apache.log4j.Logger;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import wicket.Request;
import wicket.Session;
import wicket.authentication.AuthenticatedWebApplication;
import wicket.authentication.AuthenticatedWebSession;
import wicket.authorization.strategies.role.Roles;
import wicket.proxy.IProxyTargetLocator;
import wicket.proxy.LazyInitProxyFactory;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySession extends AuthenticatedWebSession {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactorySession.class);

    private AuthenticationProvider authenticationProvider;
    private Authentication authentication;
    private Roles roles;

    public static ArtifactorySession get() {
        return (ArtifactorySession) Session.get();
    }

    public ArtifactorySession(final AuthenticatedWebApplication application, Request request) {
        super(application, request);
        authenticationProvider = (AuthenticationProvider) LazyInitProxyFactory.createProxy(
                AuthenticationProvider.class, new AuthenticationProviderLocator());
    }

    public boolean authenticate(final String username, final String password) {
        roles = null;
        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        boolean authenticated;
        try {
            Authentication authentication =
                    authenticationProvider.authenticate(authenticationToken);
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
            }
        } catch (AuthenticationException e) {
            authenticated = false;
        }
        return authenticated;
    }

    public void signOut() {
        super.signOut();
        roles = null;
        authentication = null;
        detach();
    }

    public Roles getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.hasRole(role);
    }

    @Override
    protected void attach() {
        super.attach();
        //Add the authentication to the request thread
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
    }


    @Override
    protected void detach() {
        SecurityContextHolder.clearContext();
        super.detach();
    }

    private static class AuthenticationProviderLocator implements IProxyTargetLocator {
        @SuppressWarnings({"UnnecessaryLocalVariable"})
        public Object locateProxyTarget() {
            ArtifactoryContext context = ContextUtils.getArtifactoryContext();
            SecurityHelper securityHelper = context.getSecurity();
            AuthenticationProvider authenticationProvider =
                    securityHelper.getAuthenticationProvider();
            return authenticationProvider;
        }
    }
}





