package org.artifactory.rest.common.service.admin.reverseProxies;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyRepoConfig;
import org.artifactory.rest.common.model.proxies.ProxiesModel;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Chen  Keinan
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckReverseProxyPortAvailabilityService implements RestService {

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        int portToCheck = Integer.parseInt(request.getPathParamByKey("port"));
        String repoKey = request.getQueryParamByKey("repoKey");

        boolean isPortInUse = false;
        MutableCentralConfigDescriptor mutableDescriptor = ContextHelper.get().getCentralConfig().getMutableDescriptor();
        ReverseProxyDescriptor reverseProxyDescriptor = mutableDescriptor.getCurrentReverseProxy();
        if (reverseProxyDescriptor != null) {
            List<ReverseProxyRepoConfig> reverseProxyRepoConfigs = reverseProxyDescriptor.getReverseProxyRepoConfigs();
            for (ReverseProxyRepoConfig reverseProxyRepoConfig : reverseProxyRepoConfigs) {
                if (reverseProxyRepoConfig != null && (reverseProxyRepoConfig.getPort() == portToCheck)) {
                    if (!reverseProxyRepoConfig.getRepoRef().getKey().equals(repoKey)) {
                        isPortInUse = true;
                        break;
                    }
                }
            }
        }
        ProxiesModel proxiesModel = new ProxiesModel();
        proxiesModel.setPortAvailable(!isPortInUse);
        response.iModel(proxiesModel);
    }
}
