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
public class ApiKeyManager implements TokenManager {
    private static final Logger log = LoggerFactory.getLogger(ApiKeyManager.class);

    public static final String API_KEY = "apiKey";

    @Autowired
    UserGroupService userGroupService;

    @Override
    public TokenKeyValue createToken(String userName) {
        TokenKeyValue token = null;
        String apiKey = null;
        try {
            apiKey = CryptoHelper.generateUniqueApiKey();
            boolean propsToken = userGroupService.createPropsToken(userName, API_KEY, apiKey);
            if (propsToken) {
                token = new TokenKeyValue(apiKey);
            }
        } catch (GeneralSecurityException e) {
            log.debug("error with generating token for :{}", API_KEY);
        } catch (SQLException e) {
            log.debug("error with adding {} :{} to DB", API_KEY, apiKey);
        }
        return token;
    }

    @Override
    public TokenKeyValue refreshToken(String userName) {
        TokenKeyValue token = null;
        String apiKey = null;
        try {
            apiKey = CryptoHelper.generateUniqueApiKey();
            boolean propsToken = userGroupService.updatePropsToken(userName, API_KEY, apiKey);
            if (propsToken) {
                token = new TokenKeyValue(apiKey);
            }
        } catch (GeneralSecurityException e) {
            log.debug("error with refreshing token for :{}", API_KEY);
        } catch (SQLException e) {
            log.debug("error with refreshing {} :{} to DB", API_KEY, apiKey);
        }
        return token;
    }

    @Override
    public boolean revokeToken(String userName) {
        boolean tokenRevokeSucceeded = false;
        try {
            tokenRevokeSucceeded = userGroupService.revokePropsToken(userName, API_KEY);
        } catch (SQLException e) {
            log.debug("error with revoking token for :{}", API_KEY);
        }
        return tokenRevokeSucceeded;
    }

    @Override
    public boolean revokeAllToken() {
        boolean tokenRevokeSucceeded = false;
        try {
            userGroupService.revokeAllPropsTokens(API_KEY);
            tokenRevokeSucceeded = true;
        } catch (SQLException e) {
            log.debug("error with revoking token for :{}", API_KEY);
        }
        return tokenRevokeSucceeded;
    }
}
