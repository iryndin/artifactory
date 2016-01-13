package org.artifactory.rest.common.service.admin.reverseProxies;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.util.AolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateReverseProxyService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AolUtils.assertNotAol("UpdateReverseProxy");
        String proxyKey = request.getPathParamByKey("id");
        ReverseProxyDescriptor descriptor = (ReverseProxyDescriptor) request.getImodel();
        updateReverseProxy(descriptor, proxyKey);
        response.info("Successfully updated reverse proxy '" + proxyKey + "'");
    }

    /**
     * update reverse proxy descriptor˜
     *
     * @param newReverseProxy - new reverse proxy
     * @param proxyKey        - reverse proxy key
     */
    private void updateReverseProxy(ReverseProxyDescriptor newReverseProxy, String proxyKey) {
        MutableCentralConfigDescriptor configDescriptor = centralConfigService.getMutableDescriptor();
        ReverseProxyDescriptor currentReverseProxy = configDescriptor.getReverseProxy(proxyKey);
        if (currentReverseProxy != null) {
            populateReverseProxyData(currentReverseProxy, newReverseProxy);
            centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
        }
    }

    /**
     * map data from new reverse proxy to current reverse proxy
     * @param currentReverseProxy - current reverse proxy descriptor
     * @param newReverseProxy - new reverse proxy
     */
    private void populateReverseProxyData(ReverseProxyDescriptor currentReverseProxy, ReverseProxyDescriptor newReverseProxy) {
        currentReverseProxy.setWebServerType(newReverseProxy.getWebServerType());
        currentReverseProxy.setArtifactoryAppContext(newReverseProxy.getArtifactoryAppContext());
        currentReverseProxy.setPublicAppContext(newReverseProxy.getPublicAppContext());
        currentReverseProxy.setServerName(newReverseProxy.getServerName());
        currentReverseProxy.setServerNameExpression(newReverseProxy.getServerNameExpression());
        currentReverseProxy.setDockerReverseProxyMethod(newReverseProxy.getDockerReverseProxyMethod());
        currentReverseProxy.setSslCertificate(newReverseProxy.getSslCertificate());
        currentReverseProxy.setSslKey(newReverseProxy.getSslKey());
        currentReverseProxy.setUseHttps(newReverseProxy.isUseHttps());
        currentReverseProxy.setSslPort(newReverseProxy.getSslPort());
        currentReverseProxy.setArtifactoryServerName(newReverseProxy.getArtifactoryServerName());
        currentReverseProxy.setArtifactoryPort(newReverseProxy.getArtifactoryPort());
        currentReverseProxy.setHttpPort(newReverseProxy.getHttpPort());
    }
}
