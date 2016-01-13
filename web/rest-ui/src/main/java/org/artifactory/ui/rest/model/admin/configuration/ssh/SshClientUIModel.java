package org.artifactory.ui.rest.model.admin.configuration.ssh;

import org.artifactory.rest.common.model.RestModel;
import org.artifactory.rest.common.util.JsonUtil;

/**
 * @author Noam Y. Tenne
 */
public class SshClientUIModel implements RestModel {

    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public SshClientUIModel() {
    }

    @Override
    public String toString() {
        return JsonUtil.jsonToString(this);
    }
}
