package org.artifactory.rest.common.service.admin.reverseProxies;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.ReverseProxyDescriptor;
import org.artifactory.descriptor.repo.ReverseProxyMethod;
import org.artifactory.repo.reverseProxy.ReverseProxyTemplateProvider;
import org.artifactory.rest.common.model.reverseproxy.CommonFile;
import org.artifactory.rest.common.service.ArtifactoryRestRequest;
import org.artifactory.rest.common.service.RestResponse;
import org.artifactory.rest.common.service.RestService;
import org.artifactory.rest.common.service.StreamRestResponse;
import org.artifactory.rest.common.util.AolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReverseProxySnippetService implements RestService {

    @Autowired
    ReverseProxyTemplateProvider templateProvider;

    @Autowired
    AuthorizationService authorizationService;

    @Override
    public void execute(ArtifactoryRestRequest request, RestResponse response) {
        AolUtils.assertNotAol("ReverseProxySnippet");
        if (authorizationService.isAnonymous()) {
            response.responseCode(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String reverseProxyKey = request.getPathParamByKey("id");
        if (StringUtils.isEmpty(reverseProxyKey) || reverseProxyKey.equals("dummy")){
            reverseProxyKey = "nginx";
        }
        boolean isDownload = Boolean.valueOf(request.getQueryParamByKey("download"));
        // get reverse proxy config descriptor
        CentralConfigService centralConfig = ContextHelper.get().getCentralConfig();
        ReverseProxyDescriptor reverseProxy = centralConfig.getMutableDescriptor().getReverseProxy(reverseProxyKey);
        if (reverseProxy == null) {
            response.error("No such reverse proxy config '" + reverseProxyKey + "'");
        } else {
            List<String> repoKeys = Lists.newArrayList();
            StringBuilder reverseProxySnippetbuilder = new StringBuilder();
            ReverseProxyMethod dockerReverseProxyMethod = reverseProxy.getDockerReverseProxyMethod();
            String dockerConfig = templateProvider.provideDockerReverseProxyServerSnippet(reverseProxyKey, repoKeys);
            // update general data
            updateGeneralData(reverseProxySnippetbuilder, dockerReverseProxyMethod, reverseProxyKey, repoKeys);
            // update docker data
            updateDockerData(reverseProxySnippetbuilder, dockerReverseProxyMethod, dockerConfig);
            // update response with snippet
            updateResponseWithSnippet(request, response, reverseProxySnippetbuilder, isDownload);
        }
    }

    /**
     * update response with generated reverse proxy snippet
     * @param request - encapsulate data related to request
     * @param response - encapsulate data related to response
     * @param reverseProxySnippetbuilder - reverse proxy string builder
     */
    private void updateResponseWithSnippet(ArtifactoryRestRequest request, RestResponse response,
                                           StringBuilder reverseProxySnippetbuilder,boolean isDownload) {
        boolean uiPath = request.getUriInfo().getPath().indexOf("/ui/") != -1;
        if (request.isUiRestCall() || uiPath || isDownload ) {
            if (isDownload){
                ((StreamRestResponse) response).setDownloadFile("artifactory.conf");
                ((StreamRestResponse) response).setDownload(true);
                CommonFile commonFile = new CommonFile(reverseProxySnippetbuilder.toString());
                (response).iModel(commonFile);
            }else {
                response.iModel(new DockerProxyTemplateInfo(reverseProxySnippetbuilder.toString()));
            }
        } else {
            response.iModel(reverseProxySnippetbuilder.toString());
        }
    }

    /**
     * update docker reverse proxy snippet
     * @param reverseProxySnippetbuilder - reverse proxy snippet builder
     * @param dockerReverseProxyMethod - docker reverse proxy method (port / sub domain)
     */
    private void updateDockerData(StringBuilder reverseProxySnippetbuilder, ReverseProxyMethod dockerReverseProxyMethod, String dockerConfig) {
        if (addDockerRelatedSnippet(dockerConfig, dockerReverseProxyMethod)) {
            reverseProxySnippetbuilder.append("\n");
            reverseProxySnippetbuilder.append(dockerConfig);
        }
    }

    /**
     * update reverse proxy general data
     * @param reverseProxySnippetbuilder - reverse proxy snippet builder
     * @param dockerReverseProxyMethod - docker reverse proxy method (port / sub domain)
     */
    private void updateGeneralData(StringBuilder reverseProxySnippetbuilder, ReverseProxyMethod dockerReverseProxyMethod
            , String reverseProxyKey, List<String> repoKeys) {
        String generalConfig = templateProvider.provideGeneralServerConfigServer(reverseProxyKey, repoKeys);
        if (generateGeneralReverseProxySnippet(dockerReverseProxyMethod)) {
            reverseProxySnippetbuilder.append(generalConfig);
        }
    }

    /**
     * add docker related snippet
     *
     * @param dockerConfig             - docker snippet setting
     * @param dockerReverseProxyMethod - docker reverse proxy method
     * @return - if true - add docker snippet
     */
    private boolean addDockerRelatedSnippet(String dockerConfig, ReverseProxyMethod dockerReverseProxyMethod) {
        return !StringUtils.isBlank(dockerConfig) && !dockerReverseProxyMethod.toString().equals("subDomain") &&
                !dockerReverseProxyMethod.toString().equals("noValue");
    }

    /**
     * generate general reverse proxy snippet
     *
     * @param dockerReverseProxyMethod - docker reverse proxy method
     * @return if true - generate general reverse proxy snippet
     */
    private boolean generateGeneralReverseProxySnippet(ReverseProxyMethod dockerReverseProxyMethod) {
        return (dockerReverseProxyMethod != null);
    }
}