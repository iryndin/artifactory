package org.artifactory.security.providermgr;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.artifactory.common.ConstantValues;
import org.artifactory.security.props.auth.model.OauthErrorModel;
import org.artifactory.security.props.auth.model.OauthModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * artifactory specific token cache implementation ,
 * The tokens are cached in-memory and auto expire according to the
 *
 * @author Chen Keinan
 */
@Component
public class ArtifactoryTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryTokenProvider.class);

    private LoadingCache<ArtifactoryCacheKey, OauthModel> tokens;

    @PostConstruct
    public void initTokensCache() {
        tokens = CacheBuilder.newBuilder()
                .initialCapacity(1000)
                .expireAfterWrite(ConstantValues.genericTokensCacheIdleTimeSecs.getLong(), TimeUnit.SECONDS)
                .build(new CacheLoader<ArtifactoryCacheKey, OauthModel>() {
                    @Override
                    public OauthModel load(ArtifactoryCacheKey key) throws Exception {
                        return fetchNewToken(key);
                    }
                });
    }

    public OauthModel getToken(ArtifactoryCacheKey artifactoryCacheKey) {
        String key = null;
        OauthModel oauthModel = null;
        try {
            log.trace("Getting token for " + key);
            oauthModel = tokens.get(artifactoryCacheKey);
            return oauthModel;
        } catch (ExecutionException e) {
            throw new RuntimeException("Could not get token from cache for " + key, e);
        } finally {
            if (oauthModel != null) {
                if (oauthModel instanceof OauthErrorModel) {
                    tokens.invalidate(artifactoryCacheKey);
                }
            }
        }
    }

    private OauthModel fetchNewToken(ArtifactoryCacheKey artifactoryCacheKey) {
        return artifactoryCacheKey.getProviderMgr().fetchTokenFromProvider();
    }
}
