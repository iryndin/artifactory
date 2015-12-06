package org.artifactory.ui.rest.model.admin.configuration.reverseProxy;

import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Shay Yaakov
 */
public class ReverseProxyRepoModel extends BaseModel {

    private String configurationKey;
    private String serverName;
    private Integer serverPort;

    public String getConfigurationKey() {
        return configurationKey;
    }

    public void setConfigurationKey(String configurationKey) {
        this.configurationKey = configurationKey;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        if (serverPort != null && serverPort > 0) {
            this.serverPort = serverPort;
        }
    }
}
