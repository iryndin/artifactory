package org.artifactory.rest.common.service.admin.reverseProxies;

import com.google.common.collect.Sets;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.rest.common.model.reverseproxy.ReverseProxyDescriptorModel;
import org.artifactory.rest.common.model.reverseproxy.ReverseProxyRepoConfigs;
import org.artifactory.rest.common.model.reverseproxy.ReverseProxyRepositories;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.util.AolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateReverseProxyService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AolUtils.assertNotAol("CreateReverseProxy");
        ReverseProxyDescriptorModel descriptor = (ReverseProxyDescriptorModel) request.getImodel();
        ReverseProxyDescriptor reverseProxyDescriptor = modelToDescriptor(descriptor);
        if (reverseProxyDescriptor == null){
            return;
        }
        addNewReverseProxy(reverseProxyDescriptor);
        updateResponse(response, reverseProxyDescriptor);
    }

    /**
     * add new reverse proxy descriptor
     * @param descriptor - new reverse proxy descriptor to add
     */
    private void addNewReverseProxy(ReverseProxyDescriptor descriptor) {
        MutableCentralConfigDescriptor configDescriptor = centralConfigService.getMutableDescriptor();
        configDescriptor.updateReverseProxy(descriptor);
        centralConfigService.saveEditedDescriptorAndReload(configDescriptor);
    }


    /**
     * update response
     * @param artifactoryResponse - reverse proxy response
     * @param descriptor - descriptor
     */
    private void updateResponse(RestResponse artifactoryResponse, ReverseProxyDescriptor descriptor) {
        artifactoryResponse.info("Successfully update reverse proxy '" + descriptor.getKey() + "'");
        artifactoryResponse.responseCode(HttpServletResponse.SC_CREATED);
    }

    /**
     * map reverse proxy model to descriptor
     * @param reverseProxyDescriptorModel - reverse proxy model
     * @return reverse proxy descriptor
     */
    private ReverseProxyDescriptor modelToDescriptor(ReverseProxyDescriptorModel reverseProxyDescriptorModel){
        MutableCentralConfigDescriptor configDescriptor = centralConfigService.getMutableDescriptor();
        ReverseProxyDescriptor reverseProxy = configDescriptor.getCurrentReverseProxy();
        if (reverseProxy == null) {
            reverseProxy = new ReverseProxyDescriptor();
            reverseProxy.setKey(reverseProxyDescriptorModel.getKey());
        }
        else {
            if (!(reverseProxy.getKey().equals(WebServerType.NGINX.toString()) || reverseProxy.getKey().equals(WebServerType.APACHE.toString()))) {
                return null;
            }

        }
        List<ReverseProxyRepoConfig> reverseProxyRepos = new ArrayList<>();
        ReverseProxyRepositories reverseProxyRepositories = reverseProxyDescriptorModel.getReverseProxyRepositories();
        if (reverseProxyRepositories != null) {
            List<ReverseProxyRepoConfigs> reverseProxyRepoConfigs = reverseProxyRepositories.getReverseProxyRepoConfigs();
            updateRepoReverseProxies(reverseProxyRepoConfigs, reverseProxyRepos);
        }
        reverseProxy.setKey(reverseProxyDescriptorModel.getKey());
        reverseProxy.setArtifactoryPort(reverseProxyDescriptorModel.getArtifactoryPort());
        reverseProxy.setArtifactoryServerName(reverseProxyDescriptorModel.getArtifactoryServerName());
        reverseProxy.setDockerReverseProxyMethod(reverseProxyDescriptorModel.getDockerReverseProxyMethod());
        reverseProxy.setArtifactoryAppContext(reverseProxyDescriptorModel.getArtifactoryAppContext());
        reverseProxy.setHttpPort(reverseProxyDescriptorModel.getHttpPort());
        reverseProxy.setSslPort(reverseProxyDescriptorModel.getSslPort());
        reverseProxy.setPublicAppContext(reverseProxyDescriptorModel.getPublicAppContext());
        reverseProxy.setServerNameExpression(reverseProxyDescriptorModel.getServerNameExpression());
        reverseProxy.setUpStreamName(reverseProxyDescriptorModel.getUpStreamName());
        reverseProxy.setUseHttp(reverseProxyDescriptorModel.isUseHttp());
        reverseProxy.setUseHttps(reverseProxyDescriptorModel.isUseHttps());
        reverseProxy.setWebServerType(reverseProxyDescriptorModel.getWebServerType());
        reverseProxy.setSslKey(reverseProxyDescriptorModel.getSslKey());
        reverseProxy.setSslCertificate(reverseProxyDescriptorModel.getSslCertificate());
        reverseProxy.setServerName(reverseProxyDescriptorModel.getServerName());
        if (!reverseProxyRepos.isEmpty()){
            reverseProxy.setReverseProxyRepoConfigs(reverseProxyRepos);
        }
        return reverseProxy;
    }

    /**
     * update repo reverse proxy
     * @param reverseProxyRepoConfigs - reverse proxy reppo config
     * @param reverseProxyRepos  - reverse proxy repo
     */
    private void updateRepoReverseProxies(List<ReverseProxyRepoConfigs> reverseProxyRepoConfigs, List<ReverseProxyRepoConfig> reverseProxyRepos) {
        reverseProxyRepoConfigs.forEach(reverseProxyRepo -> {
            ReverseProxyRepoConfig proxyRepoConfig = new ReverseProxyRepoConfig();
            getAllRepos().forEach(repoDescriptor -> {
                if (repoDescriptor.getKey().equals(reverseProxyRepo.getRepoRef())) {
                    proxyRepoConfig.setRepoRef(repoDescriptor);
                    proxyRepoConfig.setPort(reverseProxyRepo.getPort());
                    proxyRepoConfig.setServerName(reverseProxyRepo.getServerName());
                    reverseProxyRepos.add(proxyRepoConfig);
                }
            });
        });
    }

    /**
     * get all local , remote and virtual repositories
     * @return set of repositories
     */
    private Set<RepoBaseDescriptor> getAllRepos() {
        RepositoryService repositoryService = ContextHelper.get().getRepositoryService();
        Set<RepoBaseDescriptor> baseDescriptors = Sets.newTreeSet();
        List<LocalRepoDescriptor> localDescriptors = repositoryService.getLocalAndCachedRepoDescriptors();
        baseDescriptors.addAll(localDescriptors);
        // add remote repo
        List<RemoteRepoDescriptor> remoteDescriptors = repositoryService.getRemoteRepoDescriptors();
        baseDescriptors.addAll(remoteDescriptors);
        // add virtual repo
        List<VirtualRepoDescriptor> virtualDescriptors = repositoryService.getVirtualRepoDescriptors();
        baseDescriptors.addAll(virtualDescriptors);

        return baseDescriptors;
    }
}
