/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.TypeArtifactFilter;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.wagon.Wagon;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.util.FileUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.springframework.security.util.FieldUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Service
public class Maven {
    private static final Logger log = LoggerFactory.getLogger(Maven.class);

    public static final File LOCAL_REPO_DIR = new File(System.getProperty("java.io.tmpdir"), "artifactory-local-repo");

    private PlexusContainer container;
    private MavenEmbedder maven;

    public Maven() {
        if (!LOCAL_REPO_DIR.exists() && !LOCAL_REPO_DIR.mkdirs()) {
            throw new RuntimeException("Failed to create dummy local repository dir '" +
                    LOCAL_REPO_DIR.getPath() + "'.");
        }
        maven = new MavenEmbedder();
        maven.setLogger(new MavenEmbedderConsoleLogger());
        /*MavenEmbedderConfiguration req = new DefaultMavenEmbedderConfiguration();
        req.setConfigurationCustomizer(new ContainerCustomizer() {
            public void customize(PlexusContainer container) {
                try {
                    Maven.this.container = container;
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to run ConfigurationCustomizer.", e);
                }
            }
        });*/
        try {
            //Setup the container
            /*Field field = maven.getClass().getDeclaredField("container");
            field.setAccessible(true);
            container = (PlexusContainer) field.get(maven);*/
            maven.setClassLoader(Thread.currentThread().getContextClassLoader());
            maven.setLocalRepositoryDirectory(LOCAL_REPO_DIR);
            maven.start();
            container = maven.getEmbedder().getContainer();
            //Touch the deployer
            lookup(ArtifactDeployer.ROLE);
            //Register the jcr wagon
            ComponentDescriptor jcrWagonDesc = new ComponentDescriptor();
            jcrWagonDesc.setRole(Wagon.ROLE);
            String jcrWagonName = JcrWagon.class.getName();
            jcrWagonDesc.setImplementation(jcrWagonName);
            jcrWagonDesc.setRoleHint("jcr");
            jcrWagonDesc.setIsolatedRealm(true);
            container.addComponentDescriptor(jcrWagonDesc);
            //container.createAndAutowire(jcrWagonName);
            container.createComponentInstance(jcrWagonDesc);
            //Register artifactMetadataSource needed for resolution
            ComponentDescriptor artifactMetadataSourceDesc = new ComponentDescriptor();
            artifactMetadataSourceDesc.setRole(ArtifactMetadataSource.ROLE);
            String mavenMetadataSourceName = MavenMetadataSource.class.getName();
            artifactMetadataSourceDesc.setImplementation(mavenMetadataSourceName);
            artifactMetadataSourceDesc.setRoleHint(MavenMetadataSource.ROLE_HINT);
            artifactMetadataSourceDesc.setIsolatedRealm(true);
            container.addComponentDescriptor(artifactMetadataSourceDesc);
            //container.createAndAutowire(mavenMetadataSourceName);
            container.createComponentInstance(artifactMetadataSourceDesc);
            manipulateComponentLoggers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start maven embedder.", e);
        }
    }

    public ArtifactResolutionResult resolve(Artifact artifact, RealRepo artifactRepo,
            List<? extends RealRepo> otherRepos) {
        ArtifactResolutionResult artifactResolutionResult;
        try {
            ArtifactRepository localRepository = createRepository(artifactRepo);
            List<ArtifactRepository> remoteRepos = createRepositories(otherRepos);
            ArtifactMetadataSource source = getArtifactMetadataSource();
            ResolutionGroup resolutionGroup = source.retrieve(artifact, localRepository, remoteRepos);
            ArtifactCollector collector = getArtifactCollector();
            TypeArtifactFilter filter = new TypeArtifactFilter(artifact.getType());
            artifactResolutionResult = collector.collect(
                    resolutionGroup.getArtifacts(), artifact, localRepository, remoteRepos,
                    source, filter, Collections.emptyList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve artifact '" + artifact + "'.", e);
        }
        return artifactResolutionResult;
    }

    public void deploy(File file, Artifact artifact, final LocalRepo repo, File tempUploadsDir)
            throws ArtifactDeploymentException {
        File repoDir = null;
        try {
            //Deploy to a jcr repo
            ArtifactRepositoryFactory repositoryFactory = getArtifactRepositoryFactory();
            File dir = org.artifactory.util.FileUtils.createRandomDir(tempUploadsDir, repo.getKey() + ".");
            String tempDirUrl = FileUtils.getDecodedFileUrl(dir);
            boolean uniqueSnapshotVersions = repo.getSnapshotVersionBehavior().equals(SnapshotVersionBehavior.UNIQUE);
            ArtifactRepository localRepository = repositoryFactory.createDeploymentArtifactRepository(
                    repo.getKey(), tempDirUrl, getArtifactRepositoryLayout(), uniqueSnapshotVersions);
            repoDir = new File(localRepository.getBasedir());
            String url = "jcr:";
            ArtifactRepository deploymentRepository = repositoryFactory.createDeploymentArtifactRepository(
                    repo.getKey(), url, getArtifactRepositoryLayout(), uniqueSnapshotVersions);
            //Do the actual deployment
            ArtifactDeployer deployer = getDeployer();
            deployer.deploy(file, artifact, deploymentRepository, localRepository);
        } catch (Exception e) {
            throw new ArtifactDeploymentException(e);
        } finally {
            //Cleanup local repository
            if (repoDir != null) {
                try {
                    org.apache.commons.io.FileUtils.forceDelete(repoDir);
                } catch (IOException e) {
                    log.warn("Failed to delete temp repo directory'{}'.", repoDir.getPath());
                }
            }
        }
    }

    public PlexusContainer getContainer() {
        return container;
    }

    private List<ArtifactRepository> createRepositories(List<? extends RealRepo> repos)
            throws MalformedURLException {
        ArrayList<ArtifactRepository> list = new ArrayList<ArtifactRepository>(repos.size());
        for (RealRepo repo : repos) {
            ArtifactRepository artifactRepository = createRepository(repo);
            list.add(artifactRepository);
        }
        return list;
    }

    public MavenEmbedder getMaven() {
        return maven;
    }

    public ArtifactDeployer getDeployer() {
        return (ArtifactDeployer) lookup(ArtifactDeployer.ROLE);
    }

    public ArtifactRepositoryFactory getArtifactRepositoryFactory() {
        return (ArtifactRepositoryFactory) lookup(ArtifactRepositoryFactory.ROLE);
    }

    public ArtifactRepositoryLayout getArtifactRepositoryLayout() {
        return (ArtifactRepositoryLayout) lookup(ArtifactRepositoryLayout.ROLE);
    }

    public ArtifactMetadataSource getArtifactMetadataSource() {
        return (ArtifactMetadataSource) lookup(ArtifactMetadataSource.ROLE);
    }

    public WagonManager getWagonManager() {
        return (WagonManager) lookup(WagonManager.ROLE);
    }

    public ArtifactCollector getArtifactCollector() {
        return (ArtifactCollector) lookup(ArtifactCollector.class.getName());
    }

    public Artifact createArtifact(MavenArtifactInfo artifactInfo) {
        String groupId = artifactInfo.getGroupId();
        String artifactId = artifactInfo.getArtifactId();
        String version = artifactInfo.getVersion();
        String classifier = artifactInfo.getClassifier();
        String type = artifactInfo.getType();
        return createArtifact(groupId, artifactId, version, classifier, type);
    }

    public Artifact createArtifact(String groupId, String artifactId, String version, String classifier, String type) {
        Artifact artifact = maven.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);
        artifact.setRelease(!MavenNaming.isVersionSnapshot(version));
        return artifact;
    }

    @PreDestroy
    public void destroy() throws Exception {
        org.apache.commons.io.FileUtils.deleteDirectory(LOCAL_REPO_DIR);
    }

    private ArtifactRepository createRepository(RealRepo repo) {
        ArtifactRepository repos;
        try {
            repos = maven.createRepository(null/*repo.getUrl()*/, repo.getKey());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to create repository.", e);
        }
        return repos;
    }

    private Object lookup(String key) {
        try {
            /*ClassRealm classRealm = container.getContainerRealm();
            return container.lookup(key, classRealm);*/
            return container.lookup(key);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to find plexus component '" + key + "'.");
        }
    }

    private void manipulateComponentLoggers() {
        WagonManager wagonManager = getWagonManager();
        PlexusLoggerAdapter logger = (PlexusLoggerAdapter) FieldUtils.getProtectedFieldValue("logger", wagonManager);
        logger.setThreshold(org.codehaus.plexus.logging.Logger.LEVEL_ERROR);
    }
}