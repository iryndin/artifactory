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
 *
 * Based on: /org/apache/maven/maven-embedder/2.0.4/maven-embedder-2.0.4.jar!
 * /org/apache/maven/embedder/MavenEmbedder.class
 */

package org.artifactory.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.embedder.*;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.RuntimeInfo;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class intended to be used by clients who wish to embed Maven into their applications
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class MavenEmbedder {
    // ----------------------------------------------------------------------
    // Embedder
    // ----------------------------------------------------------------------
    private Embedder embedder;

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------
    private MavenProjectBuilder mavenProjectBuilder;
    private ArtifactRepositoryFactory artifactRepositoryFactory;
    private LifecycleExecutor lifecycleExecutor;
    private MavenXpp3Reader modelReader;
    private MavenXpp3Writer modelWriter;
    private ProfileManager profileManager;
    private PluginDescriptorBuilder pluginDescriptorBuilder;
    private ArtifactFactory artifactFactory;
    private ArtifactResolver artifactResolver;
    private ArtifactRepositoryLayout defaultArtifactRepositoryLayout;

    // ----------------------------------------------------------------------
    // Configuration
    // ----------------------------------------------------------------------
    private Settings settings;
    private ArtifactRepository localRepository;
    private File localRepositoryDirectory;
    private ClassLoader classLoader;
    private MavenEmbedderLogger logger;

    // ----------------------------------------------------------------------
    // User options
    // ----------------------------------------------------------------------
    // release plugin uses this but in IDE there will probably always be some form of interaction.
    private boolean interactiveMode;
    private boolean offline;
    private String globalChecksumPolicy;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void setInteractiveMode(boolean interactiveMode) {
        this.interactiveMode = interactiveMode;
    }

    public boolean isInteractiveMode() {
        return interactiveMode;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setGlobalChecksumPolicy(String globalChecksumPolicy) {
        this.globalChecksumPolicy = globalChecksumPolicy;
    }

    public String getGlobalChecksumPolicy() {
        return globalChecksumPolicy;
    }

    /**
     * Set the classloader to use with the maven embedder.
     *
     * @param classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setLocalRepositoryDirectory(File localRepositoryDirectory) {
        this.localRepositoryDirectory = localRepositoryDirectory;
    }

    public File getLocalRepositoryDirectory() {
        return localRepositoryDirectory;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public MavenEmbedderLogger getLogger() {
        return logger;
    }

    public void setLogger(MavenEmbedderLogger logger) {
        this.logger = logger;
    }

    public Embedder getEmbedder() {
        return embedder;
    }

    // ----------------------------------------------------------------------
    // Embedder Client Contract
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Model
    // ----------------------------------------------------------------------

    public Model readModel(File model) throws XmlPullParserException, IOException {
        return modelReader.read(new FileReader(model));
    }

    public void writeModel(Writer writer, Model model)
            throws IOException {
        modelWriter.write(writer, model);
    }

    // ----------------------------------------------------------------------
    // Project
    // ----------------------------------------------------------------------

    public MavenProject readProject(File mavenProject)
            throws ProjectBuildingException {
        return mavenProjectBuilder.build(mavenProject, localRepository, profileManager);
    }

    public MavenProject readProjectWithDependencies(File mavenProject,
                                                    TransferListener transferListener)
            throws ProjectBuildingException, ArtifactResolutionException,
            ArtifactNotFoundException {
        return mavenProjectBuilder.buildWithDependencies(mavenProject, localRepository,
                profileManager, transferListener);
    }

    public MavenProject readProjectWithDependencies(File mavenProject)
            throws ProjectBuildingException, ArtifactResolutionException,
            ArtifactNotFoundException {
        return mavenProjectBuilder
                .buildWithDependencies(mavenProject, localRepository, profileManager);
    }

    public List<MavenProject> collectProjects(File basedir, String[] includes, String[] excludes)
            throws MojoExecutionException {
        List<MavenProject> projects = new ArrayList<MavenProject>();
        List<File> poms = getPomFiles(basedir, includes, excludes);
        for (File pom : poms) {
            try {
                MavenProject p = readProject(pom);
                projects.add(p);
            }
            catch (ProjectBuildingException e) {
                throw new MojoExecutionException("Error loading " + pom, e);
            }
        }
        return projects;
    }

    // ----------------------------------------------------------------------
    // Artifacts
    // ----------------------------------------------------------------------

    public Artifact createArtifact(String groupId, String artifactId, String version, String scope,
                                   String type) {
        return artifactFactory.createArtifact(groupId, artifactId, version, scope, type);
    }

    public Artifact createArtifactWithClassifier(String groupId, String artifactId, String version,
                                                 String type, String classifier) {
        return artifactFactory
                .createArtifactWithClassifier(groupId, artifactId, version, type, classifier);
    }

    public void resolve(Artifact artifact, List remoteRepositories,
                        ArtifactRepository localRepository)
            throws ArtifactResolutionException, ArtifactNotFoundException {
        artifactResolver.resolve(artifact, remoteRepositories, localRepository);
    }

    // ----------------------------------------------------------------------
    // Plugins
    // ----------------------------------------------------------------------

    public static List getAvailablePlugins() {
        List<SummaryPluginDescriptor> plugins = new ArrayList<SummaryPluginDescriptor>();
        plugins.add(makeMockPlugin("org.apache.maven.plugins", "maven-jar-plugin",
                "Maven Jar Plug-in"));
        plugins.add(makeMockPlugin("org.apache.maven.plugins", "maven-compiler-plugin",
                "Maven Compiler Plug-in"));
        return plugins;
    }

    public PluginDescriptor getPluginDescriptor(SummaryPluginDescriptor summaryPluginDescriptor)
            throws MavenEmbedderException {
        PluginDescriptor pluginDescriptor;
        try {
            InputStream is = classLoader.getResourceAsStream(
                    "/plugins/" + summaryPluginDescriptor.getArtifactId() + ".xml");
            pluginDescriptor = pluginDescriptorBuilder.build(new InputStreamReader(is));
        }
        catch (PlexusConfigurationException e) {
            throw new MavenEmbedderException("Error retrieving plugin descriptor.", e);
        }
        return pluginDescriptor;
    }

    private static SummaryPluginDescriptor makeMockPlugin(
            String groupId, String artifactId, String name) {
        return new SummaryPluginDescriptor(groupId, artifactId, name);
    }

    public static final String DEFAULT_LOCAL_REPO_ID = "local";

    public static final String DEFAULT_LAYOUT_ID = "default";

    public ArtifactRepository createLocalRepository(File localRepository)
            throws ComponentLookupException {
        return createLocalRepository(localRepository.getAbsolutePath(), DEFAULT_LOCAL_REPO_ID);
    }

    public ArtifactRepository createLocalRepository(Settings settings)
            throws ComponentLookupException {
        return createLocalRepository(settings.getLocalRepository(), DEFAULT_LOCAL_REPO_ID);
    }

    public ArtifactRepository createLocalRepository(String url, String repositoryId)
            throws ComponentLookupException {
        if (!url.startsWith("file:")) {
            url = "file://" + url;
        }

        return createRepository(url, repositoryId);
    }

    public ArtifactRepository createRepository(String url, String repositoryId)
            throws ComponentLookupException {
        // snapshots vs releases
        // offline = to turning the update policy off

        //TODO: we'll need to allow finer grained creation of repositories but this will do for now

        String updatePolicyFlag = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;

        String checksumPolicyFlag = ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN;

        ArtifactRepositoryPolicy snapshotsPolicy =
                new ArtifactRepositoryPolicy(true, updatePolicyFlag, checksumPolicyFlag);

        ArtifactRepositoryPolicy releasesPolicy =
                new ArtifactRepositoryPolicy(true, updatePolicyFlag, checksumPolicyFlag);

        return artifactRepositoryFactory.createArtifactRepository(repositoryId, url,
                defaultArtifactRepositoryLayout, snapshotsPolicy, releasesPolicy);
    }

    // ----------------------------------------------------------------------
    // Internal utility code
    // ----------------------------------------------------------------------

    private static RuntimeInfo createRuntimeInfo(Settings settings) {
        RuntimeInfo runtimeInfo = new RuntimeInfo(settings);

        runtimeInfo.setPluginUpdateOverride(Boolean.FALSE);

        return runtimeInfo;
    }

    private static List<File> getPomFiles(File basedir, String[] includes, String[] excludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(basedir);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();

        List<File> poms = new ArrayList<File>();
        for (int i = 0; i < scanner.getIncludedFiles().length; i++) {
            poms.add(new File(basedir, scanner.getIncludedFiles()[i]));
        }

        return poms;
    }

    // ----------------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------------

    @SuppressWarnings({"deprecation"})
    public void start()
            throws MavenEmbedderException {
        // ----------------------------------------------------------------------
        // Set the maven.home system property which is need by components like
        // the plugin registry builder.
        // ----------------------------------------------------------------------

        if (classLoader == null) {
            throw new IllegalStateException("A classloader must be specified using setClassLoader(ClassLoader).");
        }

        embedder = new Embedder();

        if (logger != null) {
            embedder.setLoggerManager(
                    new MavenEmbedderLoggerManager(new PlexusLoggerAdapter(logger)));
        }

        try {
            ClassWorld classWorld = new ClassWorld();

            classWorld.newRealm("plexus.core", classLoader);

            embedder.start(classWorld);

            // ----------------------------------------------------------------------
            // Lookup each of the components we need to provide the desired
            // client interface.
            // ----------------------------------------------------------------------

            modelReader = new MavenXpp3Reader();

            modelWriter = new MavenXpp3Writer();

            pluginDescriptorBuilder = new PluginDescriptorBuilder();

            profileManager = new DefaultProfileManager(embedder.getContainer());

            mavenProjectBuilder = (MavenProjectBuilder) embedder.lookup(MavenProjectBuilder.ROLE);

            // ----------------------------------------------------------------------
            // Artifact related components
            // ----------------------------------------------------------------------

            artifactRepositoryFactory = (ArtifactRepositoryFactory) embedder.lookup(ArtifactRepositoryFactory.ROLE);

            artifactFactory = (ArtifactFactory) embedder.lookup(ArtifactFactory.ROLE);

            artifactResolver = (ArtifactResolver) embedder.lookup(ArtifactResolver.ROLE);

            defaultArtifactRepositoryLayout = (ArtifactRepositoryLayout) embedder
                    .lookup(ArtifactRepositoryLayout.ROLE, DEFAULT_LAYOUT_ID);

            lifecycleExecutor = (LifecycleExecutor) embedder.lookup(LifecycleExecutor.ROLE);

            createMavenSettings();

            profileManager.loadSettingsProfiles(settings);

            localRepository = createLocalRepository(settings);
        }
        catch (PlexusContainerException e) {
            throw new MavenEmbedderException("Cannot start Plexus embedder.", e);
        }
        catch (DuplicateRealmException e) {
            throw new MavenEmbedderException("Cannot create Classworld realm for the embedder.", e);
        }
        catch (ComponentLookupException e) {
            throw new MavenEmbedderException("Cannot lookup required component.", e);
        }
    }

    /**
     * Create the Settings that will be used with the embedder. If we are aligning with the user installation then we
     * lookup the standard settings builder and use that to create our settings. Otherwise we constructs a settings
     * object and populate the information ourselves.
     *
     * @throws org.apache.maven.embedder.MavenEmbedderException
     *
     * @throws org.codehaus.plexus.component.repository.exception.ComponentLookupException
     *
     */
    private void createMavenSettings()
            throws MavenEmbedderException, ComponentLookupException {
        if (localRepositoryDirectory == null) {
            throw new IllegalArgumentException(
                    "When not aligning with a user install you must specify a local repository location using the setLocalRepositoryDirectory( File ) method.");
        }
        settings = new Settings();
        settings.setLocalRepository(localRepositoryDirectory.getAbsolutePath());
        settings.setRuntimeInfo(createRuntimeInfo(settings));
        settings.setOffline(offline);
        settings.setInteractiveMode(interactiveMode);
    }

    // ----------------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------------

    public void stop()
            throws MavenEmbedderException {
        try {
            embedder.release(mavenProjectBuilder);
            embedder.release(artifactRepositoryFactory);
            embedder.release(lifecycleExecutor);
        }
        catch (ComponentLifecycleException e) {
            throw new MavenEmbedderException("Cannot stop the embedder.", e);
        }
    }
}