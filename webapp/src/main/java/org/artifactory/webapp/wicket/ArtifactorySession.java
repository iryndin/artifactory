package org.artifactory.webapp.wicket;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.AuthenticationProvider;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.userdetails.UserDetailsService;
import org.apache.log4j.Logger;
import org.artifactory.security.SecurityHelper;
import wicket.authentication.AuthenticatedWebApplication;
import wicket.authentication.AuthenticatedWebSession;
import wicket.authorization.strategies.role.Roles;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySession extends AuthenticatedWebSession {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactorySession.class);

    private transient DaoAuthenticationProvider authenticationProvider;
    private Authentication authentication;
    private Roles roles;

    /**
     * Required since instantiated by reflection using this signature
     *
     * @param application
     */
    public ArtifactorySession(final AuthenticatedWebApplication application) {
        this((ArtifactoryApp) application);
    }

    public ArtifactorySession(ArtifactoryApp application) {
        super(application);
        getOrCreateAuthenticationProvider();
    }

    public boolean authenticate(final String username, final String password) {
        roles = null;
        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        boolean authenticated;
        try {
            AuthenticationProvider authenticationProvider = getOrCreateAuthenticationProvider();
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

    private AuthenticationProvider getOrCreateAuthenticationProvider() {
        if (authenticationProvider == null) {
            ArtifactoryApp application = (ArtifactoryApp) getApplication();
            authenticationProvider = new DaoAuthenticationProvider();
            SecurityHelper security = application.getCc().getSecurity();
            UserDetailsService userDetailsService = security.getUserDetailsService();
            authenticationProvider.setUserDetailsService(userDetailsService);
            try {
                authenticationProvider.afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate ArtifactorySession.", e);
            }
        }
        return authenticationProvider;
    }
}
