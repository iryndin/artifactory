package org.artifactory.rest.common.model.reverseproxy;

import org.artifactory.descriptor.repo.ReverseProxyRepoConfig;

/**
 * @author Chen Keinan
 */
public class ReverseProxyRepoConfigs {
    private String repoRef;
    private String serverName;
    private int port;

    public ReverseProxyRepoConfigs() {
        // for jackson
    }

        public ReverseProxyRepoConfigs(ReverseProxyRepoConfig reverseProxyRepoConfig){
        this.repoRef = reverseProxyRepoConfig.getRepoRef().getKey();
        this.serverName = reverseProxyRepoConfig.getServerName();
        this.port = reverseProxyRepoConfig.getPort();
    }

    public String getRepoRef() {
        return repoRef;
    }

    public void setRepoRef(String repoRef) {
        this.repoRef = repoRef;
    }

    public String getServerName() {

        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
