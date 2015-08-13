package org.artifactory.ui.rest.service.admin.configuration.general;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.bintray.BintrayConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.ui.rest.model.admin.configuration.generalconfig.GeneralConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Chen Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateGeneralConfigService implements RestService {

    @Autowired
    CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        GeneralConfig generalConfig = (GeneralConfig) request.getImodel();
        // update general setting and set config descriptor
        updateDescriptorAndSave(generalConfig);
        response.info("Successfully updated settings");
    }

    /**
     * update config descriptor with general config setting and save
     *
     * @param generalConfig - general setting sent from client
     */
    private void updateDescriptorAndSave(GeneralConfig generalConfig) {
        MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
        mutableDescriptor.setServerName(generalConfig.getServerName());
        mutableDescriptor.setDateFormat(generalConfig.getDateFormat());
        mutableDescriptor.setUrlBase(generalConfig.getCustomUrlBase());
        mutableDescriptor.setFileUploadMaxSizeMb(generalConfig.getFileUploadMaxSize());
        mutableDescriptor.setOfflineMode(generalConfig.isGlobalOfflineMode());
        mutableDescriptor.getAddons().setShowAddonsInfo(generalConfig.isShowAddonSettings());
        mutableDescriptor.setLogo(generalConfig.getLogoUrl());
        // update bintray config descriptor
        updateBintrayDescriptor(generalConfig, mutableDescriptor);
        centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
    }

    private void updateBintrayDescriptor(GeneralConfig generalConfig,
            MutableCentralConfigDescriptor mutableDescriptor) {
        BintrayConfigDescriptor bintrayMutableDescriptor = Optional.ofNullable(mutableDescriptor.getBintrayConfig())
                .orElse(new BintrayConfigDescriptor());
        bintrayMutableDescriptor.setFileUploadLimit(generalConfig.getBintrayFilesUploadLimit());
        mutableDescriptor.setBintrayConfig(bintrayMutableDescriptor);
    }
}
