package org.artifactory.ui.rest.service.admin.security.oauth;

import com.google.common.collect.Lists;
import org.artifactory.addon.oauth.OAuthHandler;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.security.oauth.OAuthProviderSettings;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthProviderUIModel;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUIModel;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUIProvidersTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author Gidi Shabat
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateOrCreateOAuthSettings implements RestService<OAuthUIModel> {
    private static final Logger log = LoggerFactory.getLogger(UpdateOrCreateOAuthSettings.class);
    @Autowired
    CentralConfigService centralConfigService;
    @Autowired
    OAuthHandler oAuthHandler;

    @Override
    public void execute(ArtifactoryRestRequest<OAuthUIModel> request, RestResponse response) {
        log.debug("Updating OAuth settings");
        OAuthUIModel imodel = request.getImodel();
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        OAuthSettings oauthSettings = mutableDescriptor.getSecurity().getOauthSettings();
        if(oauthSettings!=null){
            oauthSettings.setEnableIntegration(imodel.isEnabled());
            oauthSettings.setDefaultNpm(imodel.getDefaultNpm());
            oauthSettings.setPersistUsers(imodel.isPersistUsers());
            oauthSettings.setAllowUserToAccessProfile(imodel.isAllowUserToAccessProfile());
        }else{
            oauthSettings=new OAuthSettings();
            oauthSettings.setEnableIntegration(imodel.isEnabled());
            oauthSettings.setDefaultNpm(imodel.getDefaultNpm());
            oauthSettings.setPersistUsers(imodel.isPersistUsers());
            oauthSettings.setOauthProvidersSettings(getProviders(imodel));
            mutableDescriptor.getSecurity().setOauthSettings(oauthSettings);
            oauthSettings.setAllowUserToAccessProfile(imodel.isAllowUserToAccessProfile());
        }
        centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
        response.info("Successfully update OAuth settings");
        log.debug("Successfully Updated OAuth settings");

    }

    private List<OAuthProviderSettings> getProviders(OAuthUIModel imodel) {
        List<OAuthProviderSettings> result= Lists.newArrayList();
        List<OAuthProviderUIModel> providers = imodel.getProviders();
        if(providers!=null){
            for (OAuthProviderUIModel uiProvider : providers) {
                OAuthProviderSettings oAuthProviderModel = new OAuthProviderSettings();
                oAuthProviderModel.setName(uiProvider.getName());
                oAuthProviderModel.setEnabled(uiProvider.isEnabled());
                oAuthProviderModel.setId(uiProvider.getId());
                oAuthProviderModel.setSecret(uiProvider.getSecret());
                oAuthProviderModel.setApiUrl(uiProvider.getApiUrl());
                oAuthProviderModel.setAuthUrl(uiProvider.getAuthUrl());
                oAuthProviderModel.setTokenUrl(uiProvider.getTokenUrl());
                oAuthProviderModel.setBasicUrl(uiProvider.getBasicUrl());
                oAuthProviderModel.setDomain(uiProvider.getDomain());
                OAuthUIProvidersTypeEnum uiProviderType = OAuthUIProvidersTypeEnum.valueOf(uiProvider.getProviderType());
                oAuthProviderModel.setProviderType(uiProviderType.getProviderType().name());
                oAuthProviderModel.setProviderType(uiProvider.getProviderType());
                result.add(oAuthProviderModel);
            }
        }
        return result;
    }
}
