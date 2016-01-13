package org.artifactory.security.props.auth;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.security.props.auth.model.TokenKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 * @author Chen Keinan
 */
@Component
public abstract class PropsTokenManager implements TokenManager {
    private static final Logger log = LoggerFactory.getLogger(PropsTokenManager.class);

    @Autowired
    UserGroupService userGroupService;

    @Override
    public TokenKeyValue createToken(String userName) {
        TokenKeyValue token = null;
        String tokenValue;
        String key = getPropKey();
        try {
            tokenValue = CryptoHelper.generateUniqueToken();
            boolean propsToken = userGroupService.createPropsToken(userName, key, tokenValue);
            if (propsToken) {
                token = new TokenKeyValue(tokenValue);
            }
        } catch (GeneralSecurityException e) {
            log.debug("error with generating token for :{}", key);
        } catch (SQLException e) {
            log.debug("error with adding {} :{} to DB", key, token);
        }
        return token;
    }

    @Override
    public TokenKeyValue addExternalToken(String userName, String tokenValue) {
        TokenKeyValue token = null;
        String key = getPropKey();
        try {
            boolean propsToken = userGroupService.createPropsToken(userName, key, tokenValue);
            if (propsToken) {
                token = new TokenKeyValue(tokenValue);
            }
        } catch (SQLException e) {
            log.debug("error with adding {} :{} to DB", key, token);
        }
        return token;
    }

    /**
     * return props key for each token type (oauth , apiKey and etc)
     *
     * @return key
     */
    protected abstract String getPropKey();

    @Override
    public TokenKeyValue refreshToken(String userName) {
        String key = getPropKey();
        TokenKeyValue token = null;
        String value = null;
        try {
            value = CryptoHelper.generateUniqueToken();
            boolean propsToken = userGroupService.updatePropsToken(userName, key, value);
            if (propsToken) {
                token = new TokenKeyValue(value);
            }
        } catch (GeneralSecurityException e) {
            log.debug("error with refreshing token for :{}", key);
        } catch (SQLException e) {
            log.debug("error with refreshing {} :{} to DB", key, value);
        }
        return token;
    }

    @Override
    public boolean revokeToken(String userName) {
        boolean tokenRevokeSucceeded = false;
        String key = getPropKey();
        try {
            tokenRevokeSucceeded = userGroupService.revokePropsToken(userName, key);
        } catch (SQLException e) {
            log.debug("error with revoking token for :{}", key);
        }
        return tokenRevokeSucceeded;
    }

    @Override
    public boolean revokeAllToken() {
        boolean tokenRevokeSucceeded = false;
        String key = getPropKey();
        try {
            userGroupService.revokeAllPropsTokens(key);
            tokenRevokeSucceeded = true;
        } catch (SQLException e) {
            log.debug("error with revoking token for :{}", key);
        }
        return tokenRevokeSucceeded;
    }
}
