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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Shay Yaakov
 * @author Chen Keinan
 */
public abstract class ReverseProxyTemplateProvider {
    private static final Logger log = LoggerFactory.getLogger(ReverseProxyTemplateProvider.class);

    @Autowired
    private AddonsManager addonsManager;

    /**
     * @return
     */
    public String provideGeneralServerConfigServer(List<String> repoKeys) {
        CentralConfigService centralConfig = ContextHelper.get().getCentralConfig();
        ReverseProxyDescriptor reverseProxy = centralConfig.getMutableDescriptor().getCurrentReverseProxy();
        String snippet = getGeneralReverseProxySnippet(reverseProxy, repoKeys);
        if (snippet != null) {
            return snippet;
        }
        return null;
    }



    /**
     * return docker reverse proxy snippet
     * @param repo - docker repository
     * @param config  - reverse proxy config
     * @param generalOnly - if true general config
     * @return -  snippet
     */
    //TODO: Remove "boolean isNginx" from method signature
    protected String buildDockerTemplate(ReverseProxyDescriptor reverseProxyDescriptor, RepoDescriptor repo,
            ReverseProxyRepoConfig config, boolean generalOnly, String templatePath) {
        try {
            FilteredResourcesAddon filteredResourcesAddon = addonsManager.addonByType(FilteredResourcesAddon.class);
            ReverseProxyMethod dockerReverseProxyMethod = reverseProxyDescriptor.getDockerReverseProxyMethod();
            InputStreamReader reader = getInputStreamReader(templatePath);
            int repoPort = config.getPort();
            int generalPort = getGeneralPort(reverseProxyDescriptor);
            Map<Object, Object> params = Maps.newHashMap();
            // add template variables
            params.put("repoKey", repo.getKey());
            params.put("addOnce", false);
            params.put("addGeneral", false);
            params.put("addSsl", false);
            params.put("useHttp", reverseProxyDescriptor.isUseHttp());
            params.put("httpPort", reverseProxyDescriptor.getHttpPort());
            params.put("serverName", reverseProxyDescriptor.getServerName());
            params.put("useHttps", reverseProxyDescriptor.isUseHttps());
            params.put("httpOnly", false);
            params.put("httpsOnly", false);
            String artifactoryAppContext = reverseProxyDescriptor.getArtifactoryAppContext();
            artifactoryAppContext = updateAppContext(artifactoryAppContext);
            params.put("absoluteAppContext", artifactoryAppContext);
            params.put("appContext", artifactoryAppContext);
            String publicAppContext = reverseProxyDescriptor.getPublicAppContext();
            ReverseProxyDescriptor reverseProxy = reverseProxyDescriptor;
            updateWebPublicContext(params, reverseProxy);
            params.put("publicContext", publicAppContext);
            params.put("quotes", "\"");
            updatePublicContextWithSlash(params, reverseProxy);
            addHaApacheForDocker(params);
            updateLocalNameAndPortData(reverseProxyDescriptor, params,
                    reverseProxyDescriptor.getArtifactoryPort());
            params.put("upstreamName", reverseProxyDescriptor.getUpStreamName());
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

    /**
     * return reverse proxy snippet
     * @param repoDescriptor - repo descriptor
     * @param reverseProxyRepoConfig - reverse proxy descriptor
     * @param generalOnly - is general data
     * @return  snippet
     */
    protected abstract String getReverseProxySnippet(ReverseProxyDescriptor reverseProxy, RepoDescriptor repoDescriptor,
            ReverseProxyRepoConfig reverseProxyRepoConfig,
            boolean generalOnly);

    /**
     * check weather general ssl port and docker port are the same
     * @param reverseProxy - general port config
     * @param reverseProxyRepoConfig - docker port config
     * @return
     */
    protected boolean isGeneralSslAndDockerPortAreTheSame(ReverseProxyDescriptor reverseProxy,
            ReverseProxyRepoConfig reverseProxyRepoConfig) {
        return reverseProxy.isUseHttps() && reverseProxy.getSslPort() == (reverseProxyRepoConfig.getPort());
    }

    /**
     * generate reverse proxy snippet
     *
     * @param reverseProxyDescriptor - reverse proxy descriptor
     * @return reverse proxy snippet
     */
    protected abstract String getGeneralReverseProxySnippet(ReverseProxyDescriptor reverseProxyDescriptor,
            List<String> repoKeys);


    /**
     * return provide global snippet by reverse proxy id
     *
     * @return - proxy snippet
     */
    public String provideDockerReverseProxyServerSnippet(List<String> repoKeys) {
        ReverseProxyDescriptor reverseProxy = ContextHelper.get().getCentralConfig().
                getMutableDescriptor().getCurrentReverseProxy();
        List<ReverseProxyRepoConfig> reverseProxyRepoConfigs = reverseProxy.getReverseProxyRepoConfigs();
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
                } else {
                    if (isGeneralSslAndDockerPortAreTheSame(reverseProxy, reverseProxyRepoConfig)) {
                        repoKeys.add(reverseProxyRepoConfig.getRepoRef().getKey());
                    }
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


    protected String updateAppContext(String artifactoryAppContext) {
        if (!StringUtils.isEmpty(artifactoryAppContext)){
            artifactoryAppContext = artifactoryAppContext+"/";
        }
        return artifactoryAppContext;
    }

    protected void updateWebPublicContext(Map<Object, Object> params, ReverseProxyDescriptor reverseProxy) {
        String webPublicAppContext = reverseProxy.getPublicAppContext();
        if (StringUtils.isEmpty(webPublicAppContext)){
            webPublicAppContext = "/";
        }else{
            webPublicAppContext = "/"+webPublicAppContext+"/";
        }
        params.put("webPublicContext", webPublicAppContext);
    }

    /**
     * update public context value with slash
     *
     * @param params       - params map
     * @param reverseProxy - reverse proxy descriptor
     */
    public void updatePublicContextWithSlash(Map<Object, Object> params, ReverseProxyDescriptor reverseProxy) {
        String publicAppContext = reverseProxy.getPublicAppContext();
        if (!StringUtils.isEmpty(publicAppContext)){
            publicAppContext = "/"+publicAppContext;
        }
        params.put("publicContextWithSlash", publicAppContext);
    }

    /**
     * update local host data
     * @param reverseProxyDescriptor - reverse proxy config
     * @param params - template param
     */
    protected void updateLocalNameAndPortData(ReverseProxyDescriptor reverseProxyDescriptor,
                                            Map<Object, Object> params,int port) {
        if (isHaConfigure()){
            params.put("localNameAndPort",reverseProxyDescriptor.getUpStreamName());
        }else{
            params.put("localNameAndPort", reverseProxyDescriptor.getArtifactoryServerName()+":"+port);
        }
    }

    /**
     * @param dockerReverseProxyMethod - docker reverse proxy method
     * @param params                   - params map
     */
    protected void updateSubDomain(ReverseProxyMethod dockerReverseProxyMethod, Map<Object, Object> params) {
        if (dockerReverseProxyMethod != null) {
            params.put("subdomain", dockerReverseProxyMethod.toString().equals("subDomain"));
        } else {
            params.put("subdomain", false);
        }
    }

    /**
     * return general nginx reverse proxy snippet
     * @param reverseProxyDescriptor - reverse proxy descriptor
     * @return nginx general snippet
     */
    protected String buildGeneralTemplate(ReverseProxyDescriptor reverseProxyDescriptor,
            String templatePath, List<String> repoKeys, ReverseProxyPorts reverseProxyPorts, boolean addHa) {
        try {
            FilteredResourcesAddon filteredResourcesAddon = addonsManager.addonByType(FilteredResourcesAddon.class);
            InputStreamReader reader = getInputStreamReader(templatePath);
            Map<Object, Object> params = Maps.newHashMap();
            ReverseProxyMethod dockerReverseProxyMethod = reverseProxyDescriptor.getDockerReverseProxyMethod();
            int sslPort = reverseProxyDescriptor.getSslPort();
            // update template variables
            if (!repoKeys.isEmpty()) {
                params.put("repoKey", repoKeys.get(0));
            }
            params.put("serverName", reverseProxyDescriptor.getServerName());
            updateGeneralHttpPorts(reverseProxyDescriptor, params, sslPort,reverseProxyPorts);
            String artifactoryAppContext = reverseProxyDescriptor.getArtifactoryAppContext();
            artifactoryAppContext = updateAppContext(artifactoryAppContext);
            params.put("absoluteAppContext", StringUtils.isEmpty(artifactoryAppContext) ? "" : artifactoryAppContext.replaceAll("/", ""));
            params.put("appContext", artifactoryAppContext);
            params.put("publicContext", reverseProxyDescriptor.getPublicAppContext());
            params.put("webapp",StringUtils.isEmpty(reverseProxyDescriptor.getPublicAppContext())?"webapp":"");
            updatePublicContextWithSlash(params, reverseProxyDescriptor);
            params.put("sslCrtPath", reverseProxyDescriptor.getSslCertificate());
            params.put("sslKeyPath", reverseProxyDescriptor.getSslKey());
            boolean isSamePort = isDockerAndGeneralSamePort(reverseProxyDescriptor);
            boolean isSamePortValue = isSamePort && reverseProxyDescriptor.getDockerReverseProxyMethod().equals(ReverseProxyMethod.PORTPERREPO);
            params.put("isSamePort", isSamePortValue);
            addHaConfiguration(params, addHa);
            addHaFlag(params);
            params.put("addOnce", addHa);
            updateWebPublicContext(params, reverseProxyDescriptor);
            updateLocalNameAndPortData(reverseProxyDescriptor, params,
                    reverseProxyDescriptor.getArtifactoryPort());
            params.put("upstreamName", reverseProxyDescriptor.getUpStreamName());
            params.put("generalOnly", true);
            params.put("addGeneral",true);
            params.put("quotes", "\"");
            updateSubDomainVariable(params, dockerReverseProxyMethod);
            //  params.put("isDocker", !noValue);
            return filteredResourcesAddon.filterResource(reader, params);
        } catch (Exception e) {
            log.error("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
            log.debug("Unable to filter nginx reverse proxy template: " + e.getMessage(), e);
        }
        return "";
    }

    /**
     * update general http ports
     *
     * @param reverseProxyDescriptor - reverse proxy descriptor
     * @param params                 - params map
     * @param sslPort                - ssl port
     * @param reverseProxyPorts
     */
    protected void updateGeneralHttpPorts(ReverseProxyDescriptor reverseProxyDescriptor, Map<Object, Object> params,
            int sslPort, ReverseProxyPorts reverseProxyPorts) {
        params.put("sslPort", sslPort);
        params.put("addSsl", reverseProxyDescriptor.isUseHttps());
        params.put("useHttps", reverseProxyDescriptor.isUseHttps());
        params.put("useHttp", reverseProxyDescriptor.isUseHttp());
        params.put("httpPort", reverseProxyDescriptor.getHttpPort());
    }

    private boolean isDockerAndGeneralSamePort(ReverseProxyDescriptor reverseProxyDescriptor) {
        return reverseProxyDescriptor.getReverseProxyRepoConfigs().stream().anyMatch(reverseProxyRepoConfig ->
                reverseProxyRepoConfig.getPort() == reverseProxyDescriptor.getSslPort());
    }

    /**
     * @param params - params map
     */
    protected void addHaConfiguration(Map<Object, Object> params, boolean addForThisTemplate) {
        if (addForThisTemplate && isHaConfigure()) {
            haData(params);
        }
    }

    /**
     * @param params - params map
     */
    protected void addHaFlag(Map<Object, Object> params) {
        if (isHaConfigure()) {
            params.put("addHa", true);
        } else {
            params.put("addHa", false);
        }
    }

    protected abstract boolean isNginx();

    /**
     * look for template file in etc 1st and then in resource
     * @param templatePath
     * @return
     */
    protected InputStreamReader getInputStreamReader(String templatePath){
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

    protected void updateSubDomainVariable(Map<Object, Object> params, ReverseProxyMethod dockerReverseProxyMethod) {
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
    protected int getGeneralPort(ReverseProxyDescriptor reverseProxyDescriptor) {
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
     *
     * @return list of servers (ip and port)
     */
    protected List<String> fetchHaServerList() {
        List<String> servers = new ArrayList<>();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
        List<ArtifactoryServer> allArtifactoryServers = haCommonAddon.getAllArtifactoryServers();
        allArtifactoryServers.forEach(server -> {
            String serverAndPortFromContext = getServerBalancerKey(server);
            if (!StringUtils.isEmpty(serverAndPortFromContext)) {
                servers.add(serverAndPortFromContext);
            }
        });
        return servers;
    }

    protected abstract String getServerBalancerKey(ArtifactoryServer server);

    /**
     * check if ha is configure
     * @return true if ha is configure
     */
    public boolean isHaConfigure() {
        return !fetchHaServerList().isEmpty();
    }

    /**
     * check if ha is configure
     *
     * @param params
     * @return true if ha is configure
     */
    public abstract boolean addHaApacheForDocker(Map<Object, Object> params);
}
