package org.artifactory.rest.services;

import org.artifactory.rest.common.service.admin.advance.GetStorageSummaryService;
import org.artifactory.rest.common.service.admin.reverseProxies.CreateReverseProxyService;
import org.artifactory.rest.common.service.admin.reverseProxies.GetReverseProxiesService;
import org.artifactory.rest.common.service.admin.reverseProxies.ReverseProxySnippetService;
import org.artifactory.rest.common.service.admin.reverseProxies.UpdateReverseProxyService;
import org.artifactory.rest.common.service.admin.userprofile.*;
import org.artifactory.rest.common.service.artifact.AddSha256ToArtifactService;
import org.artifactory.rest.services.replication.*;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Chen Keinan
 */
public abstract class ConfigServiceFactory {

    // get replication services
    @Lookup
    public abstract GetReplicationService getReplication();

    @Lookup
    public abstract CreateReplicationService createOrReplaceReplication();

    @Lookup
    public abstract CreateMultipleReplicationService createMultipleReplication();

    @Lookup
    public abstract UpdateReplicationService updateReplication();

    @Lookup
    public abstract UpdateMultipleReplicationsService updateMultipleReplications();

    @Lookup
    public abstract DeleteReplicationsService deleteReplicationsService();

    @Lookup
    public abstract GetStorageSummaryService getStorageSummaryService();

    @Lookup
    public abstract GetApiKeyService getApiKey();

    @Lookup
    public abstract CreateApiKeyService createApiKey();

    @Lookup
    public abstract RevokeApiKeyService revokeApiKey();

    @Lookup
    public abstract UpdateApiKeyService regenerateApiKey();

    @Lookup
    public abstract GetUsersAndApiKeys getUsersAndApiKeys();

    @Lookup
    public abstract SyncUsersAndApiKeys syncUsersAndApiKeys();

    @Lookup
    public abstract AddSha256ToArtifactService addSha256ToArtifact();

    @Lookup
    public abstract CreateReverseProxyService createReverseProxy();

    @Lookup
    public abstract UpdateReverseProxyService updateReverseProxy();

    @Lookup
    public abstract GetReverseProxiesService getReverseProxies();

    @Lookup
    public abstract ReverseProxySnippetService getReverseProxySnippet();

}
