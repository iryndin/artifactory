package org.artifactory.rest.common.service.admin.reverseProxies;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyRepoConfig;
import org.artifactory.descriptor.repo.WebServerType;
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

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class GetReverseProxiesService implements RestService {

    @Autowired
    private CentralConfigService centralConfigService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AolUtils.assertNotAol("GetReverseProxies");
        fetchSingleOrMultiReverseProxy(response, request);
    }

    private void fetchSingleOrMultiReverseProxy(RestResponse artifactoryResponse,
            ArtifactoryRestRequest artifactoryRequest) {
        String proxyKey = artifactoryRequest.getPathParamByKey("id");
        if (StringUtils.isEmpty(proxyKey)) {
            proxyKey = "dummy";
        }
        if (StringUtils.isEmpty(proxyKey)) {
            buildBadRequestResponse(artifactoryResponse);
            return;
        }
        if (!(proxyKey.equals(WebServerType.NGINX.toString()) ||
                proxyKey.equals(WebServerType.APACHE.toString()) || proxyKey.equals("dummy"))) {
            artifactoryResponse.responseCode(HttpServletResponse.SC_NOT_FOUND);
            artifactoryResponse.error("Reverse proxy id is missing");
            return;
        }
        if (isMultiProxy(proxyKey)) {
            updateResponseWithMultiProxyInfo(artifactoryResponse);
        } else {
            updateResponseWithSingleProxyInfo(artifactoryResponse, proxyKey);
        }
    }

    /**
     * build bad request response
     *
     * @param artifactoryResponse - artifactory response
     */
    private void buildBadRequestResponse(RestResponse artifactoryResponse) {
        artifactoryResponse.responseCode(HttpServletResponse.SC_BAD_REQUEST);
        artifactoryResponse.error("Reverse proxy id is missing");
    }

    /**
     * build response with multi proxy info
     *
     * @param artifactoryResponse
     */
    private void updateResponseWithMultiProxyInfo(RestResponse artifactoryResponse) {
        List<ReverseProxyDescriptor> reverseProxies = centralConfigService.getMutableDescriptor().getReverseProxies();
        artifactoryResponse.iModelList(reverseProxies);
    }

    /**
     * update response with single proxy response
     *
     * @param artifactoryResponse - artifactory response
     * @param proxyKey
     */
    private void updateResponseWithSingleProxyInfo(RestResponse artifactoryResponse, String proxyKey) {
        ReverseProxyDescriptor reverseProxy = centralConfigService.getMutableDescriptor().getCurrentReverseProxy();
        if (reverseProxy != null) {
            if (!((reverseProxy.getKey().equals(proxyKey)) || (proxyKey.equals("dummy")))) {
                artifactoryResponse.responseCode(HttpServletResponse.SC_NOT_FOUND);
                artifactoryResponse.error("Reverse proxy with id:" + proxyKey + " not found");
                return;
            }
        }
        ReverseProxyDescriptorModel reverseProxyDescriptorModel = descriptorToModel(reverseProxy);
        artifactoryResponse.iModel(reverseProxyDescriptorModel);
    }

    /**
     * check if multi proxy
     *
     * @param proxyKey - proxy key
     * @return if true - return multi proxy
     */
    private boolean isMultiProxy(String proxyKey) {
        return proxyKey == null || proxyKey.length() == 0;
    }


    /**
     * convert descriptor to ui model
     * @param descriptor - reverse proxy descriptor
     * @return - reverse proxy model
     */
    private ReverseProxyDescriptorModel descriptorToModel(ReverseProxyDescriptor descriptor){
        if (descriptor == null){
            return new ReverseProxyDescriptorModel();
        }

        List<ReverseProxyRepoConfig> reverseProxyRepoConfigs = descriptor.getReverseProxyRepoConfigs();
        ReverseProxyRepositories reverseProxyRepositories = new ReverseProxyRepositories();
         final List<ReverseProxyRepoConfigs> finalReverseProxyRepositoriesList = new ArrayList<>();
        reverseProxyRepoConfigs.forEach(reverseProxyRepoConfig -> {
            finalReverseProxyRepositoriesList.add(new ReverseProxyRepoConfigs(reverseProxyRepoConfig));
         });
        ReverseProxyDescriptorModel reverseProxyDescriptorModel = new ReverseProxyDescriptorModel();
        reverseProxyDescriptorModel.setKey(descriptor.getKey());
        reverseProxyDescriptorModel.setArtifactoryPort(descriptor.getArtifactoryPort());
        reverseProxyDescriptorModel.setArtifactoryServerName(descriptor.getArtifactoryServerName());
        reverseProxyDescriptorModel.setDockerReverseProxyMethod(descriptor.getDockerReverseProxyMethod());
        reverseProxyDescriptorModel.setArtifactoryAppContext(descriptor.getArtifactoryAppContext());
        reverseProxyDescriptorModel.setHttpPort(descriptor.getHttpPort());
        reverseProxyDescriptorModel.setSslPort(descriptor.getSslPort());
        reverseProxyDescriptorModel.setPublicAppContext(descriptor.getPublicAppContext());
        reverseProxyDescriptorModel.setServerNameExpression(descriptor.getServerNameExpression());
        reverseProxyDescriptorModel.setUpStreamName(descriptor.getUpStreamName());
        reverseProxyDescriptorModel.setUseHttp(descriptor.isUseHttp());
        reverseProxyDescriptorModel.setUseHttps(descriptor.isUseHttps());
        reverseProxyDescriptorModel.setWebServerType(descriptor.getWebServerType());
        reverseProxyDescriptorModel.setSslKey(descriptor.getSslKey());
        reverseProxyDescriptorModel.setSslCertificate(descriptor.getSslCertificate());
        reverseProxyDescriptorModel.setServerName(descriptor.getServerName());
        if (!finalReverseProxyRepositoriesList.isEmpty()){
            reverseProxyRepositories.setReverseProxyRepoConfigs(finalReverseProxyRepositoriesList);
            reverseProxyDescriptorModel.setReverseProxyRepositories(reverseProxyRepositories);
        }
        return reverseProxyDescriptorModel;
    }
}
