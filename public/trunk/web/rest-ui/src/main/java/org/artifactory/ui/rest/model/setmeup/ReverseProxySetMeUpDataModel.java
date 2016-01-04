package org.artifactory.ui.rest.model.setmeup;

import org.artifactory.rest.common.model.BaseModel;
import org.artifactory.ui.rest.model.utils.repositories.RepoKeyType;

import java.util.List;

/**
 * @author Chen Keinan
 */
public class ReverseProxySetMeUpDataModel extends BaseModel {

    private boolean usingHttps;
    private boolean methodSelected;
    private boolean usingPorts;
    private String  serverName;
    private Integer repoPort;

    public boolean isUsingPorts() {
        return usingPorts;
    }

    public void setUsingPorts(boolean usingPorts) {
        this.usingPorts = usingPorts;
    }

    public boolean isUsingHttps() {
        return usingHttps;
    }

    public void setUsingHttps(boolean usingHttps) {
        this.usingHttps = usingHttps;
    }

    public boolean isMethodSelected() {
        return methodSelected;
    }

    public void setMethodSelected(boolean methodSelected) {
        this.methodSelected = methodSelected;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Integer getRepoPort() {
        return repoPort;
    }

    public void setRepoPort(Integer repoPort) {
        this.repoPort = repoPort;
    }

}
