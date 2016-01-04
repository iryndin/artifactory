package org.artifactory.ui.rest.service.admin.configuration;

import org.artifactory.rest.common.service.admin.reverseProxies.CheckReverseProxyPortAvailabilityService;
import org.artifactory.rest.common.service.admin.reverseProxies.CreateReverseProxyService;
import org.artifactory.rest.common.service.admin.reverseProxies.GetReverseProxiesService;
import org.artifactory.rest.common.service.admin.reverseProxies.UpdateReverseProxyService;
import org.artifactory.ui.rest.model.admin.configuration.repository.local.LocalRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.remote.RemoteRepositoryConfigModel;
import org.artifactory.ui.rest.model.admin.configuration.repository.virtual.VirtualRepositoryConfigModel;
import org.artifactory.ui.rest.service.admin.configuration.bintray.GetBintrayUIService;
import org.artifactory.ui.rest.service.admin.configuration.bintray.TestBintrayUIService;
import org.artifactory.ui.rest.service.admin.configuration.bintray.UpdateBintrayUIService;
import org.artifactory.ui.rest.service.admin.configuration.blackduck.GetBlackDuckService;
import org.artifactory.ui.rest.service.admin.configuration.blackduck.TestBlackDuckService;
import org.artifactory.ui.rest.service.admin.configuration.blackduck.UpdateBlackDuckService;
import org.artifactory.ui.rest.service.admin.configuration.general.*;
import org.artifactory.ui.rest.service.admin.configuration.ha.GetHighAvailabilityMembersService;
import org.artifactory.ui.rest.service.admin.configuration.ha.RemoveServerService;
import org.artifactory.ui.rest.service.admin.configuration.layouts.*;
import org.artifactory.ui.rest.service.admin.configuration.licenses.*;
import org.artifactory.ui.rest.service.admin.configuration.mail.GetMailService;
import org.artifactory.ui.rest.service.admin.configuration.mail.TestMailService;
import org.artifactory.ui.rest.service.admin.configuration.mail.UpdateMailService;
import org.artifactory.ui.rest.service.admin.configuration.propertysets.*;
import org.artifactory.ui.rest.service.admin.configuration.proxies.CreateProxyService;
import org.artifactory.ui.rest.service.admin.configuration.proxies.DeleteProxyService;
import org.artifactory.ui.rest.service.admin.configuration.proxies.GetProxiesService;
import org.artifactory.ui.rest.service.admin.configuration.proxies.UpdateProxyService;
import org.artifactory.ui.rest.service.admin.configuration.registerpro.GetLicenseKeyService;
import org.artifactory.ui.rest.service.admin.configuration.registerpro.UpdateLicenseKeyService;
import org.artifactory.ui.rest.service.admin.configuration.repositories.CreateRepositoryConfigService;
import org.artifactory.ui.rest.service.admin.configuration.repositories.DeleteRepositoryConfigService;
import org.artifactory.ui.rest.service.admin.configuration.repositories.GetRepositoryConfigService;
import org.artifactory.ui.rest.service.admin.configuration.repositories.UpdateRepositoryConfigService;
import org.artifactory.ui.rest.service.admin.configuration.repositories.replication.*;
import org.artifactory.ui.rest.service.admin.configuration.repositories.util.*;
import org.artifactory.ui.rest.service.admin.configuration.repositories.util.validator.ValidateRepoNameService;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Chen Keinan
 */
public abstract class ConfigServiceFactory {


    // mail services
    @Lookup
    public abstract UpdateMailService updateMailService();

    @Lookup
    public abstract GetMailService getMailService();

    @Lookup
    public abstract TestMailService testMailService();

    //register pro service
    @Lookup
    public abstract GetLicenseKeyService getLicenseKeyService();

    @Lookup
    public abstract UpdateLicenseKeyService updateLicenseKeyService();

    // proxies services
    @Lookup
    public abstract CreateProxyService createProxiesService();

    @Lookup
    public abstract UpdateProxyService updateProxiesService();

    @Lookup
    public abstract GetProxiesService getProxiesService();

    @Lookup
    public abstract DeleteProxyService deleteProxiesService();

    // reverse proxies services
    @Lookup
    public abstract CreateReverseProxyService createReverseProxy();

    @Lookup
    public abstract UpdateReverseProxyService updateReverseProxy();

    @Lookup
    public abstract GetReverseProxiesService getReverseProxies();

    // licenses services
    @Lookup
    public abstract ExportLicenseFileService exportLicenseFileService();

    @Lookup
    public abstract CreateArtifactLicenseService createArtifactLicenseService();

    @Lookup
    public abstract UpdateArtifactLicenseService updateArtifactLicenseService();

    @Lookup
    public abstract GetArtifactLicenseService getArtifactLicenseService();

    @Lookup
    public abstract DeleteArtifactLicenseService deleteArtifactLicenseService();

    // black duck services
    @Lookup
    public abstract UpdateBlackDuckService updateBlackDuckService();

    @Lookup
    public abstract GetBlackDuckService getBlackDuckService();

    @Lookup
    public abstract TestBlackDuckService testBlackDuckService();

    // bintray services
    @Lookup
    public abstract UpdateBintrayUIService updateBintrayService();

    @Lookup
    public abstract GetBintrayUIService getBintrayService();

    @Lookup
    public abstract TestBintrayUIService testBintrayService();

    @Lookup
    public abstract GetGeneralConfigService getGeneralConfig();

    @Lookup
    public abstract GetGeneralConfigDataService getGeneralConfigData();

    @Lookup
    public abstract UpdateGeneralConfigService updateGeneralConfig();

    @Lookup
    public abstract UploadLogoService uploadLogo();

    @Lookup
    public abstract GetUploadLogoService getUploadLogo();

    @Lookup
    public abstract DeleteUploadedLogoService deleteUploadedLogo();

    @Lookup
    public abstract CreatePropertySetService createPropertySet();

    @Lookup
    public abstract GetConfigPropertySetNamesService getPropertySetNames();

    @Lookup
    public abstract GetConfigPropertySetService getPropertySet();

    @Lookup
    public abstract UpdatePropertySetService updatePropertySet();

    @Lookup
    public abstract DeletePropertySetService deletePropertySet();

    // configuration repository service
    @Lookup
    public abstract CreateRepositoryConfigService createRepositoryConfig();

    @Lookup
    public abstract GetRepositoryConfigService getRepositoryConfig();

    @Lookup
    public abstract UpdateRepositoryConfigService updateRepositoryConfig();

    @Lookup
    public abstract DeleteRepositoryConfigService deleteRepositoryConfig();

    @Lookup
    public abstract GetRepositoryInfoService getRepositoriesInfo();

    @Lookup
    public abstract RemoteRepositoryTestUrl<RemoteRepositoryConfigModel> remoteRepositoryTestUrl();

    @Lookup
    public abstract SmartRepoCapabilitiesDiscoveringService<RemoteRepositoryConfigModel> discoverSmartRepoCapabilities();

    @Lookup
    public abstract GetAvailableRepositoryFields getAvailableRepositoryFieldChoices();

    @Lookup
    public abstract GetDefaultRepositoryValues getDefaultRepositoryValues();

    @Lookup
    public abstract GetRemoteRepoUrlMappingService getRemoteReposUrlMapping();

    @Lookup
    public abstract ValidateRepoNameService validateRepoName();

    @Lookup
    public abstract ExecuteRemoteReplicationService executeImmediateReplication();

    @Lookup
    public abstract TestLocalReplicationService testLocalReplication();

    @Lookup
    public abstract ValidateLocalReplicationService validateLocalReplication();

    @Lookup
    public abstract TestRemoteReplicationService testRemoteReplication();

    @Lookup
    public abstract GetLayoutsService getLayoutsService();

    @Lookup
    public abstract GetLayoutInfoService getLayoutInfoService();

    @Lookup
    public abstract UpdateLayoutService updateLayoutService();

    @Lookup
    public abstract CreateLayoutService createLayoutService();

    @Lookup
    public abstract DeleteLayoutService deleteLayoutService();

    @Lookup
    public abstract TestArtPathService testArtPathService();

    @Lookup
    public abstract ResolveRegexService resolveRegexService();

    @Lookup(value = "allAvailableRepositories")
    public abstract GetAvailableRepositories getAvailableRepositories();

    @Lookup
    public abstract GetIndexerAvailableRepositories getIndexerAvailableRepositories();

    @Lookup
    public abstract GetResolvedRepositories<VirtualRepositoryConfigModel> getResolvedRepositories();

    @Lookup
    public abstract ExecuteAllLocalReplicationsService executeAllLocalReplications();

    @Lookup
    public abstract ExecuteLocalReplicationService<LocalRepositoryConfigModel> executeLocalReplication();

    @Lookup
    public abstract ReorderRepositoriesService reorderRepositories();

    @Lookup
    public abstract IsJcenterConfiguredService isJcenterConfigured();

    @Lookup
    public abstract CreateDefaultJcenterRepoService createDefaultJcenterRepo();

    @Lookup
    public abstract GetHighAvailabilityMembersService getHighAvailabilityMembers();

    @Lookup
    public abstract RemoveServerService removeServer();

    @Lookup
    public abstract CheckReverseProxyPortAvailabilityService checkReverseProxyPortAvailability();


}
