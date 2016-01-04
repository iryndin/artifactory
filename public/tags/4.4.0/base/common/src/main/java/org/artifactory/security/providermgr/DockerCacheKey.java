package org.artifactory.security.providermgr;

import org.springframework.security.authentication.AuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Chen Keinan
 */
public class DockerCacheKey extends ArtifactoryCacheKey {

    public DockerCacheKey(String user, String basicauth, String repoKey,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        super(user, basicauth, repoKey, authenticationDetailsSource);
    }

    @Override
    public ProviderMgr getProviderMgr() {
        return new DockerProviderMgr(authenticationDetailsSource, basicauth);
    }
}
