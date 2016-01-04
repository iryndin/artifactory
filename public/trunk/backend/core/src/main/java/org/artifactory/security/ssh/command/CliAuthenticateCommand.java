package org.artifactory.security.ssh.command;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.md.Properties;
import org.artifactory.security.props.auth.SshTokenManager;
import org.artifactory.security.props.auth.TokenManager;
import org.artifactory.security.ssh.UsernameAttributeKey;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.util.PathUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chen Keinan
 */
public class CliAuthenticateCommand extends AbstractAuthenticateCommand {

    public static final String COMMAND_NAME = "jfrog-authenticate";

    public CliAuthenticateCommand(CentralConfigService centralConfigService,
                                  UserGroupStoreService userGroupStoreService,
                                  String command, TokenManager tokenManager) {
        super(centralConfigService, userGroupStoreService, command, tokenManager);
    }

    @Override
    protected void parseCommandDetails(String command) {
        // do nothing
    }

    protected void sendAuthHeader() throws IOException {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> header = new HashMap<>();
        // update header href
        updateHeaderHref(response);
        // update header authorization
        updateHeaderAuthorization(header);
        // write response
        writeResponse(response, header);
    }

    /**
     * update header basic authentication
     *
     * @param header - header object
     */
    private void updateHeaderAuthorization(Map<String, Object> header) {
        String username = serverSession.getAttribute(new UsernameAttributeKey());
        Properties propertiesForUser = userGroupStoreService.findPropertiesForUser(username);
        String token = propertiesForUser.getFirst(SshTokenManager.SSH_KEY);
        if (StringUtils.isBlank(token)) {
            token = generateUniqueToken(username);
        }
        header.put(SshTokenManager.AUTHORIZATION_HEADER, SshTokenManager.OAUTH_TOKEN_PREFIX + token);
    }

    protected void updateResponseHeader(Map<String, Object> response, Map<String, Object> header) {
        response.put("headers", header);
    }

    /**
     * update header href
     *
     * @param response - response map
     */
    private void updateHeaderHref(Map<String, Object> response) {
        String urlBase = getBaseUrl();
        response.put(HREF, PathUtils.addTrailingSlash(urlBase));
    }
}
