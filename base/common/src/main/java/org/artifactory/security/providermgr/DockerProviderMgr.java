package org.artifactory.security.providermgr;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.security.LoginHandler;
import org.artifactory.security.props.auth.OauthManager;
import org.artifactory.security.props.auth.model.OauthErrorEnum;
import org.artifactory.security.props.auth.model.OauthErrorModel;
import org.artifactory.security.props.auth.model.OauthModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author Chen Keinan
 */
public class DockerProviderMgr implements ProviderMgr {
    private static final Logger log = LoggerFactory.getLogger(DockerProviderMgr.class);

    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource;
    private String authHeader;

    public DockerProviderMgr(AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource,
            String authHeader) {
        this.authenticationDetailsSource = authenticationDetailsSource;
        this.authHeader = authHeader;
    }

    @Override
    public OauthModel fetchTokenFromProvider() {
        LoginHandler loginHandler = ContextHelper.get().beanForType(LoginHandler.class);
        // if basic auth no present return with unauthorized
        String[] tokens = new String[0];
        try {
            tokens = loginHandler.extractAndDecodeHeader(authHeader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String username = tokens[0];
        OauthModel json = null;
        if (authHeader.startsWith("Basic ")) {
            try {
                return loginHandler.doBasicAuthWithDb(tokens, authenticationDetailsSource);
            } catch (Exception e) {
                json = new OauthErrorModel(HttpServletResponse.SC_UNAUTHORIZED,
                        OauthErrorEnum.BAD_CREDENTIAL);
                log.debug("failed to authenticate with basic authentication");
            }
            // if auth integration isn't enable return
            if (!isOauthSettingEnable()) {
                log.debug("Artifactory basic authentication failed ,oauth integration isn't enable");
                return json;
            }
            return loginHandler.doBasicAuthWithProvider(authHeader, username);
        } else {
            if (authHeader.startsWith(OauthManager.OAUTH_TOKEN_PREFIX)) {
                try {
                    return loginHandler.doPropsAuthWithDb(username);
                } catch (ParseException e) {
                    json = new OauthErrorModel(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            OauthErrorEnum.INTERNAL_SERVER_ERROR);
                    log.error(e.toString());
                }
            }
        }
        return json;
    }


    /**
     * check weather oauth integration is enabled
     *
     * @return true if auth integration is enabled
     */
    private boolean isOauthSettingEnable() {
        CentralConfigDescriptor descriptor = ContextHelper.get().getCentralConfig().getDescriptor();
        SecurityDescriptor security = descriptor.getSecurity();
        if (security != null) {
            OAuthSettings oauthSettings = security.getOauthSettings();
            if (oauthSettings != null && oauthSettings.getEnableIntegration()) {
                return true;
            }
        }
        return false;
    }
}
