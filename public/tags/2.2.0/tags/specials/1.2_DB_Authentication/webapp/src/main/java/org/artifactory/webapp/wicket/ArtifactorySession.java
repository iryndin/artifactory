package org.artifactory.webapp.wicket;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.apache.log4j.Logger;
import org.artifactory.security.SecurityHelper;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import wicket.Request;
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
    }

    public Roles getRoles() {
        return roles;
    }

    public Object getPrincipal() {
        return authentication != null ? authentication.getPrincipal() : null;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.hasRole(role);
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





