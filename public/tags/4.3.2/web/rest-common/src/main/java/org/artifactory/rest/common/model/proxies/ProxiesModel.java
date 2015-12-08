package org.artifactory.rest.common.model.proxies;

import com.google.common.collect.Lists;
import org.artifactory.rest.common.model.BaseModel;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class ProxiesModel extends BaseModel {
    private List<String> proxyKeys = Lists.newArrayList();
    private Boolean portAvailable;

    public Boolean getPortAvailable() {
        return portAvailable;
    }

    public void setPortAvailable(Boolean portAvailable) {
        this.portAvailable = portAvailable;
    }

    public List<String> getProxyKeys() {
        return proxyKeys;
    }

    public void addProxy(String proxyKey) {
        proxyKeys.add(proxyKey);
    }

    public void setProxyKeys(List<String> proxyKeys) {
        this.proxyKeys = proxyKeys;
    }
}

