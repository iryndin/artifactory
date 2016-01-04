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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AddOAuthProviderSettings implements RestService<OAuthProviderUIModel> {
    private static final Logger log = LoggerFactory.getLogger(AddOAuthProviderSettings.class);

    @Autowired
    CentralConfigService centralConfigService;
    @Autowired
    OAuthHandler oAuthHandler;


    @Override
    public void execute(ArtifactoryRestRequest<OAuthProviderUIModel> request, RestResponse response) {
        OAuthProviderUIModel imodel = request.getImodel();
        OAuthProviderSettings oAuthProviderSettings = new OAuthProviderSettings();
        oAuthProviderSettings.setEnabled(imodel.isEnabled());
        oAuthProviderSettings.setName(imodel.getName());
        oAuthProviderSettings.setProviderType(imodel.getProviderType());
        oAuthProviderSettings.setId(imodel.getId());
        oAuthProviderSettings.setSecret(imodel.getSecret());
        oAuthProviderSettings.setApiUrl(imodel.getApiUrl());
        oAuthProviderSettings.setBasicUrl(imodel.getBasicUrl());
        oAuthProviderSettings.setAuthUrl(imodel.getAuthUrl());
        oAuthProviderSettings.setDomain(imodel.getDomain());
        oAuthProviderSettings.setTokenUrl(imodel.getTokenUrl());
        if(StringUtils.isBlank(oAuthProviderSettings.getName())){
            response.error("Missing provider name");
            return;
        }
        log.debug("Adding OAuth provider '{}'",imodel.getName());
        if(StringUtils.isBlank(oAuthProviderSettings.getProviderType())){
            response.error("Missing provider name");
            return;
        }
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        OAuthSettings oauthSettings = mutableDescriptor.getSecurity().getOauthSettings();
        if (oauthSettings != null) {
            if(isProviderExist(oAuthProviderSettings, oauthSettings)){
                response.error("Couldn't add provider, already exists.");
                return;
            }
            oauthSettings.getOauthProvidersSettings().add(oAuthProviderSettings);
            centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            response.info("Successfully add OAuth provider :" + imodel.getName());
            log.debug("Successfully added OAuth provider '{}'", imodel.getName());
        } else {
            response.error("Couldn't add OAuth provider, OAuth settings doesn't exist");
        }
    }

    private boolean isProviderExist(OAuthProviderSettings oAuthProviderUIModel, OAuthSettings oauthSettings) {
        return oauthSettings.getOauthProvidersSettings().stream().anyMatch(
                e -> e.getName().equals(oAuthProviderUIModel.getName()));
    }
}