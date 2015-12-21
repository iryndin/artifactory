package org.artifactory.ui.rest.service.admin.security.oauth;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthProviderSettings;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthProviderUIModel;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUIProvidersTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.artifactory.ui.rest.model.admin.security.oauth.OAuthUIProvidersTypeEnum.valueOf;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateOAuthProviderSettings implements RestService<OAuthProviderUIModel> {
    private static final Logger log = LoggerFactory.getLogger(UpdateOAuthProviderSettings.class);
    @Autowired
    CentralConfigService centralConfigService;
    @Autowired
    OAuthHandler oAuthHandler;


    @Override
    public void execute(ArtifactoryRestRequest<OAuthProviderUIModel> request, RestResponse response) {
        OAuthProviderUIModel imodel = request.getImodel();
        String providerName = imodel.getName();
        if(StringUtils.isBlank(providerName)){
            response.error("Missing provider name");
            return;
        }
        log.debug("Updating OAuth provider '{}'",providerName);
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        OAuthSettings oauthSettings = mutableDescriptor.getSecurity().getOauthSettings();
        if (oauthSettings != null) {
            OAuthProviderSettings providerToUpdate = getProviderToUpdate(providerName, oauthSettings);
            if(providerToUpdate ==null){
                response.error("Couldn't update provider, not exist.");
            }else {
                providerToUpdate.setId(imodel.getId());
                providerToUpdate.setSecret(imodel.getSecret());
                providerToUpdate.setApiUrl(imodel.getApiUrl());
                providerToUpdate.setBasicUrl(imodel.getBasicUrl());
                providerToUpdate.setAuthUrl(imodel.getAuthUrl());
                providerToUpdate.setTokenUrl(imodel.getTokenUrl());
                OAuthUIProvidersTypeEnum uiProviderType = valueOf(imodel.getProviderType());
                providerToUpdate.setProviderType(uiProviderType.getProviderType().name());
                providerToUpdate.setProviderType(imodel.getProviderType());
                providerToUpdate.setDomain(imodel.getDomain());
                providerToUpdate.setEnabled(imodel.isEnabled());
                providerToUpdate.setName(imodel.getName());
                centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
                response.info("Successfully update OAuth provider :"+providerName);
                log.debug("Successfully update OAuth provider '{}'",providerName);
            }
        } else {
            response.error("Couldn't add OAuth provider, OAuth settings doesn't exist");
        }
    }

    private OAuthProviderSettings getProviderToUpdate(String providerName,
            OAuthSettings oauthSettings) {
        Optional<OAuthProviderSettings> first = oauthSettings.getOauthProvidersSettings().stream().filter(
                e -> e.getName().equals(providerName)).findFirst();
        return first.isPresent()?first.get():null;
    }
}
