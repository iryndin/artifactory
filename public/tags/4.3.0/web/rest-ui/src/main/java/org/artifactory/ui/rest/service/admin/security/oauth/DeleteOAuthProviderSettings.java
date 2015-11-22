package org.artifactory.ui.rest.service.admin.security.oauth;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthProviderSettings;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteOAuthProviderSettings implements RestService<String> {
    private static final Logger log = LoggerFactory.getLogger(DeleteOAuthProviderSettings.class);
    @Autowired
    CentralConfigService centralConfigService;
    @Autowired
    OAuthHandler oAuthHandler;
    @Autowired
    UserGroupService userGroupService;

    @Override
    public void execute(ArtifactoryRestRequest<String> request, RestResponse response) {
        String providerName = request.getImodel();
        if(StringUtils.isBlank(providerName)){
            response.error("Couldn't delete provider, missing provider name");
            return;
        }
        log.debug("Deleting OAuth provider '{}'", providerName);
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        OAuthSettings oauthSettings = mutableDescriptor.getSecurity().getOauthSettings();
        if (oauthSettings != null) {
            // Remove provider to delete
            List<OAuthProviderSettings> providers = oauthSettings.getOauthProvidersSettings().stream().filter(
                    e -> !e.getName().equals(providerName)).collect(Collectors.toList());
            // Override providers
            oauthSettings.setOauthProvidersSettings(providers);
            centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            userGroupService.deletePropertyFromAllUsers("authinfo." + providerName);
            response.info("Successfully deleting OAuth provider :"+providerName);
            log.debug("Successfully deleting OAuth provider '{}'", providerName);
        } else {
            response.error("Couldn't delete OAuth provider, OAuth settings doesn't exist");
        }
    }

}