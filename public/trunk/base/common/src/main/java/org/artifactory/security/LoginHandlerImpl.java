package org.artifactory.security;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.md.Properties;
import org.artifactory.security.props.auth.OauthManager;
import org.artifactory.security.props.auth.model.*;
import org.artifactory.util.dateUtils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

/**
 * @author Chen  Keinan
 */
@Component
public class LoginHandlerImpl implements LoginHandler {

    public final static String TOKEN_EXPIRES_IN = "3600";

    @Autowired
    UserGroupService userGroupService;

    @Autowired
    OauthManager oauthManager;


    @Override
    public OauthModel doBasicAuthWithDb(String[] tokens,
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) throws IOException, ParseException {
        assert tokens.length == 2;
        AuthenticationManager authenticationManager = ContextHelper.get().beanForType(AuthenticationManager.class);
        String username = tokens[0];
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, tokens[1]);
        authRequest.setDetails(authenticationDetailsSource);
        Authentication authenticate = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        Properties propertiesForUser = userGroupService.findPropertiesForUser(username);
        String token = propertiesForUser.getFirst(OauthManager.OAUTH_KEY);
        if (StringUtils.isBlank(token)) {
            TokenKeyValue tokenKeyValue = oauthManager.createToken(username);
            token = tokenKeyValue.getToken();
        }
        String createdAt = DateUtils.formatBuildDate(System.currentTimeMillis());
        OauthModel oauthModel = new AuthenticationModel(token, createdAt, TOKEN_EXPIRES_IN);
        return oauthModel;
    }

    @Override
    public OauthModel doBasicAuthWithProvider(String header, String username) {
        OAuthHandler oAuthHandler = ContextHelper.get().beanForType(OAuthHandler.class);
        CentralConfigDescriptor descriptor = ContextHelper.get().getCentralConfig().getDescriptor();
        OAuthSettings oauthSettings = descriptor.getSecurity().getOauthSettings();
        String defaultProvider = oauthSettings.getDefaultNpm();
        // try to get token from provider
        OauthModel oauthModel = oAuthHandler.getCreateToken(defaultProvider, username, header);
        return oauthModel;
    }

    @Override
    public OauthModel doPropsAuthWithDb(String username) throws ParseException {
        Properties propertiesForUser = userGroupService.findPropertiesForUser(username);
        String token = propertiesForUser.getFirst(OauthManager.OAUTH_KEY);
        if (StringUtils.isBlank(token)) {
            String createdAt = DateUtils.formatBuildDate(System.currentTimeMillis());
            AuthenticationModel authenticationModel = new AuthenticationModel(token, createdAt, "3600");
            return authenticationModel;
        } else {
            return new OauthErrorModel(HttpServletResponse.SC_NOT_FOUND, OauthErrorEnum.USER_NOT_FOUND);
        }
    }

    @Override
    public String[] extractAndDecodeHeader(String header) throws IOException {

        byte[] base64Token = header.substring(6).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = org.springframework.security.crypto.codec.Base64.decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
        String token = new String(decoded, "UTF-8");

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[]{token.substring(0, delim), token.substring(delim + 1)};
    }
}
