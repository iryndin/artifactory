package org.artifactory.security.providermgr;

import org.springframework.security.authentication.AuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

/**
 * Model object for saving bearer tokens
 *
 * @author Chen Keinan
 */
public abstract class ArtifactoryCacheKey {
    private String user;
    protected String basicauth;
    private String repoKey;
    protected AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;

    public ArtifactoryCacheKey(String user, String basicauth, String repoKey,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        this.user = user;
        this.basicauth = basicauth;
        this.repoKey = repoKey;
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBasicauth() {
        return basicauth;
    }

    public void setBasicauth(String basicauth) {
        this.basicauth = basicauth;
    }

    public String getRepoKey() {
        return repoKey;
    }

    public void setRepoKey(String repoKey) {
        this.repoKey = repoKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArtifactoryCacheKey that = (ArtifactoryCacheKey) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (basicauth != null ? !basicauth.equals(that.basicauth) : that.basicauth != null) return false;
        return !(repoKey != null ? !repoKey.equals(that.repoKey) : that.repoKey != null);

    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (basicauth != null ? basicauth.hashCode() : 0);
        result = 31 * result + (repoKey != null ? repoKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TokenCacheKey{" +
                "scope='" + user + '\'' +
                ", realm='" + basicauth + '\'' +
                ", repoKey='" + repoKey + '\'' +
                '}';
    }

    public abstract ProviderMgr getProviderMgr();
}
