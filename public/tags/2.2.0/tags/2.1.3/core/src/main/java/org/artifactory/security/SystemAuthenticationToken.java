package org.artifactory.security;

import org.artifactory.api.security.UserInfo;
import org.springframework.security.providers.AbstractAuthenticationToken;

import java.io.Serializable;

/**
 * An authentication object to be used when needing authentication details for system tasks
 *
 * @author Noam Y. Tenne
 */
public class SystemAuthenticationToken extends AbstractAuthenticationToken implements Serializable {

    public Object getCredentials() {
        return "";
    }

    public Object getPrincipal() {
        return new SimpleUser(new UserInfo("system") {
            @Override
            public boolean isAdmin() {
                return true;
            }
        });
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}
