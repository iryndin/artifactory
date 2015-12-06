package org.artifactory.repo.reverseProxy;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.filteredresources.FilteredResourcesAddon;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.repo.*;
import org.artifactory.storage.db.servers.model.ArtifactoryServer;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Shay Yaakov
 */
@Component
public class ReverseProxyTemplateProvider {
    private static final Logger log = LoggerFactory.getLogger(ReverseProxyTemplateProvider.class);

    @Autowired
    private AddonsManager addonsManager;

    /**
     * @return
     */
    public String provideGeneralServerConfigServer(String reverseProxyKey) {
        CentralConfigService centralConfig = ContextHelper.get().getCentralConfig();
        ReverseProxyDescriptor reverseProxy = centralConfig.getMutableDescriptor().getReverseProxy(reverseProxyKey);
        String snippet = getGeneralReverseProxySnippet(reverseProxy);
        if (snippet != null) {
            return snippet;
        }
        return null;
    }


    /**
     * return reverse proxy snippet
     * @param repoDescriptor - repo descriptor
     * @param reverseProxyRepoConfig - reverse proxy descriptor
     * @param generalOnly - is general data
     * @return  snippet
     */
    private String getReverseProxySnippet(ReverseProxyDescriptor reverseProxy, RepoDescriptor repoDescriptor, ReverseProxyRepoConfig reverseProxyRepoConfig,
                                          boolean generalOnly) {
        if (isGeneralSslAndDockerPortAreTheSame(reverseProxy, reverseProxyRepoConfig)){
            return "";
        }
        if (reverseProxyRepoConfig != null) {
            switch (reverseProxy.getWebServerType()) {
                case NGINX:
                    return buildDockerTemplate(reverseProxy,repoDescriptor, reverseProxyRepoConfig, generalOnly, "/templates/nginx.ftl",true);
                case APACHE:
                    return buildDockerTemplate(reverseProxy,repoDescriptor, reverseProxyRepoConfig, generalOnly, "/templates/apache.ftl",false);
            }
        }
        return null;
    }

    /**
     * check wheater general ssl port and docker port are the same
     * @param reverseProxy - general port config
     * @param reverseProxyRepoConfig - docker port config
     * @return
     */
    private boolean isGeneralSslAndDockerPortAreTheSame(ReverseProxyDescriptor reverseProxy, ReverseProxyRepoConfig reverseProxyRepoConfig) {
        return reverseProxy.isUseHttps() && reverseProxy.getSslPort() == (reverseProxyRepoConfig.getPort());
    }

    /**
     * generate reverse proxy snippet
     *
     * @param reverseProxyDescriptor - reverse proxy descriptor
     * @return reverse proxy snippet
     */
    private String getGeneralReverseProxySnippet(ReverseProxyDescriptor reverseProxyDescriptor) {
        switch (reverseProxyDescriptor.getWebServerType()) {
            case NGINX:
                return buildGeneralTemplate(reverseProxyDescriptor,"/templates/nginx.ftl",true);
            case APACHE:
                return buildGeneralTemplate(reverseProxyDescriptor,"/templates/apache.ftl",false);
        }
        return null;
    }

    /**
     * return provide global snippet by reverse proxy id
     *
     * @return - proxy snippet
     */
    public String provideDockerReverseProxyServerSnippet(String reverseProxyKey) {
        ReverseProxyDescriptor reverseProxy = ContextHelper.get().getCentralConfig().
                getMutableDescriptor().getReverseProxy(reverseProxyKey);
        List<ReverseProxyRepoConfig> reverseProxyRepoConfigs = reverseProxy.getReverseProxyRepoConfigs();
        // Set<RepoDescriptor> allRepoDescriptors = getAllReposDescriptors();
        StringBuilder snippetBuilder = new StringBuilder();
        reverseProxyRepoConfigs.forEach(reverseProxyRepoConfig -> {
            if (reverseProxyRepoConfig != null) {
                RepoBaseDescriptor repoRef = reverseProxyRepoConfig.getRepoRef();
                if (!repoRef.getType().equals(RepoType.Docker)) {
                    return;
                }
                String snippet = getReverseProxySnippet(reverseProxy,repoRef, reverseProxyRepoConfig, false);
                if (!StringUtils.isEmpty(snippet)) {
                    snippetBuilder.append(snippet).append("\n");
                }
            }
        });
        String snippetToString = snippetBuilder.toString();
        if (!StringUtils.isEmpty(snippetToString)) {
            return snippetToString;
        }
        return null;
    }

    /**
     * return general nginx reverse proxy snippet
     *
     * @return nginx general snippet
     */
    public String haData(Map<Object, Object> params) {
        try {
            List<String> haData = fetchHaServerList();
            params.put("hsservers",haData);
        } catch (Exception e) {
            log.error("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
            log.debug("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
        }
        return "";
    }


    /**
     * return docker reverse proxy snippet
     * @param repo - docker repository
     * @param config  - reverse proxy config
     * @param generalOnly - if true general config
     * @return -  snippet
     */
    private String buildDockerTemplate(ReverseProxyDescriptor reverseProxyDescriptor ,RepoDescriptor repo, ReverseProxyRepoConfig config,boolean generalOnly,
                                       String templatePath,boolean isNginx) {
        try {
            FilteredResourcesAddon filteredResourcesAddon = addonsManager.addonByType(FilteredResourcesAddon.class);
            ReverseProxyMethod dockerReverseProxyMethod = reverseProxyDescriptor.getDockerReverseProxyMethod();
            boolean noValue = dockerReverseProxyMethod.toString().equals("noValue");
            InputStreamReader reader = getInputStreamReader(templatePath);
            int repoPort = config.getPort();
            int generalPort = getNginxGeneralPort(reverseProxyDescriptor);
            Map<Object, Object> params = Maps.newHashMap();
            // add template variables
            params.put("repoKey", repo.getKey());
            params.put("addHa",false);
            params.put("addGeneral",false);
            params.put("addSsl", false);
            params.put("useHttp",reverseProxyDescriptor.isUseHttp());
            params.put("httpPort",reverseProxyDescriptor.getHttpPort());
            params.put("serverName", reverseProxyDescriptor.getServerName());
            params.put("useHttps", reverseProxyDescriptor.isUseHttps());
            String artifactoryAppContext = reverseProxyDescriptor.getArtifactoryAppContext();
            artifactoryAppContext = updateAppContext(artifactoryAppContext);
            params.put("appContext", artifactoryAppContext);
            String publicAppContext = reverseProxyDescriptor.getPublicAppContext();
            ReverseProxyDescriptor reverseProxy = reverseProxyDescriptor;
            updateWebPublicContext(params, reverseProxy);
            params.put("publicContext", publicAppContext);
            updatePublicContextWithSlash(params, reverseProxy);
            updateLocalNameAndPortData(reverseProxyDescriptor, isNginx, params,
                    reverseProxyDescriptor.getArtifactoryPort());
            params.put("upstreamName", reverseProxyDescriptor.getUpStreamName());
            params.put("repoKey", repo.getKey());
            params.put("generalOnly", generalOnly);
            params.put("generalPort", generalPort);
            updateSubDomain(dockerReverseProxyMethod, params);
            params.put("repoPort", repoPort);
            return filteredResourcesAddon.filterResource(reader, params);
        } catch (Exception e) {
            log.error("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
            log.debug("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
        }
        return "";
    }

    private String updateAppContext(String artifactoryAppContext) {
        if (!StringUtils.isEmpty(artifactoryAppContext)){
            artifactoryAppContext = artifactoryAppContext+"/";
        }
        return artifactoryAppContext;
    }

    private void updateWebPublicContext(Map<Object, Object> params, ReverseProxyDescriptor reverseProxy) {
        String webPublicAppContext = reverseProxy.getPublicAppContext();
        if (StringUtils.isEmpty(webPublicAppContext)){
            webPublicAppContext = "/";
        }else{
            webPublicAppContext = "/"+webPublicAppContext+"/";
        }
        params.put("webPublicContext", webPublicAppContext);
    }

    private void updatePublicContextWithSlash(Map<Object, Object> params, ReverseProxyDescriptor reverseProxy) {
        String publicAppContext = reverseProxy.getPublicAppContext();
        if (!StringUtils.isEmpty(publicAppContext)){
            publicAppContext = "/"+publicAppContext;
        }
        params.put("publicContextWithSlash", publicAppContext);
    }




    /**
     * update local host data
     * @param reverseProxyDescriptor - reverse proxy config
     * @param isNginx - if true nginx
     * @param params - template param
     */
    private void updateLocalNameAndPortData(ReverseProxyDescriptor reverseProxyDescriptor, boolean isNginx,
                                            Map<Object, Object> params,int port) {
        if (isNginx && isHaConfigure()){
            params.put("localNameAndPort",reverseProxyDescriptor.getUpStreamName());
        }else{
            params.put("localNameAndPort", reverseProxyDescriptor.getArtifactoryServerName()+":"+port);
        }
    }

    private void updateSubDomain(ReverseProxyMethod dockerReverseProxyMethod, Map<Object, Object> params) {
        if (dockerReverseProxyMethod != null) {
            params.put("subdomain", dockerReverseProxyMethod.toString().equals("subDomain"));
        } else {
            params.put("subdomain", false);
        }
    }

    private void updateDockerApiVersion(RepoDescriptor repo, Map<Object, Object> params) {
        if (repo instanceof HttpRepoDescriptor || repo instanceof VirtualRepoDescriptor) {
            params.put("v1", false);
        } else {
            params.put("v1", repo.getDockerApiVersion().toString().equals("V1"));
        }
    }

    /**
     * return general nginx reverse proxy snippet
     * @param reverseProxyDescriptor - reverse proxy descriptor
     * @return nginx general snippet
     */
    private String buildGeneralTemplate(ReverseProxyDescriptor reverseProxyDescriptor,String templatePath,boolean isNginx) {
        try {
            FilteredResourcesAddon filteredResourcesAddon = addonsManager.addonByType(FilteredResourcesAddon.class);
            InputStreamReader reader = getInputStreamReader(templatePath);
            Map<Object, Object> params = Maps.newHashMap();
            ReverseProxyMethod dockerReverseProxyMethod = reverseProxyDescriptor.getDockerReverseProxyMethod();
            int sslPort = reverseProxyDescriptor.getSslPort();
            // update template variables
            params.put("serverName", reverseProxyDescriptor.getServerName());
            params.put("addSsl",reverseProxyDescriptor.isUseHttps());
            params.put("useHttps", reverseProxyDescriptor.isUseHttps());
            params.put("useHttp",reverseProxyDescriptor.isUseHttp());
            params.put("httpPort",reverseProxyDescriptor.getHttpPort());
            String artifactoryAppContext = reverseProxyDescriptor.getArtifactoryAppContext();
            artifactoryAppContext = updateAppContext(artifactoryAppContext);
            params.put("appContext", artifactoryAppContext);
            params.put("publicContext", reverseProxyDescriptor.getPublicAppContext());
            params.put("webapp",StringUtils.isEmpty(reverseProxyDescriptor.getPublicAppContext())?"webapp":"");
            updatePublicContextWithSlash(params, reverseProxyDescriptor);
            params.put("sslCrtPath", reverseProxyDescriptor.getSslCertificate());
            params.put("sslKeyPath", reverseProxyDescriptor.getSslKey());
            boolean isSamePort = reverseProxyDescriptor.getReverseProxyRepoConfigs().stream().anyMatch(reverseProxyRepoConfig ->
                    reverseProxyRepoConfig.getPort() == reverseProxyDescriptor.getSslPort());
            boolean isSamePortValue = isSamePort && reverseProxyDescriptor.getDockerReverseProxyMethod().equals(ReverseProxyMethod.PORTPERREPO);
            params.put("isSamePort", isSamePortValue);
            addHaConfiguration(isNginx, params);
            updateWebPublicContext(params, reverseProxyDescriptor);
            updateLocalNameAndPortData(reverseProxyDescriptor, isNginx, params,
                    reverseProxyDescriptor.getArtifactoryPort());
            params.put("upstreamName", reverseProxyDescriptor.getUpStreamName());
            params.put("generalOnly", true);
            params.put("addGeneral",true);
            updateSubDomainVariable(params, dockerReverseProxyMethod);
            params.put("sslPort", sslPort);
            //  params.put("isDocker", !noValue);
            return filteredResourcesAddon.filterResource(reader, params);
        } catch (Exception e) {
            log.error("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
            log.debug("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
        }
        return "";
    }

    private void addHaConfiguration(boolean isNginx, Map<Object, Object> params) {
        if (isNginx && isHaConfigure()){
            params.put("addHa",true);
            haData(params);
        }else{
            params.put("addHa",false);
        }
    }

    /**
     * look for template file in etc 1st and then in resource
     * @param templatePath
     * @return
     */
    private InputStreamReader getInputStreamReader(String templatePath){
        InputStreamReader reader = null;
        File etcDir = ContextHelper.get().getArtifactoryHome().getEtcDir();
        File file = new File(etcDir,File.separator + templatePath.replace("/templates/", ""));
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file.toString());
        } catch (FileNotFoundException e) {
            log.debug("file {} not found",file.toString());
        }
        if (inputStream != null) {
            reader = new InputStreamReader(inputStream);
        }
        if (reader == null) {
            reader = getReader(templatePath);
        }
        return reader;
    }

    private void updateSubDomainVariable(Map<Object, Object> params, ReverseProxyMethod dockerReverseProxyMethod) {
        if (dockerReverseProxyMethod != null) {
            params.put("subdomain", dockerReverseProxyMethod.toString().equals("subDomain"));
        } else {
            params.put("subdomain", false);
        }
    }

    /**
     * return nginx port , 1st check if exist in repo if noot take the value from global config
     *
     * @param reverseProxyDescriptor - nginx repo config
     * @return - nginx port
     */
    private int getNginxGeneralPort(ReverseProxyDescriptor reverseProxyDescriptor) {
        int port;
        if (reverseProxyDescriptor.isUseHttps()) {
            port = reverseProxyDescriptor.getSslPort();
        } else {
            port = reverseProxyDescriptor.getHttpPort();
        }
        return port;
    }

    private InputStreamReader getReader(String templateResourcePath) {
        InputStream stream = getClass().getResourceAsStream(templateResourcePath);
        return new InputStreamReader(stream);
    }

    /**
     * fetch server ip and port list
     * @return list of servers (ip and port)
     */
    private List<String> fetchHaServerList() {
        List<String> servers = new ArrayList<>();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        List<ArtifactoryServer> allArtifactoryServers = haCommonAddon.getAllArtifactoryServers();
        allArtifactoryServers.forEach(server->{
            String serverAndPortFromContext = HttpUtils.getServerAndPortFromContext(server.getContextUrl());
            if (!StringUtils.isEmpty(serverAndPortFromContext)) {
                servers.add(serverAndPortFromContext);
            }
        });
        return servers;
    }

    /**
     * check if ha is configure
     * @return true if ha is configure
     */
    public boolean isHaConfigure() {
        return !fetchHaServerList().isEmpty();
    }
}
