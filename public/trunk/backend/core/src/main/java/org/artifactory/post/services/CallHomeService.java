/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
*
* Artifactory is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Artifactory is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.artifactory.post.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.OssAddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.api.callhome.CallHomeRequest;
import org.artifactory.api.callhome.FeatureGroup;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.jackson.JacksonWriter;
import org.artifactory.api.repo.Async;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.schedule.TaskService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.artifactory.common.ConstantValues.artifactoryVersion;

/**
 * @author Michael Pasternak
 */
@Service
public class CallHomeService {

    private static final Logger log = LoggerFactory.getLogger(CallHomeService.class);
    private static final String PARAM_OS_ARCH = "os.arch";
    private static final String PARAM_OS_NAME = "os.name";
    private static final String PARAM_JAVA_VERSION = "java.version";

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private TaskService taskService;

    /**
     * Sends post message with usage info to bintray
     */
    @Async
    public void callHome() {
        if (ConstantValues.versionQueryEnabled.getBoolean() && !configService.getDescriptor().isOfflineMode()) {
            try (CloseableHttpClient client = createHttpClient()) {
                String url = ConstantValues.bintrayApiUrl.getString() + "/products/jfrog/artifactory/stats/usage";
                HttpPost postMethod = new HttpPost(url);
                postMethod.setEntity(callHomeEntity());
                log.debug("Calling home...");
                client.execute(postMethod);
            } catch (Exception e) {
                log.debug("Failed calling home: " + e.getMessage(), e);
            }
        }
    }

    /**
     * @return {@link CloseableHttpClient}
     */
    private CloseableHttpClient createHttpClient() {
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        return new HttpClientConfigurator()
                .soTimeout(15000)
                .connectionTimeout(1500)
                .retry(0, false)
                .proxy(proxy)
                .getClient();
    }

    /**
     * Produces callHomeEntity
     *
     * @return {@link HttpEntity}
     *
     * @throws IOException on serialization errors
     */
    private HttpEntity callHomeEntity() throws IOException {
        CallHomeRequest request = new CallHomeRequest();
        request.version = artifactoryVersion.getString();
        request.licenseType = getLicenseType();
        request.licenseOEM = addonsManager.isPartnerLicense() ? "VMware" : null;
        Date licenseValidUntil = addonsManager.getLicenseValidUntil();
        if (licenseValidUntil != null) {
            request.licenseExpiration = ISODateTimeFormat.dateTime().print(new DateTime(licenseValidUntil));
        }
        request.setDist(System.getProperty("artdist"));
        request.environment.hostId = addonsManager.addonByType(HaCommonAddon.class).getHostId();
        request.environment.licenseHash = addonsManager.getLicenseKeyHash();
        request.environment.attributes.osName = System.getProperty(PARAM_OS_NAME);
        request.environment.attributes.osArch = System.getProperty(PARAM_OS_ARCH);
        request.environment.attributes.javaVersion = System.getProperty(PARAM_JAVA_VERSION);

        addFeatures(request);

        return serializeToStringEntity(request);
    }

    /**
     * Serializes {@link CallHomeRequest} to {@link org.apache.http.entity.StringEntity}
     *
     * @param request {@link CallHomeRequest}
     *
     * @return {@link org.apache.http.entity.StringEntity}
     * @throws IOException happens if serialization fails
     */
    private StringEntity serializeToStringEntity(CallHomeRequest request) throws IOException {
        String serialized = JacksonWriter.serialize(request, true);
        return new StringEntity(serialized, ContentType.APPLICATION_JSON);
    }

    /**
     * Collects features metadata  {@see RTFACT-8412}
     *
     * @param request
     */
    private void addFeatures(CallHomeRequest request) {
        FeatureGroup featureGroups = new FeatureGroup();

        FeatureGroup repositoriesFeature = new FeatureGroup("repositories");
        FeatureGroup localRepositoriesFeature = new FeatureGroup("local repositories");
        FeatureGroup remoteRepositoriesFeature = new FeatureGroup("remote repositories");


        List<RealRepo> localAndRemoteRepositories = repoService.getLocalAndRemoteRepositories();
        localAndRemoteRepositories.stream().forEach(rr -> {
            if(rr.isLocal()) {
                addLocalRepoFeatures(localRepositoriesFeature, rr);

            } else {
                addRemoteRepoFeatures(remoteRepositoriesFeature, rr);

            }
        });

        long localCount = localAndRemoteRepositories.parallelStream().filter(r -> r.isLocal()).count();
        localRepositoriesFeature.addFeatureAttribute("number_of_repositories", localCount);
        remoteRepositoriesFeature.addFeatureAttribute("number_of_repositories", localAndRemoteRepositories.size()-localCount);

        repositoriesFeature.addFeature(localRepositoriesFeature);
        repositoriesFeature.addFeature(remoteRepositoriesFeature);

        // virtual repos
        FeatureGroup virtualRepositoriesFeature = new FeatureGroup("virtual repositories");
        List<VirtualRepo> virtualRepositories = repoService.getVirtualRepositories();
        virtualRepositoriesFeature.addFeatureAttribute("number_of_repositories", getVirtualReposSize(virtualRepositories));
        addVirtualRepoFeatures(virtualRepositoriesFeature, virtualRepositories);
        repositoriesFeature.addFeature(virtualRepositoriesFeature);


        featureGroups.addFeature(repositoriesFeature);
        request.addCallHomeFeature(repositoriesFeature);
    }

    /**
     * Calculates sizeof List<VirtualRepo>
     *
     * @param virtualRepositories
     * @return size
     */
    private int getVirtualReposSize(List<VirtualRepo> virtualRepositories) {
        return virtualRepositories.stream().filter(vr -> vr.getKey().equals("repo")).count() == 1 ?
            virtualRepositories.size() -1 : virtualRepositories.size();
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     *
     * @param virtualRepositoriesFeature
     * @param virtualRepositories
     */
    private void addVirtualRepoFeatures(FeatureGroup virtualRepositoriesFeature,
            List<VirtualRepo> virtualRepositories) {
        virtualRepositories.stream()
                .filter(vr -> !vr.getKey().equals("repo"))
                .forEach(vr -> {
                    virtualRepositoriesFeature.addFeature(new FeatureGroup(vr.getKey()) {{
                        addFeatureAttribute("number_of_included_repositories",
                                vr.getResolvedLocalRepos().size() + vr.getResolvedRemoteRepos().size());
                        addFeatureAttribute("package_type", vr.getDescriptor().getType().name());
                        if (vr.getDescriptor().getRepoLayout() != null) {
                            addFeatureAttribute("repository_layout", vr.getDescriptor().getRepoLayout().getName());
                        }
                        if (vr.getDescriptor().getDefaultDeploymentRepo() != null)
                            addFeatureAttribute("configured_local_deployment",
                                    vr.getDescriptor().getDefaultDeploymentRepo().getKey());
                    }});
                });
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     *
     * @param remoteRepositoriesFeature
     * @param remoteRepo
     */
    private void addRemoteRepoFeatures(FeatureGroup remoteRepositoriesFeature, final RealRepo remoteRepo) {
        // remote repos
        remoteRepositoriesFeature.addFeature(new FeatureGroup(remoteRepo.getKey()) {{
            addFeatureAttribute("package_type", remoteRepo.getDescriptor().getType().name());
            addFeatureAttribute("repository_layout",
                    remoteRepo.getDescriptor().getRepoLayout().getName());

            RemoteReplicationDescriptor remoteReplicationDescriptor =
                    configService.getDescriptor().getRemoteReplication(remoteRepo.getKey());

            if (remoteReplicationDescriptor != null) {
                addFeatureAttribute("pull_replication", remoteReplicationDescriptor.isEnabled());
                if(remoteReplicationDescriptor.isEnabled()) {
                    addFeatureAttribute("pull_replication_url",
                            ((RemoteRepoDescriptor)remoteRepo.getDescriptor()).getUrl());
                }
            } else {
                addFeatureAttribute("pull_replication", false);
            }
        }});
    }

    /**
     * Collects virtual repo metadata  {@see RTFACT-8412}
     *
     * @param localRepositoriesFeature
     * @param localRepo
     */
    private void addLocalRepoFeatures(FeatureGroup localRepositoriesFeature, final RealRepo localRepo) {
        // local repos
        localRepositoriesFeature.addFeature(new FeatureGroup(localRepo.getKey()) {{
            addFeatureAttribute("package_type", localRepo.getDescriptor().getType().name());
            addFeatureAttribute("repository_layout",
                    localRepo.getDescriptor().getRepoLayout().getName());

            LocalReplicationDescriptor localReplication =
                    configService.getDescriptor().getEnabledLocalReplication(localRepo.getKey());

            if (localReplication != null && localReplication.isEnabled()) {
                List<LocalReplicationDescriptor> repls =
                        configService.getDescriptor().getMultiLocalReplications(localRepo.getKey());
                addFeatureAttribute("push_replication", (repls == null || repls.size() == 0 ? false :
                        repls.size() > 1 ? "multi" : "true"));
                addFeatureAttribute("event_replication", localReplication.isEnableEventReplication());
                addFeatureAttribute("sync_properties", localReplication.isSyncProperties());
                addFeatureAttribute("sync_deleted", localReplication.isSyncDeletes());
            } else if (localReplication == null) {
                addFeatureAttribute("push_replication", false);
                addFeatureAttribute("event_replication", false);
                addFeatureAttribute("sync_deleted", null);
            }
        }});
    }

    private String getLicenseType() {
        if (addonsManager instanceof OssAddonsManager) {
            return "oss";
        }
        if (addonsManager.addonByType(CoreAddons.class).isAol()) {
            return "aol";
        } else if (addonsManager.getLicenseDetails()[2].equals("Trial")) {
            return "trial";
        } else if (addonsManager.getLicenseDetails()[2].equals("Commercial")) {
            return "pro";
        } else if (addonsManager.isHaLicensed()) {
            return "ent";
        }
        return null;
    }
}
