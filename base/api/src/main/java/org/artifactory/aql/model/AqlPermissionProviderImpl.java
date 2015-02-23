package org.artifactory.aql.model;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;

/**
 * @author Gidi Shabat
 */
public class AqlPermissionProviderImpl implements AqlPermissionProvider {

    private AuthorizationService authorizationService;

    public AuthorizationService getProvider() {
        if (authorizationService == null) {
            authorizationService = ContextHelper.get().getAuthorizationService();
        }
        return authorizationService;
    }

    @Override
    public boolean canRead(RepoPath repoPath) {
        return getProvider().canRead(repoPath);
    }

    @Override
    public boolean isAdmin() {
        return getProvider().isAdmin();
    }
}