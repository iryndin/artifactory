package org.artifactory.service;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.rest.artifact.ItemProperties;
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.RepoResource;
import org.artifactory.md.Properties;
import org.artifactory.repo.HttpRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.cache.expirable.CacheExpiry;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.RepoRequests;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.RemoteRepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides smart repo services
 *
 * @author Michael Pasternak
 */
@Service
public class SmartRepoServiceImpl implements SmartRepoService {

    private static final Logger log = LoggerFactory.getLogger(SmartRepoServiceImpl.class);
    private static final String PROPERTIES = "?properties";
    public static final String REMOTE_DELETED = "sourceDeleted";
    public static final String REMOTE_DELETED_MESSAGE = "The source file of this replica has been deleted";
    private static final String ARTIFACTORY_APP_PATH = "/artifactory";
    private static final String REMOTE_STORAGE_ARTIFACTORY_API_PATH = ARTIFACTORY_APP_PATH + "/api/"+
            ArtifactRestConstants.PATH_ROOT+"/";
    private static final String REMOTE_STORAGE_API_PATH = "/api/"+ ArtifactRestConstants.PATH_ROOT+"/";


    @Autowired
    private InternalRepositoryService repositoryService;

    private PropertiesAddon propertiesAddon;


    /**
     * When triggered on file download, invokes SmartRepo
     * specific logic such as:
     *
     * 1. remote properties update
     * 2. sourceDeleted=true markup
     *
     * @param resource The RepoResource download was triggered for
     */
    @Override
    public void onFileDownload(RepoResource resource) {
        if (isSmartRepo(resource)) {
            log.debug("'{}' resource was downloaded", resource);

            if (resource instanceof UnfoundRepoResource) {
                return;
            } else if (resource instanceof RemoteRepoResource) {
                // this resource cannot be in cache as this is first download,
                // so properties should be updated by old syncProperties mechanism
            } else if (resource instanceof FileResource) {
                LocalRepo localRepo = repositoryService.localRepositoryByKey(resource.getRepoPath().getRepoKey());
                if (localRepo != null) { // local repo
                    if (localRepo.itemExists(resource.getRepoPath().getPath())) {
                        unRegisterRemoteDeletedResource(resource);
                    } else {
                        // no need to save state as will be 404 response
                        // shell it be filtered out at line 59?
                    }
                } else { // not local repo
                    updateProperties(resource);
                }
            }
        }
    }

    /**
     * Updates sourceDeleted state to true
     *
     * @param resource the resource to work on
     */
    private void updateRemoteDeleted(RepoResource resource) {
        if(isOriginAbsenceDetectionEnabled(resource)) {
            log.debug("Verifying remote deleted state for resource {}", resource);
            if (isArtifactMarkedAsRemoteDeleted(resource) || !isArtifactAvailableRemotely(resource)) {
                registerRemoteDeletedResource(resource);
            } else {
                unRegisterRemoteDeletedResource(resource);
            }
        }
    }

    /**
     * @return true if artifact marked as sourceDeleted=true on remote
     *         repository or false
     */
    @Override
    public boolean isArtifactMarkedAsRemoteDeleted(RepoResource repoResource) {
        Properties properties = null;
        PropertiesAddon propertiesAddon = getPropertiesAddon();

        if(isSyncPropertiesEnabled(repoResource)) {
            // if SyncProperties enabled, we get them for free
            // i.e nothing else left to do as sibling node aware
            // of "source" existence/absence, we only need to look
            // at remote properties
            log.debug("Using local properties to check isArtifactMarkedAsRemoteDeleted");
            properties = propertiesAddon.getProperties(repoResource.getRepoPath());
        } else {
            // otherwise we need to check on a sibling node whether
            // it aware of deleted source
            log.debug("Using remote properties to check isArtifactMarkedAsRemoteDeleted");
            properties = fetchRemoteProperties(repoResource);
        }

        if (properties != null) {
            Set<String> content = properties.get(REMOTE_DELETED);
            return content != null && !content.isEmpty();
        }
        return false;
    }

    /**
     * @return {@link PropertiesAddon}
     */
    private PropertiesAddon getPropertiesAddon() {
        if (propertiesAddon == null) {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            propertiesAddon = addonsManager.addonByType(PropertiesAddon.class);
        }
        return propertiesAddon;
    }

    /**
     * Checks whether SyncProperties enabled for the given {@link RepoResource}
     *
     * @param repoResource
     *
     * @return true/false
     */
    private boolean isSyncPropertiesEnabled(RepoResource repoResource) {
        String repoKey = StringUtils.replaceLast(repoResource.getRepoPath().getRepoKey(), "-cache", "");
        HttpRepoDescriptor descriptor = (HttpRepoDescriptor) repositoryService.remoteRepoDescriptorByKey(repoKey);
        if (descriptor != null) {
            return isSmartRepo(repoResource) &&
                    descriptor.getContentSynchronisation().getProperties().isEnabled();
        }
        return false;
    }

    /**
     * Checks whether OriginAbsenceDetection is enabled for the given {@link RepoResource}
     *
     * @param repoResource
     *
     * @return true/false
     */
    private boolean isOriginAbsenceDetectionEnabled(RepoResource repoResource) {
        String repoKey = StringUtils.replaceLast(repoResource.getRepoPath().getRepoKey(), "-cache", "");
        HttpRepoDescriptor descriptor = (HttpRepoDescriptor) repositoryService.remoteRepoDescriptorByKey(repoKey);
        if (descriptor != null) {
            return isSmartRepo(repoResource) &&
                    descriptor.getContentSynchronisation().getSource().isOriginAbsenceDetection();
        }
        return false;
    }

    /**
     * Checks whether SmartRepo is enabled for the given {@param RepoResource}'s HttpRepo
     *
     * @return true/false
     */
    private boolean isSmartRepo(RepoResource repoResource) {
        return isSmartRepo(repoResource.getRepoPath().getRepoKey());
    }

    /**
     * Signifies if the {@param repoKey} is a smart repo.
     */
    public boolean isSmartRepo(String repoKey) {
        repoKey = StringUtils.replaceLast(repoKey, "-cache", "");
        HttpRepoDescriptor descriptor = (HttpRepoDescriptor) repositoryService.remoteRepoDescriptorByKey(repoKey);
        return descriptor != null && descriptor.getContentSynchronisation() != null
                && descriptor.getContentSynchronisation().isEnabled();
    }

    /**
     * Registers resource as sourceDeleted
     *
     * @param resource
     */
    private void registerRemoteDeletedResource(RepoResource resource) {
        log.debug("Registering '{}' as remoteDeleted", resource);
        PropertiesAddon propertiesAddon = getPropertiesAddon();
        Properties properties = propertiesAddon.getProperties(resource.getRepoPath());

        if (properties.get(REMOTE_DELETED) == null
                || properties.get(REMOTE_DELETED).size() == 0) {
            synchronized (properties) {
                properties.put(REMOTE_DELETED, REMOTE_DELETED_MESSAGE);
                propertiesAddon.setProperties(resource.getRepoPath(), properties);
            }
        }
    }

    /**
     * Removes markup of resource as sourceDeleted=true
     *
     * @param resource
     */
    private void unRegisterRemoteDeletedResource(RepoResource resource) {
        log.debug("Unregistering '{}' as remoteDeleted", resource);
        PropertiesAddon propertiesAddon = getPropertiesAddon();
        Properties properties = propertiesAddon.getProperties(resource.getRepoPath());

        if (properties.get(REMOTE_DELETED) != null
                && properties.get(REMOTE_DELETED).size() >= 1) {
            synchronized (properties) {
                Iterator<Map.Entry<String, String>> iterator = properties.entries().iterator();
                while(iterator.hasNext()) {
                    if(iterator.next().getKey().equals(REMOTE_DELETED))
                        iterator.remove();
                }
                propertiesAddon.setProperties(resource.getRepoPath(), properties);
            }
        }
    }

    /**
     * Performs and API call to check if resource available remotely
     *
     * @param resource
     *
     * @return true if resource available or false
     */
    private boolean isArtifactAvailableRemotely(RepoResource resource) {
        String repoKey = StringUtils.replaceLast(resource.getRepoPath().getRepoKey(), "-cache", "");
        HttpRepo repo = (HttpRepo)repositoryService.remoteRepositoryByKey(repoKey);
        String targetArtifact = repo.getUrl() + "/" + resource.getRepoPath().getPath();
        HttpHead getMethod = new HttpHead(HttpUtils.encodeQuery(targetArtifact));
        CloseableHttpResponse getResponse = null;

        log.debug("Checking if '{}' available remotely", resource);
        try {
            getResponse = repo.executeMethod(getMethod);
            if(HttpStatus.SC_NOT_FOUND == getResponse.getStatusLine().getStatusCode()) {
                return false;
            }
        } catch (IOException e) {
            log.error("Cannot fetch metadata for remote artifact: " + resource, e);
            return false;
        } finally {
            IOUtils.closeQuietly(getResponse);
        }
        return true;
    }

    /**
     * Updates properties from remote repository (if expired)
     * and artifact sourceDeleted=true status when source
     * file is no longer available
     *
     * @param resource an resource to update properties for
     *
     * @return false if any error occurred or true
     */
    @Override
    public boolean updateProperties(RepoResource resource) {
        boolean status = false;
        if (isSmartRepo(resource)) {
            log.debug("Updating properties for '{}'", resource);

            if (resource instanceof UnfoundRepoResource) {
                return status;
            }

            if (isSyncPropertiesEnabled(resource)) {
                status = doSyncProperties(resource);
            } else {
                status = doSyncSourceExistence(resource);
            }
        }
        return status;
    }

    /**
     * Checks & Updates (sourceDeleted=true|false)
     * obtainable via remote properties, if current state
     * is expired
     *
     * this is actually an alternative for properties
     * synchronization and re-use same expiry mechanism
     *
     * @param resource
     *
     * @return true if remote deleted state verified otherwise false
     */
    private boolean doSyncSourceExistence(RepoResource resource) {
        if(isOriginAbsenceDetectionEnabled(resource)) {
            String repoKey = StringUtils.replaceLast(resource.getRepoPath().getRepoKey(), "-cache", "");
            HttpRepo repo = (HttpRepo)repositoryService.remoteRepositoryByKey(repoKey);

            if (repo != null && isPropertiesExpired(resource, repo) ) {
                updateRemoteDeleted(resource);
                return true;
            }
        }
        return false;
    }

    /**
     * Updates properties from remote repository (if expired)
     *
     * @param resource an resource to update properties for
     *
     * @return false if any error occurred or true
     */
    private boolean doSyncProperties(RepoResource resource) {
        try {
            String repoKey = StringUtils.replaceLast(resource.getRepoPath().getRepoKey(), "-cache", "");
            HttpRepoDescriptor descriptor = (HttpRepoDescriptor) repositoryService.remoteRepoDescriptorByKey(repoKey);
            HttpRepo repo = (HttpRepo)repositoryService.remoteRepositoryByKey(repoKey);

            if (!shouldSyncProperties(descriptor, repo, resource)) {
                return true;
            }
            boolean status = pullAndUpdateProperties(resource, repo);
            updateRemoteDeleted(resource);
            return status;
        }catch (Exception e){
            log.error("Smart repo failed to get properties from remote repo :" + resource.getRepoPath(), e);
            return false;
        }
    }

    /**
     * Checks whether current state is applicable for the properties update
     *
     * @param descriptor
     * @param repo
     * @param resource
     *
     * @return success/failure
     */
    private boolean shouldSyncProperties(HttpRepoDescriptor descriptor, HttpRepo repo, RepoResource resource) {
        if (descriptor == null || repo == null) {
            log.debug("Not performing content synchronization due to missing HttpRepoDescriptor/HttpRepo in context," +
                    " for instance, this may happen if repoKey is incorrect");
            return false;
        }

        if (descriptor.getContentSynchronisation() == null) {
            log.debug("Not performing content synchronization due to missing ContentSynchronisation config");
            return false;
        }

        if (!descriptor.getContentSynchronisation().isEnabled() ||
                !descriptor.getContentSynchronisation().getProperties().isEnabled()) {
            log.debug("Not performing content synchronization due to disabled ContentSynchronisation");
            return false;
        }
        // If file doesn't exist then do not update properties since it will be updated during file download
        if (!repositoryService.exists(resource.getRepoPath())) {
            log.debug("Not performing content synchronization due to absence of resource {}", resource);
            return false;
        }
        if (resourceIsExpirable(resource,repo)) {
            log.debug("Not performing content synchronization cause resource is expirable and " +
                    "properties will be fetched along with the artifact (if enabled)");
            return false;
        }
        if (!isPropertiesExpired(resource, repo) ) {
            log.debug("Not performing content synchronization cause properties did not expired yet");
            return false;
        }
        return true;
    }

    /**
     * Fetches remote properties
     *
     * @param resource - repo resource
     *
     * @return {@link Properties}
     */
    private Properties fetchRemoteProperties(RepoResource resource) {
        log.debug("Downloading remote properties for artifact {}", resource);

        CloseableHttpResponse getResponse = null;
        InputStream stream = null;
        Properties properties = null;

        String repoKey = StringUtils.replaceLast(resource.getRepoPath().getRepoKey(), "-cache", "");
        HttpRepo repo = (HttpRepo)repositoryService.remoteRepositoryByKey(repoKey);

        if (repo != null) {
            boolean ok, notFound;
            String targetArtifact = buildPropertiesRestApiUrl(repo, resource);
            HttpGet getMethod = new HttpGet(HttpUtils.encodeQuery(targetArtifact));
            try {
                getResponse = repo.executeMethod(getMethod);
                ok = HttpStatus.SC_OK == getResponse.getStatusLine().getStatusCode();
                notFound = HttpStatus.SC_NOT_FOUND == getResponse.getStatusLine().getStatusCode();
                if (ok || notFound) {
                    stream = getResponse.getEntity().getContent();
                    properties = (Properties) InfoFactoryHolder.get().createProperties();
                    if (ok && stream != null) {
                        ItemProperties itemProperties = JacksonReader.streamAsClass(stream, ItemProperties.class);
                        RepoRequests.logToContext("Received remote property content '{}'", itemProperties);
                        for (Map.Entry<String, String[]> entry : itemProperties.properties.entrySet()) {
                            String[] values = entry.getValue();
                            RepoRequests.logToContext("Found remote property key '{}' with values '%s'", entry.getKey(), values);
                            if (!entry.getKey().startsWith(ReplicationAddon.PROP_REPLICATION_PREFIX)) {
                                properties.putAll(entry.getKey(), values);
                            }
                        }
                    }
                }
                repositoryService.unexpireIfExists(repo.getLocalCacheRepo(),resource.getRepoPath().getPath());
            } catch (IOException e) {
                log.debug("Cannot fetch remote properties", e);
            } finally {
                IOUtils.closeQuietly(stream);
                IOUtils.closeQuietly(getResponse);
            }
        } else {
            log.debug("Cannot determinate the HttpRepo according to repoKey {}", repoKey);
        }
        return properties;
    }

    /**
     * Combines url to fetch properties
     *
     * @param httpRepo
     * @param resource
     *
     * @return url
     */
    private String buildPropertiesRestApiUrl(HttpRepo httpRepo, RepoResource resource) {
        URI uri = URI.create(httpRepo.getUrl());
        if (uri != null) {
            String remoteRepo = PathUtils.getLastPathElement(httpRepo.getUrl());

            return new StringBuilder()
                    .append(uri.getScheme())
                    .append("://")
                    .append(uri.getHost())
                    .append(uri.getPort() != -1 ?
                                    ":" + uri.getPort()
                                    :
                                    ""
                    )
                    .append(uri.getPath().startsWith(ARTIFACTORY_APP_PATH) ?
                                    REMOTE_STORAGE_ARTIFACTORY_API_PATH
                                    :
                                    REMOTE_STORAGE_API_PATH
                    )
                    .append(remoteRepo)
                    .append("/")
                    .append(resource.getRepoPath().getPath())
                    .append(PROPERTIES)
                    .toString();

        } else {
            throw new IllegalArgumentException(
                    "HttpRepo \""+ httpRepo.getKey() + "\"  target URL \"" + httpRepo.getUrl() +
                            "\" seems to be malformed, constructing request URI has failed"
            );
        }
    }

    /**
     * Performs local properties update with remote content
     *
     * @param resource
     * @param repo
     *
     * @return false if any error occurred or true
     */
    private boolean pullAndUpdateProperties(RepoResource resource, HttpRepo repo) {
        log.debug("Downloading remote properties for artifact {}", resource);

        boolean ok, notFound;
        InputStream stream = null;
        CloseableHttpResponse getResponse = null;

        String targetArtifact = buildPropertiesRestApiUrl(repo, resource);
        HttpGet getMethod = new HttpGet(HttpUtils.encodeQuery(targetArtifact));

        try {
            getResponse = repo.executeMethod(getMethod);
            ok = HttpStatus.SC_OK == getResponse.getStatusLine().getStatusCode();
            notFound = HttpStatus.SC_NOT_FOUND == getResponse.getStatusLine().getStatusCode();
            if (ok || notFound) {
                stream = getResponse.getEntity().getContent();
                Properties properties = (Properties) InfoFactoryHolder.get().createProperties();
                if (ok && stream != null) {
                    ItemProperties itemProperties = JacksonReader.streamAsClass(stream, ItemProperties.class);
                    RepoRequests.logToContext("Received remote property content '{}'", itemProperties);
                    for (Map.Entry<String, String[]> entry : itemProperties.properties.entrySet()) {
                        String[] values = entry.getValue();
                        RepoRequests.logToContext("Found remote property key '{}' with values '%s'", entry.getKey(), values);
                        if (!entry.getKey().startsWith(ReplicationAddon.PROP_REPLICATION_PREFIX)) {
                            properties.putAll(entry.getKey(), values);
                        }
                    }
                }
                setProperties(resource, properties);
            }
            repositoryService.unexpireIfExists(repo.getLocalCacheRepo(),resource.getRepoPath().getPath());
        } catch (Exception e) {
            log.debug("Cannot update remote properties: {}", e);
            return false;
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(getResponse);
        }
        return notFound ? false : ok;
    }

    private boolean resourceIsExpirable(RepoResource resource, HttpRepo repo) {
        // If the file is expirable then the expirable mechanism is responsible to update the file and the properties
        String path = resource.getRepoPath().getPath();
        CacheExpiry cacheExpiry = ContextHelper.get().beanForType(CacheExpiry.class);
        return cacheExpiry.isExpirable(repo.getLocalCacheRepo(), path);
    }

    /**
     * Updates local Properties with remote Properties content
     *
     * @param resource   - repo resource
     * @param properties - properties
     */
    private void setProperties(RepoResource resource, Properties properties) {
        log.debug("Setting properties '{}' for resource '{}'", properties, resource);
        if (properties.size() == 0 && !isArtifactAvailableRemotely(resource))
            return;
        PropertiesAddon propertiesAddon = getPropertiesAddon();
        propertiesAddon.setProperties(resource.getRepoPath(), properties);
    }

    /**
     * Checks whether given resource's properties cache has expired
     *
     * @param resource - repo resource
     * @param repo - http repo
     *
     * @return yes/no
     */
    private boolean isPropertiesExpired(RepoResource resource, HttpRepo repo) {
        RepoPath repoPath = resource.getRepoPath();
        long lastUpdated = repositoryService.getFileInfo(repoPath).getLastUpdated();
        long cacheAge = System.currentTimeMillis() - lastUpdated;
        long retrievalCachePeriodMillis = repo.getRetrievalCachePeriodSecs() * 1000L;
        // If cache age is less than retrieval cache period then do not update properties.
        if (cacheAge < retrievalCachePeriodMillis) {
            return false;
        }
        return true;
    }
}
