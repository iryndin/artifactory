package org.artifactory.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFilter;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.TypeArtifactFilter;
import org.apache.maven.embedder.ContainerCustomizer;
import org.apache.maven.embedder.DefaultMavenEmbedRequest;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.resource.ArtifactResource;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class MavenWrapper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(MavenWrapper.class);

    private CentralConfig cc;
    private PlexusContainer container;
    private MavenEmbedder maven;


    public MavenWrapper(CentralConfig cc) {
        this.cc = cc;
        maven = MavenUtil.createMavenEmbedder();
        DefaultMavenEmbedRequest req = new DefaultMavenEmbedRequest();
        req.setConfigurationCustomizer(new ContainerCustomizer() {
            public void customize(PlexusContainer container) {
                try {
                    MavenWrapper.this.container = container;
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to run ConfigurationCustomizer.", e);
                }
            }
        });
        try {
            maven.start(req);
            container.lookup(ArtifactDeployer.ROLE);
            //Register artifactMetadataSource needed for resolution
            ComponentDescriptor artifactMetadataSourceDesc = new ComponentDescriptor();
            artifactMetadataSourceDesc.setRole(ArtifactMetadataSource.ROLE);
            String name = MavenMetadataSource.class.getName();
            artifactMetadataSourceDesc.setImplementation(name);
            artifactMetadataSourceDesc.setRoleHint(MavenMetadataSource.ROLE_HINT);
            artifactMetadataSourceDesc.setIsolatedRealm(true);
            container.addComponentDescriptor(artifactMetadataSourceDesc);
            container.createAndAutowire(name);
            //container.createComponentInstance(artifactMetadataSourceDesc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start maven embedder.", e);
        }
    }

    public ArtifactResolutionResult resolve(Artifact artifact, Repo artifactRepo,
            List<? extends Repo> otherRepos) {
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
            //Deploy to a temp local repo
            ArtifactRepository artifactRepo = createDeploymentRepository(repo);
            //Do the actual deployment
            ArtifactDeployer deployer = getDeployer();
            deployer.deploy(file, artifact, artifactRepo, artifactRepo);
            //Import the deployment to the local repository
            repoDir = new File(artifactRepo.getBasedir());
            //Cleanup local repository metadata files
            Collection localRepoFiles =
                    FileUtils.listFiles(repoDir, new WildcardFilter("maven-metadata-*.xml"),
                            TrueFileFilter.INSTANCE);
            for (Object localRepoFile1 : localRepoFiles) {
                File localRepoFile = (File) localRepoFile1;
                localRepoFile.delete();
            }
            repo.importFolder(repoDir);
        } catch (Exception e) {
            throw new ArtifactDeploymentException("Failed to deploy '" + file + "'.", e);
        } finally {
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

    private List<ArtifactRepository> createRepositories(List<? extends Repo> repos)
            throws MalformedURLException {
        ArrayList<ArtifactRepository> list = new ArrayList<ArtifactRepository>(repos.size());
        for (Repo repo : repos) {
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
        artifact.setRelease(!MavenUtil.isVersionSnapshot(version));
        return artifact;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private ArtifactRepository createRepository(Repo repo) {
        ArtifactRepository repos = maven.createRepository(null/*repo.getUrl()*/, repo.getKey());
        return repos;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private ArtifactRepository createDeploymentRepository(LocalRepo repo) throws IOException {
        ArtifactRepositoryFactory repositoryFactory = getArtifactRepositoryFactory();
        File dir = cc.createTempDeploymentDir(repo);
        String url = dir.toURI().toURL().toString();
        ArtifactRepository repos = repositoryFactory.createDeploymentArtifactRepository(
                repo.getKey(), url, getArtifactRepositoryLayout(),
                repo.isUseSnapshotUniqueVersions());
        return repos;
    }

    private Object lookup(String key) {
        try {
            return container.lookup(key);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to find plexus component '" + key + "'.");
        }
    }
}
