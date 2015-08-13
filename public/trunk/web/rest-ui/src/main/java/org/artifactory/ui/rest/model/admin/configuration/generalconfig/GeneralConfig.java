package org.artifactory.ui.rest.model.admin.configuration.generalconfig;

import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen Keinan
 */
public class GeneralConfig extends BaseModel {

    private String serverName;
    private String customUrlBase;
    private Integer fileUploadMaxSize;
    private String dateFormat;
    private Boolean globalOfflineMode;
    private Boolean showAddonSettings;
    private String logoUrl;
    private int bintrayFilesUploadLimit;

    public GeneralConfig(){}

    public GeneralConfig(MutableCentralConfigDescriptor mutableDescriptor) {
        serverName =  mutableDescriptor.getServerName();
        customUrlBase = mutableDescriptor.getUrlBase();
        fileUploadMaxSize = mutableDescriptor.getFileUploadMaxSizeMb();
        dateFormat = mutableDescriptor.getDateFormat();
        globalOfflineMode = mutableDescriptor.isOfflineMode();
        showAddonSettings = mutableDescriptor.getAddons().isShowAddonsInfo();
        logoUrl = mutableDescriptor.getLogo();
        bintrayFilesUploadLimit = getBintrayFileUploadLimit(mutableDescriptor);
    }

    private int getBintrayFileUploadLimit(MutableCentralConfigDescriptor mutableDescriptor) {
        if(mutableDescriptor.getBintrayConfig() != null){
            return mutableDescriptor.getBintrayConfig().getFileUploadLimit();
        }
        else {
            return 0;
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getCustomUrlBase() {
        return customUrlBase;
    }

    public void setCustomUrlBase(String customUrlBase) {
        this.customUrlBase = customUrlBase;
    }

    public Integer getFileUploadMaxSize() {
        return fileUploadMaxSize;
    }

    public void setFileUploadMaxSize(Integer fileUploadMaxSize) {
        this.fileUploadMaxSize = fileUploadMaxSize;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Boolean isGlobalOfflineMode() {
        return globalOfflineMode;
    }

    public void setGlobalOfflineMode(Boolean globalOfflineMode) {
        this.globalOfflineMode = globalOfflineMode;
    }

    public Boolean isShowAddonSettings() {
        return showAddonSettings;
    }

    public void setShowAddonSettings(Boolean showAddonSettings) {
        this.showAddonSettings = showAddonSettings;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public int getBintrayFilesUploadLimit() {
        return bintrayFilesUploadLimit;
    }

    public void setBintrayFilesUploadLimit(int bintrayFilesUploadLimit) {
        this.bintrayFilesUploadLimit = bintrayFilesUploadLimit;
    }
}