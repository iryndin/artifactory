/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.maven;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
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
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.wagon.Wagon;
import org.artifactory.ArtifactoryHome;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.SnapshotVersionBehavior;
import org.artifactory.resource.ArtifactResource;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.springframework.beans.factory.DisposableBean;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Maven implements DisposableBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Maven.class);

    public static final File LOCAL_REPO_DIR =
            new File(System.getProperty("java.io.tmpdir"), "artifactory-local-repo");
    public static final File USER_SETTINGS_FILE =
            new File(System.getProperty("java.io.tmpdir"), "artifactory-settings.xml");

    private static final Random intGenerator = new Random(System.currentTimeMillis());

    private PlexusContainer container;
    private MavenEmbedder maven;

    public Maven() {
        if (!LOCAL_REPO_DIR.exists() && !LOCAL_REPO_DIR.mkdirs()) {
            throw new RuntimeException("Failed to create dummy local repository dir '" +
                    LOCAL_REPO_DIR.getPath() + "'.");
        }
        maven = MavenUtils.createMavenEmbedder();
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
            //setLocalRepositoryDirectory() will create an offline maven, so we create a dummy user
            //settings dir. Anyway, setLocalRepositoryDirectory() has no effect if a user m2 exists.
            System.setProperty(MavenSettingsBuilder.ALT_LOCAL_REPOSITORY_LOCATION,
                    LOCAL_REPO_DIR.getAbsolutePath());
            System.setProperty(MavenSettingsBuilder.ALT_USER_SETTINGS_XML_LOCATION,
                    USER_SETTINGS_FILE.getAbsolutePath());
            maven.setAlignWithUserInstallation(true);
            maven.setLocalRepositoryDirectory(LOCAL_REPO_DIR);
            maven.start();
            Field field = maven.getClass().getDeclaredField("embedder");
            field.setAccessible(true);
            Embedder embedder = (Embedder) field.get(maven);
            container = embedder.getContainer();
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
            ResolutionGroup resolutionGroup =
                    source.retrieve(artifact, localRepository, remoteRepos);
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

    public void deploy(File file, Artifact artifact, final LocalRepo repo)
            throws ArtifactDeploymentException {
        File repoDir = null;
        try {
            //Deploy to a jcr repo
            ArtifactRepositoryFactory repositoryFactory = getArtifactRepositoryFactory();
            File dir = createTempDeploymentDir(repo);
            String tempDirUrl = dir.toURI().toURL().toString();
            boolean uniqueSnapshotVersions =
                    repo.getSnapshotVersionBehavior().equals(SnapshotVersionBehavior.unique);
            ArtifactRepository localRepository =
                    repositoryFactory.createDeploymentArtifactRepository(
                            repo.getKey(), tempDirUrl, getArtifactRepositoryLayout(),
                            uniqueSnapshotVersions);
            repoDir = new File(localRepository.getBasedir());
            String url = "jcr:";
            ArtifactRepository deploymentRepository =
                    repositoryFactory.createDeploymentArtifactRepository(
                            repo.getKey(), url, getArtifactRepositoryLayout(),
                            uniqueSnapshotVersions);
            //Do the actual deployment
            ArtifactDeployer deployer = getDeployer();
            deployer.deploy(file, artifact, deploymentRepository, localRepository);
        } catch (Exception e) {
            throw new ArtifactDeploymentException("Failed to deploy file '" + file + "'.", e);
        } finally {
            //Cleanup local repository
            if (repoDir != null) {
                try {
                    FileUtils.forceDelete(repoDir);
                } catch (IOException e) {
                    LOGGER.warn(
                            "Failed to delete temp repo directory'" + repoDir.getPath() + "'.", e);
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

    public Artifact createArtifact(ArtifactResource pa) {
        return createArtifact(pa.getGroupId(),
                pa.getArtifactId(),
                pa.getVersion(),
                pa.getClassifier(),
                pa.getType());
    }

    public Artifact createArtifact(
            String groupId, String artifactId, String version, String classifier, String type) {
        Artifact artifact =
                maven.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);
        artifact.setRelease(!MavenUtils.isVersionSnapshot(version));
        return artifact;
    }

    public void destroy() throws Exception {
        FileUtils.deleteDirectory(LOCAL_REPO_DIR);
    }

    /**
     * Create a unique dir for managing parallel deployment. TODO: Check who is deleting these!
     *
     * @param repo the current local repo
     * @return a unique temp directory
     */
    public static File createTempDeploymentDir(LocalRepo repo) {
        File dir = new File(ArtifactoryHome.getTmpDataDir(),
                repo.getKey() + "_" + intGenerator.nextInt());
        if (dir.exists()) {
            // Either we did not clean up or VERY unlucky
            throw new RuntimeException(
                    "Temp directory " + dir.getAbsolutePath() + " for deployment already exists!");
        }
        if (!dir.mkdirs()) {
            throw new RuntimeException("Failed to create deployment repository directory '" +
                    dir.getPath() + "'.");
        }
        return dir;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
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
}
