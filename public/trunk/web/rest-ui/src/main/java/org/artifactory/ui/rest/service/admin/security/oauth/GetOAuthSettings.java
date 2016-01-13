package org.artifactory.ui.rest.service.admin.security.oauth;

import com.google.common.collect.Lists;
import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.addon.oauth.OAuthProvidersTypeEnum;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthProviderSettings;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthProviderInfo;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthProviderUIModel;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUIModel;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUIProvidersTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.artifactory.addon.oauth.OAuthProvidersTypeEnum.valueOf;

/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetOAuthSettings implements RestService {
    private static final Logger log = LoggerFactory.getLogger(GetOAuthSettings.class);
    @Autowired
    CentralConfigService centralConfigService;
    @Autowired
    OAuthHandler oAuthHandler;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        log.debug("Retrieving OAuth settings ");
        OAuthSettings oauthSettings = centralConfigService.getDescriptor().getSecurity().getOauthSettings();
        OAuthUIModel model;
        if(oauthSettings!=null){
             model=createUIModel(oauthSettings);
        }else{
            OAuthSettings oAuthSettings = new OAuthSettings();
            MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
            mutableDescriptor.getSecurity().setOauthSettings(oAuthSettings);
            centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
            model=createUIModel(oAuthSettings);
        }
        response.iModel(model);
    }

    private OAuthUIModel createUIModel(OAuthSettings oauthSettings) {
        OAuthUIModel oAuthUIModel = new OAuthUIModel();
        oAuthUIModel.setEnabled(oauthSettings.getEnableIntegration());
        oAuthUIModel.setProviders(getProvidersFromSettings(oauthSettings));
        oAuthUIModel.setAvailableTypes(getAvailableProviders());
        oAuthUIModel.setDefaultNpm(oauthSettings.getDefaultNpm());
        oAuthUIModel.setPersistUsers(oauthSettings.getPersistUsers());
        oAuthUIModel.setAllowUserToAccessProfile(oauthSettings.isAllowUserToAccessProfile());
        return oAuthUIModel;
    }

    private List<OAuthProviderUIModel> getProvidersFromSettings(OAuthSettings oauthSettings) {
        return oauthSettings.getOauthProvidersSettings().
                    stream().filter(Objects::nonNull).map(this::toModel).collect(Collectors.toList());
    }

    private List<OAuthProviderInfo> getAvailableProviders() {
        ArrayList<OAuthUIProvidersTypeEnum> es = Lists.newArrayList(OAuthUIProvidersTypeEnum.values());
        return es.stream().filter(Objects::nonNull).map(OAuthUIProvidersTypeEnum::getProviderInfo)
                .collect(Collectors.toList());
    }

    private OAuthProviderUIModel toModel(OAuthProviderSettings oAuthProviderSettings) {
        OAuthProviderUIModel oAuthProviderUIModel = new OAuthProviderUIModel();
        oAuthProviderUIModel.setId(oAuthProviderSettings.getId());
        oAuthProviderUIModel.setSecret(oAuthProviderSettings.getSecret());
        oAuthProviderUIModel.setApiUrl(oAuthProviderSettings.getApiUrl());
        oAuthProviderUIModel.setBasicUrl(oAuthProviderSettings.getBasicUrl());
        oAuthProviderUIModel.setAuthUrl(oAuthProviderSettings.getAuthUrl());
        oAuthProviderUIModel.setTokenUrl(oAuthProviderSettings.getTokenUrl());
        oAuthProviderUIModel.setEnabled(oAuthProviderSettings.getEnabled());
        oAuthProviderUIModel.setDomain(oAuthProviderSettings.getDomain());
        OAuthProvidersTypeEnum providerType = valueOf(oAuthProviderSettings.getProviderType());
        OAuthUIProvidersTypeEnum uiProviderType=OAuthUIProvidersTypeEnum.fromProviderType(providerType);
        oAuthProviderUIModel.setProviderType(uiProviderType.name());
        oAuthProviderUIModel.setName(oAuthProviderSettings.getName());
        return oAuthProviderUIModel;
    }
}