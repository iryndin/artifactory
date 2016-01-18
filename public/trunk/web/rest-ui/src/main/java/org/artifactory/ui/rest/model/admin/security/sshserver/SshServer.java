package org.artifactory.ui.rest.model.admin.security.sshserver;

import org.artifactory.descriptor.security.sshserver.SshServerSettings;
import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;
import org.artifactory.ui.rest.model.admin.security.signingkey.SignKey;

/**
 * @author Noam Y. Tenne
 */
public class SshServer extends SshServerSettings implements RestModel {

    private SignKey serverKey;

    private String customUrlBase;

    public SshServer() {
    }

    public SshServer(SshServerSettings sshServerSettings) {
        if (sshServerSettings != null) {
            super.setEnableSshServer(sshServerSettings.isEnableSshServer());
            super.setSshServerPort(sshServerSettings.getSshServerPort());
        }
    }

    public SignKey getServerKey() {
        return serverKey;
    }

    public void setServerKey(SignKey serverKey) {
        this.serverKey = serverKey;
    }

    public String getCustomUrlBase() { return customUrlBase; }

    public void setCustomUrlBase(String customUrlBase) { this.customUrlBase = customUrlBase; }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
