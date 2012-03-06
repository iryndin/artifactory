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

package org.artifactory.build;

import com.gc.iotools.stream.is.InputStreamFromOutputStream;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.addon.license.LicensesAddon;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.build.AfterBuildSaveAction;
import org.artifactory.addon.plugin.build.BeforeBuildSaveAction;
import org.artifactory.api.build.ImportableExportableBuild;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.cache.ChecksumPair;
import org.artifactory.build.cache.MissingChecksumCallable;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.factory.xstream.XStreamFactory;
import org.artifactory.fs.FileInfo;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.PathBuilder;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.data.BinaryContent;
import org.artifactory.sapi.data.MutableBinaryContent;
import org.artifactory.sapi.data.MutableVfsNode;
import org.artifactory.sapi.data.VfsDataService;
import org.artifactory.sapi.data.VfsNode;
import org.artifactory.sapi.data.VfsNodeType;
import org.artifactory.sapi.fs.VfsService;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonStreamContext;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.node.ObjectNode;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.release.PromotionStatus;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.artifactory.storage.StorageConstants.*;

/**
 * Build service main implementation
 *
 * @author Noam Y. Tenne
 */
@Service
@Reloadable(beanClass = InternalBuildService.class, initAfter = {JcrService.class})
public class BuildServiceImpl implements InternalBuildService {
    private static final Logger log = LoggerFactory.getLogger(BuildServiceImpl.class);

    private static final String EXPORTABLE_BUILD_VERSION = "v1";
    public static final String BUILDS_EXPORT_DIR = "builds";

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CachedThreadPoolTaskExecutor executor;

    @Autowired
    private VfsDataService vfsDataService;

    @Autowired
    private VfsService vfsService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SearchService searchService;

    @Autowired(required = false)
    private Builds builds;

    /**
     * Keep a cache for each type of checksums because we can get requests for different types of checksum for the same
     * item
     */
    private ConcurrentMap<String, FutureTask<ChecksumPair>> md5Cache;
    private ConcurrentMap<String, FutureTask<ChecksumPair>> sha1Cache;

    @Override
    public void init() {
        md5Cache = new MapMaker().softValues()
                .expireAfterWrite(ConstantValues.missingBuildChecksumCacheIdeTimeSecs.getLong(), TimeUnit.SECONDS)
                .makeMap();
        sha1Cache = new MapMaker().softValues()
                .expireAfterWrite(ConstantValues.missingBuildChecksumCacheIdeTimeSecs.getLong(), TimeUnit.SECONDS)
                .makeMap();
        //Create initial builds folder
        vfsService.createIfNeeded(getPathFactory().getBuildsRootPath());
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        //Nothing to reload
    }

    @Override
    public void destroy() {
        //Nothing to destroy
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @Override
    public void addBuild(final Build build) {
        String buildName = build.getName();
        String escapedBuildName = getPathFactory().escape(buildName);
        String buildNumber = build.getNumber();
        String started = build.getStarted();
        String escapedStarted = getPathFactory().escape(started);
        String currentUser = authorizationService.currentUsername();

        log.debug("Adding info for build '{}' #{}", buildName, buildNumber);

        build.setArtifactoryPrincipal(currentUser);

        populateMissingChecksums(build.getModules());

        Set<String> artifactChecksums = Sets.newHashSet();
        Set<String> dependencyChecksums = Sets.newHashSet();
        collectModuleChecksums(build.getModules(), artifactChecksums, dependencyChecksums);

        MutableVfsNode buildNumberNode = createAndGetNumberNode(escapedBuildName, buildNumber, started);
        MutableVfsNode buildStartedNode = buildNumberNode.getOrCreateSubNode(escapedStarted, VfsNodeType.UNSTRUCTURED);

        InputStreamFromOutputStream stream = null;
        try {
            stream = new InputStreamFromOutputStream() {

                @Override
                protected Object produce(OutputStream outputStream) throws Exception {
                    JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(outputStream);
                    jsonGenerator.writeObject(build);
                    return null;
                }
            };
            PluginsAddon pluginsAddon = addonsManager.addonByType(PluginsAddon.class);
            pluginsAddon.execPluginActions(BeforeBuildSaveAction.class, builds, new BuildContext() {
            });
            buildStartedNode.setContent(vfsDataService.createBinary(BuildRestConstants.MT_BUILD_INFO, stream));
            pluginsAddon.execPluginActions(AfterBuildSaveAction.class, builds, new BuildContext() {
            });
        } catch (Exception e) {
            String errorMessage = String.format("An error occurred while writing JSON data to the node of build name " +
                    "'%s', number '%s', started at '%s'.", buildName, buildNumber, started);
            throw new RuntimeException(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        try {
            saveBuildFileChecksums(buildStartedNode, artifactChecksums, dependencyChecksums);
        } catch (Exception e) {
            String errorMessage = String.format("An error occurred while saving checksum properties on the node of " +
                    "build name '%s', number '%s', started at '%s'.", buildName, buildNumber, started);
            throw new RuntimeException(errorMessage, e);
        }

        try {
            updateReleaseLastStatusProperty(build, buildStartedNode);
        } catch (RepositoryException e) {
            String errorMessage = String.format("An error occurred while updating the latest release status " +
                    "property of build name '%s', number '%s', started at '%s'.", buildName, buildNumber, started);
            throw new RuntimeException(errorMessage, e);
        }

        log.debug("Added info for build '{}' #{}", buildName, buildNumber);

        LicensesAddon licensesAddon = addonsManager.addonByType(LicensesAddon.class);
        licensesAddon.performOnBuildArtifacts(build);
    }

    @Override
    public Build getBuild(String buildName, String buildNumber, String buildStarted) {
        String buildPath = getBuildPathFromParams(buildName, buildNumber, buildStarted);
        return getBuild(buildPath);
    }

    /**
     * Get the build at the given JCR path
     *
     * @param buildPath Absolute JCR path
     * @return Build object if found. Null if not
     */
    private Build getBuild(String buildPath) {
        try {
            VfsNode buildNode = vfsDataService.findByPath(buildPath);
            return getBuild(buildNode);
        } catch (Exception e) {
            log.error("Unable to parse build object from the data of '{}': '{}'", buildPath, e.getMessage());
            log.debug("Unable to parse build object from the data of '" + buildPath + "'.", e);
        }
        return null;
    }

    private Build getBuild(VfsNode buildNode) {
        if (buildNode == null) {
            return null;
        }
        String buildPath = buildNode.absolutePath();
        InputStream buildInputStream = null;
        try {
            if (buildNode.hasContent()) {
                buildInputStream = buildNode.content().getStream();
                return JacksonReader.streamAsClass(buildInputStream, Build.class);
            }
        } catch (Exception e) {
            log.error("Unable to parse build object from the data of '{}': '{}'", buildPath, e.getMessage());
            log.debug("Unable to parse build object from the data of '" + buildPath + "'.", e);
        } finally {
            IOUtils.closeQuietly(buildInputStream);
        }
        return null;
    }

    @Override
    public String getBuildAsJson(String buildName, String buildNumber, String buildStarted) {
        String buildPath = getBuildPathFromParams(buildName, buildNumber, buildStarted);
        return vfsService.getContentAsString(buildPath);
    }

    @Override
    public void deleteBuild(String buildName, boolean deleteArtifacts, MultiStatusHolder multiStatusHolder) {
        if (deleteArtifacts) {
            Set<BuildRun> existingBuilds = searchBuildsByName(buildName);
            for (BuildRun existingBuild : existingBuilds) {
                removeBuildArtifacts(existingBuild, multiStatusHolder);
            }
        }
        String buildPath = PathFactoryHolder.get().getBuildsPath(buildName);
        vfsService.delete(buildPath);
    }

    @Override
    public void deleteBuild(BuildRun buildRun, boolean deleteArtifacts,
            MultiStatusHolder multiStatusHolder) {
        String buildName = buildRun.getName();
        if (deleteArtifacts) {
            removeBuildArtifacts(buildRun, multiStatusHolder);
        }
        vfsService.delete(getBuildPathFromParams(buildName, buildRun.getNumber(), buildRun.getStarted()));
        Set<BuildRun> remainingBuilds = searchBuildsByName(buildName);
        if (remainingBuilds.isEmpty()) {
            deleteBuild(buildName, false, multiStatusHolder);
        }
    }

    private void removeBuildArtifacts(BuildRun buildRun, MultiStatusHolder status) {
        String buildName = buildRun.getName();
        String buildNumber = buildRun.getNumber();
        Build build = getBuild(buildName, buildNumber, buildRun.getStarted());
        status.setDebug("Starting to remove the artifacts of build '" + buildName + "' #" + buildNumber, log);
        for (Module module : build.getModules()) {
            for (Artifact artifact : module.getArtifacts()) {
                Set<FileInfo> matchingArtifacts = getBuildFileBeanInfo(buildName, buildNumber, artifact, true);
                for (FileInfo matchingArtifact : matchingArtifacts) {
                    RepoPath repoPath = matchingArtifact.getRepoPath();
                    BasicStatusHolder undeployStatus = repositoryService.undeploy(repoPath);
                    status.merge(undeployStatus);
                }
            }
        }
        status.setDebug("Finished removing the artifacts of build '" + buildName + "' #" + buildNumber, log);
    }

    @Override
    public Build getLatestBuildByNameAndNumber(String buildName, String buildNumber) {
        if (StringUtils.isBlank(buildName)) {
            return null;
        }
        String absPath = getBuildPathFromParams(buildName, buildNumber, null);
        VfsNode buildNumberNode = vfsDataService.findByPath(absPath);
        if (buildNumberNode == null) {
            return null;
        }
        Build buildToReturn = null;
        VfsNode chosenNode = null;
        Calendar chosenCreated = null;
        for (VfsNode child : buildNumberNode.children()) {
            Calendar childCreated = child.getProperty(PROP_ARTIFACTORY_CREATED).getDate();
            if (chosenNode == null || chosenCreated.before(childCreated)) {
                chosenNode = child;
                chosenCreated = childCreated;
            }
        }
        if (chosenNode != null) {
            buildToReturn = getBuild(chosenNode);
        }

        return buildToReturn;
    }

    @Override
    public Set<BuildRun> searchBuildsByName(String buildName) {
        return getTransactionalMe().transactionalSearchBuildsByName(buildName);
    }

    @Override
    public Set<BuildRun> transactionalSearchBuildsByName(String buildName) {
        Set<BuildRun> results = Sets.newHashSet();

        if (StringUtils.isBlank(buildName)) {
            return results;
        }

        String absPath = getBuildPathFromParams(buildName, null, null);
        VfsNode buildNameNode = vfsDataService.findByPath(absPath);

        if (buildNameNode != null) {
            for (VfsNode buildNumberNode : buildNameNode.children()) {
                String escapedBuildNumber = buildNumberNode.getName();
                String buildNumber = getPathFactory().unEscape(escapedBuildNumber);
                for (VfsNode buildStartedNode : buildNumberNode.children()) {
                    String decodedBuildStarted = getPathFactory().unEscape(buildStartedNode.getName());
                    BuildRun buildRun;
                    if (!buildStartedNode.hasProperty(PROP_BUILD_RELEASE_LAST_STATUS)) {
                        buildRun = new BuildRun(buildName, buildNumber, decodedBuildStarted);
                    } else {
                        String releaseStatus = buildStartedNode
                                .getProperty(PROP_BUILD_RELEASE_LAST_STATUS).getString();
                        buildRun = new BuildRun(buildName, buildNumber, decodedBuildStarted,
                                releaseStatus);
                    }
                    results.add(buildRun);
                    log.debug("Found {} in build '{}'.", buildRun, buildName);
                }
            }
        }

        return results;
    }

    @Override
    public Set<BuildRun> searchBuildsByNameAndNumber(String buildName, String buildNumber) {
        return getTransactionalMe().transactionalSearchBuildsByNameAndNumber(buildName, buildNumber);
    }

    @Override
    public Set<BuildRun> transactionalSearchBuildsByNameAndNumber(String buildName, String buildNumber) {
        Set<BuildRun> results = Sets.newHashSet();

        if (StringUtils.isBlank(buildName) || StringUtils.isBlank(buildNumber)) {
            return results;
        }

        String absPath = getBuildPathFromParams(buildName, buildNumber, null);
        VfsNode buildNumberNode = vfsDataService.findByPath(absPath);

        if (buildNumberNode != null) {
            for (VfsNode buildStartedNode : buildNumberNode.children()) {
                String decodedBuildStarted = getPathFactory().unEscape(buildStartedNode.getName());
                results.add(new BuildRun(buildName, buildNumber, decodedBuildStarted));
            }
        }

        return results;
    }

    @Override
    public Set<FileInfo> getBuildFileBeanInfo(String buildName, String buildNumber, BuildFileBean bean,
            boolean strictMatching) {
        PropertiesAddon propertiesAddon = addonsManager.addonByType(PropertiesAddon.class);
        ChecksumSearchControls controls = new ChecksumSearchControls();
        controls.addChecksum(ChecksumType.sha1, bean.getSha1());
        controls.addChecksum(ChecksumType.md5, bean.getMd5());
        Set<RepoPath> searchResults = searchService.searchArtifactsByChecksum(controls);

        if (!strictMatching && (searchResults.size() == 1)) {
            return Sets.<FileInfo>newHashSet(new FileInfoProxy(searchResults.iterator().next()));
        } else if (!searchResults.isEmpty()) {
            Map<RepoPath, Properties> resultProperties = propertiesAddon.getProperties(searchResults);
            return getBestMatchingResult(searchResults, resultProperties, buildName, buildNumber, strictMatching);
        }
        return Sets.newHashSet();
    }

    @Override
    public Set<FileInfo> getBestMatchingResult(Set<RepoPath> searchResults, Map<RepoPath, Properties> resultProperties,
            String buildName, String buildNumber, boolean strictMatching) {

        if (resultProperties.isEmpty()) {
            Set<FileInfo> matchingItems = Sets.newHashSet();
            if (!strictMatching) {
                FileInfo latestItem = getLatestItem(searchResults);
                if (latestItem != null) {
                    matchingItems.add(latestItem);
                }
            }
            return matchingItems;
        } else {
            return matchResultBuildNameAndNumber(searchResults, resultProperties, buildName, buildNumber,
                    strictMatching);
        }
    }

    @Override
    public void exportTo(ExportSettings settings) {
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();
        multiStatusHolder.setDebug("Starting build info export", log);

        File buildsFolder = new File(settings.getBaseDir(), BUILDS_EXPORT_DIR);
        prepareBuildsFolder(settings, multiStatusHolder, buildsFolder);
        if (multiStatusHolder.isError()) {
            return;
        }

        try {
            PathFactory pathFactory = getPathFactory();
            VfsNode buildsRoot = vfsDataService.findByPath(pathFactory.getBuildsRootPath());
            if (buildsRoot == null) {
                multiStatusHolder.setError(
                        "Found build root JCR item, but it's not a node. Build export was not performed.", log);
                return;
            }

            long exportedBuildCount = 1;
            for (VfsNode buildNameNode : buildsRoot.children()) {
                String buildName = pathFactory.unEscape(buildNameNode.getName());
                for (VfsNode buildNumberNode : buildNameNode.children()) {
                    String buildNumber = pathFactory.unEscape(buildNumberNode.getName());
                    for (VfsNode buildStartedNode : buildNumberNode.children()) {
                        try {
                            exportBuild(settings, buildStartedNode, buildName, buildNumber, exportedBuildCount,
                                    buildsFolder);
                            exportedBuildCount++;
                        } catch (Exception e) {
                            String errorMessage = String.format("Failed to export build info: %s:%s", buildName,
                                    buildNumber);
                            if (settings.isFailFast()) {
                                throw new Exception(errorMessage, e);
                            }
                            multiStatusHolder.setError(errorMessage, e, log);
                        }
                    }
                }
            }
        } catch (Exception e) {
            multiStatusHolder.setError("Error occurred during build info export.", e, log);
        }

        if (settings.isIncremental() && !multiStatusHolder.isError()) {
            try {
                log.debug("Cleaning previous builds backup folder.");

                File[] backupDirsToRemove = settings.getBaseDir().listFiles(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(BACKUP_BUILDS_FOLDER);
                    }
                });
                if (backupDirsToRemove != null) {
                    for (File backupDirToRemove : backupDirsToRemove) {
                        log.debug("Cleaning previous build backup folder: {}", backupDirToRemove.getAbsolutePath());
                        FileUtils.forceDelete(backupDirToRemove);
                    }
                }
            } catch (IOException e) {
                multiStatusHolder.setError("Failed to clean previous builds backup folder.", e, log);
            }
        }

        multiStatusHolder.setDebug("Finished build info export", log);
    }

    /**
     * Makes sure that all the correct build/backup dirs are prepared for backup
     *
     * @param settings          Export settings
     * @param multiStatusHolder Process status holder
     * @param buildsFolder      Builds folder within the backup
     */
    private void prepareBuildsFolder(ExportSettings settings, MutableStatusHolder multiStatusHolder,
            File buildsFolder) {
        if (buildsFolder.exists()) {
            // Backup previous builds folder if incremental
            if (settings.isIncremental()) {
                File tempBuildBackupDir = new File(settings.getBaseDir(),
                        BACKUP_BUILDS_FOLDER + "." + System.currentTimeMillis());
                try {
                    FileUtils.moveDirectory(buildsFolder, tempBuildBackupDir);
                    FileUtils.forceMkdir(buildsFolder);
                } catch (IOException e) {
                    multiStatusHolder.setError(
                            "Failed to create incremental builds temp backup dir: " + tempBuildBackupDir, e, log);
                }
            }
        } else {
            try {
                FileUtils.forceMkdir(buildsFolder);
            } catch (IOException e) {
                multiStatusHolder.setError("Failed to create builds backup dir: " + buildsFolder, e, log);
            }
        }
    }

    @Override
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();
        multiStatusHolder.setStatus("Starting build info import", log);

        try {
            // delete existing root builds node
            String buildsRootPath = getPathFactory().getBuildsRootPath();
            vfsService.delete(buildsRootPath);
            vfsService.createIfNeeded(buildsRootPath);
        } catch (Exception e) {
            multiStatusHolder.setError("Failed to delete builds root node", e, log);
            return;
        }

        File buildsFolder = new File(settings.getBaseDir(), BUILDS_EXPORT_DIR);
        String buildsFolderPath = buildsFolder.getPath();
        if (!buildsFolder.exists()) {
            multiStatusHolder.setStatus("'" + buildsFolderPath + "' folder is either non-existent or not a " +
                    "directory. Build info import was not performed", log);
            return;
        }

        IOFileFilter buildExportFileFilter = new AbstractFileFilter() {
            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                return fileName.startsWith("build") && fileName.endsWith(".xml");
            }
        };

        @SuppressWarnings({"unchecked"})
        Collection<File> buildExportFiles =
                FileUtils.listFiles(buildsFolder, buildExportFileFilter, DirectoryFileFilter.DIRECTORY);

        if (buildExportFiles.isEmpty()) {
            multiStatusHolder.setStatus("'" + buildsFolderPath + "' folder does not contain build export files. " +
                    "Build info import was not performed", log);
            return;
        }

        importBuildFiles(settings, buildExportFiles);
        multiStatusHolder.setStatus("Finished build info import", log);
    }

    @Override
    public void importBuild(ImportSettings settings, ImportableExportableBuild build) throws Exception {
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();

        String buildName = getPathFactory().escape(build.getBuildName());
        String buildNumber = getPathFactory().escape(build.getBuildNumber());
        String buildStarted = getPathFactory().escape(build.getBuildStarted());

        multiStatusHolder.setDebug(
                String.format("Beginning import of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);
        MutableVfsNode buildNumberNode = createAndGetNumberNode(buildName, build.getBuildNumber(),
                build.getBuildStarted());
        MutableVfsNode buildStartedNode = buildNumberNode.getOrCreateSubNode(buildStarted, VfsNodeType.UNSTRUCTURED);

        InputStream stream = null;
        try {
            stream = IOUtils.toInputStream(build.getJson());
            MutableBinaryContent binary = vfsDataService.createBinary(BuildRestConstants.MT_BUILD_INFO, stream);
            binary.setLastModified(build.getLastModified());
            buildStartedNode.setContent(binary);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        buildStartedNode.setProperty(PROP_ARTIFACTORY_CREATED, build.getCreated());
        buildStartedNode.setProperty(PROP_ARTIFACTORY_LAST_MODIFIED, build.getLastModified());
        buildStartedNode.setProperty(PROP_ARTIFACTORY_LAST_MODIFIED_BY, build.getLastModifiedBy());

        // set the artifacts and dependencies checksums
        saveBuildFileChecksums(buildStartedNode, build.getArtifactChecksums(), build.getDependencyChecksums());

        updateReleaseLastStatusProperty(getBuild(buildStartedNode), buildStartedNode);

        multiStatusHolder.setDebug(
                String.format("Finished import of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);
    }

    @Override
    public Set<String> findScopes(Build build) {
        final Set<String> scopes = Sets.newHashSet();
        if (build.getModules() != null) {
            for (Module module : build.getModules()) {
                if (module.getDependencies() != null) {
                    for (Dependency dependency : module.getDependencies()) {
                        List<String> dependencyScopes = dependency.getScopes();
                        if (dependencyScopes != null) {
                            for (String dependencyScope : dependencyScopes) {
                                if (StringUtils.isBlank(dependencyScope)) {
                                    scopes.add(UNSPECIFIED_SCOPE);
                                } else {
                                    scopes.add(dependencyScope);
                                }
                            }
                        }
                    }
                }
            }
        }
        return scopes;
    }

    @Override
    public boolean isGenericBuild(Build build) {
        BuildAgent buildAgent = build.getBuildAgent();
        if (buildAgent != null) {
            String buildAgentName = buildAgent.getName();
            return !"ivy".equalsIgnoreCase(buildAgentName) && !"maven".equalsIgnoreCase(buildAgentName) &&
                    !"gradle".equalsIgnoreCase(buildAgentName);
        }

        BuildType type = build.getType();
        return BuildType.ANT.equals(type) || BuildType.GENERIC.equals(type);
    }

    @Override
    public String getBuildCiServerUrl(BuildRun buildRun) throws IOException {
        String buildPath = getBuildPathFromParams(buildRun.getName(), buildRun.getNumber(),
                buildRun.getStarted());
        InputStream jsonStream = null;
        JsonParser parser = null;
        try {
            jsonStream = vfsService.getStream(buildPath);
            if (jsonStream != null) {
                parser = JacksonFactory.createJsonParser(jsonStream);

                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new IOException("Expected data stream to start with an object.");
                }

                while (parser.nextToken() != null) {
                    JsonStreamContext context = parser.getParsingContext();
                    String fieldName = context.getCurrentName();
                    if ("url".equals(fieldName) && context.getParent().inRoot()) {
                        JsonToken urlValueToken = parser.nextToken();
                        if (urlValueToken != null) {
                            String url = parser.getText();
                            //The parser may take null literally
                            if (StringUtils.isBlank(url) || "null".equals(url)) {
                                return null;
                            }
                            return url;
                        }
                    }
                }
            }
        } finally {
            if (jsonStream != null) {
                IOUtils.closeQuietly(jsonStream);
            }
            if (parser != null) {
                parser.close();
            }
        }

        return null;
    }

    @Override
    public MoveCopyResult moveOrCopyBuildItems(boolean move, BuildRun buildRun, String targetRepoKey,
            boolean artifacts, boolean dependencies, List<String> scopes, Properties properties, boolean dryRun) {
        BuildItemMoveCopyHelper itemMoveCopyHelper = new BuildItemMoveCopyHelper();
        return itemMoveCopyHelper.moveOrCopy(move, buildRun, targetRepoKey, artifacts, dependencies, scopes,
                properties, dryRun);
    }

    @Override
    public PromotionResult promoteBuild(BuildRun buildRun, Promotion promotion) {
        BuildPromotionHelper buildPromotionHelper = new BuildPromotionHelper();
        return buildPromotionHelper.promoteBuild(buildRun, promotion);
    }

    @Override
    public void renameBuilds(String from, String to) {
        Set<BuildRun> buildsToRename = searchBuildsByName(from);
        if (buildsToRename.isEmpty()) {
            log.error("Could not find builds by the name '{}'. No builds were renamed.", from);
            return;
        }

        for (BuildRun buildToRename : buildsToRename) {
            try {
                getTransactionalMe().renameBuildContent(buildToRename, to);
                log.info("Renamed build number '{}' that started at '{}' from '{}' to '{}'.", new String[]{
                        buildToRename.getNumber(), buildToRename.getStarted(), buildToRename.getName(), to});
            } catch (Exception e) {
                log.error("Failed to rename build: '{}' #{} that started at {}.", new String[]{buildToRename.getName(),
                        buildToRename.getNumber(), buildToRename.getStarted()});
            }
        }

        try {
            getTransactionalMe().renameBuildNode(from, to);
            log.info("Renamed build node from '{}' to '{}'.", from, to);
        } catch (Exception e) {
            log.error("Failed to rename JCR build node from '{}' to '{}'.", from, to);
        }
    }

    @Override
    public void renameBuildNode(String from, String to) throws RepositoryException {
        String oldNamePath = getBuildPathFromParams(from, null, null);

        if (!vfsService.nodeExists(oldNamePath)) {
            log.error("Could not find a build tree node by the name '{}'. Build node was not renamed.", from);
            return;
        }
        MutableVfsNode oldNameNode = vfsDataService.getOrCreate(oldNamePath);

        String newNamePath = getBuildPathFromParams(to, null, null);
        MutableVfsNode newNameNode = vfsDataService.getOrCreate(newNamePath);
        for (MutableVfsNode oldNumberNode : oldNameNode.mutableChildren()) {
            MutableVfsNode newNumberNode = newNameNode.getOrCreateSubNode(oldNumberNode.getName(),
                    VfsNodeType.UNSTRUCTURED);
            for (MutableVfsNode oldStartedNode : oldNumberNode.mutableChildren()) {
                oldStartedNode.moveTo(newNumberNode);
            }
            if (!oldNumberNode.hasChildren()) {
                oldNumberNode.delete();
            }
        }
        if (!oldNameNode.hasChildren()) {
            oldNameNode.delete();
        }
    }

    @Override
    public void renameBuildContent(BuildRun buildRun, String to) throws RepositoryException, IOException {
        String buildName = buildRun.getName();
        String buildNumber = buildRun.getNumber();
        String buildStarted = buildRun.getStarted();

        String buildPath = getBuildPathFromParams(buildName, buildNumber, buildStarted);
        VfsNode buildNode = vfsDataService.findByPath(buildPath);
        if (buildNode == null) {
            log.error("Could not find build to rename at path: {}", buildPath);
            return;
        }

        Set<String> artifactChecksums = getChecksumPropertyValue(buildNode, PROP_BUILD_ARTIFACT_CHECKSUMS);
        Set<String> dependencyChecksums = getChecksumPropertyValue(buildNode, PROP_BUILD_DEPENDENCY_CHECKSUMS);

        InputStream jsonStream = buildNode.content().getStream();
        final JsonNode rootNode;
        try {
            rootNode = JacksonReader.streamAsTree(jsonStream);
        } finally {
            IOUtils.closeQuietly(jsonStream);
        }
        ((ObjectNode) rootNode).put("name", to);

        MutableVfsNode mutableBuildNode;
        InputStreamFromOutputStream stream = null;
        try {
            stream = new InputStreamFromOutputStream() {
                @Override
                protected Object produce(OutputStream outputStream) throws Exception {
                    JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(outputStream);
                    jsonGenerator.writeTree(rootNode);
                    return null;
                }
            };
            mutableBuildNode = vfsDataService.makeMutable(buildNode);
            mutableBuildNode.setContent(vfsDataService.createBinary(BuildRestConstants.MT_BUILD_INFO, stream));
        } catch (Exception e) {
            log.error("An error occurred while writing JSON data to the node of build name '{}', number '{}', " +
                    "started at '{}'.", new String[]{buildName, buildNumber, buildStarted});
            return;
        } finally {
            IOUtils.closeQuietly(stream);
        }

        try {
            saveBuildFileChecksums(mutableBuildNode, artifactChecksums, dependencyChecksums);
        } catch (Exception e) {
            log.error("An error occurred while saving checksum properties on the node of build name '{}', number " +
                    "'{}', started at '{}'.", new String[]{buildName, buildNumber, buildStarted});
        }
    }

    @Override
    public void updateBuild(final Build build) {
        String buildName = build.getName();
        String buildNumber = build.getNumber();
        String buildStarted = build.getStarted();

        String buildPathFromParams = getBuildPathFromParams(buildName, buildNumber, buildStarted);

        MutableVfsNode buildNode = vfsDataService.getOrCreate(buildPathFromParams);

        InputStreamFromOutputStream stream = null;
        try {
            stream = new InputStreamFromOutputStream() {

                @Override
                protected Object produce(OutputStream outputStream) throws Exception {
                    JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(outputStream);
                    jsonGenerator.writeObject(build);
                    return null;
                }
            };
            buildNode.setContent(vfsDataService.createBinary(BuildRestConstants.MT_BUILD_INFO, stream));
        } catch (Exception e) {
            String errorMessage = String.format("An error occurred while updating the JSON data of build name " +
                    "'%s', number '%s', started at '%s'.", buildName, buildNumber, buildStarted);
            throw new RuntimeException(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        try {
            updateReleaseLastStatusProperty(build, buildNode);
        } catch (RepositoryException e) {
            String errorMessage = String.format("An error occurred while updating the latest release status " +
                    "property of build name '%s', number '%s', started at '%s'.", buildName, buildNumber, buildStarted);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public String getBuildLatestReleaseStatus(String buildName, String buildNumber, String buildStarted) {
        String buildPathFromParams = getBuildPathFromParams(buildName, buildNumber, buildStarted);
        VfsNode buildNode = vfsDataService.findByPath(buildPathFromParams);
        if (buildNode != null) {
            return buildNode.getStringProperty(PROP_BUILD_RELEASE_LAST_STATUS);
        }
        return null;
    }

    /**
     * Locates and fills in missing checksums of the module artifacts and dependencies
     *
     * @param modules Modules to populate
     */
    private void populateMissingChecksums(List<Module> modules) {
        if (modules != null) {
            for (Module module : modules) {
                handleBeanPopulation(module.getArtifacts());
                handleBeanPopulation(module.getDependencies());
            }
        }
    }

    /**
     * Locates and fills in missing checksums of a build file bean
     *
     * @param buildFiles List of build files to populate
     */
    private void handleBeanPopulation(List<? extends BuildFileBean> buildFiles) {
        if (buildFiles != null) {
            for (BuildFileBean buildFile : buildFiles) {
                boolean sha1Exists = StringUtils.isNotBlank(buildFile.getSha1());
                boolean md5Exists = StringUtils.isNotBlank(buildFile.getMd5());

                //If the bean has both or none of the checksums, return
                if ((sha1Exists && md5Exists) || ((!sha1Exists && !md5Exists))) {
                    return;
                }

                if (!sha1Exists) {
                    populateBeanSha1Checksum(buildFile);
                } else {
                    populateBeanMd5Checksum(buildFile);
                }
            }
        }
    }

    /**
     * Locates and fills the missing SHA1 checksum of a build file bean
     *
     * @param bean Bean to populate
     */
    private void populateBeanSha1Checksum(BuildFileBean bean) {
        log.trace("Populating sha1 checksum for {}", bean);
        String md5 = bean.getMd5();
        FutureTask<ChecksumPair> future = getFuture(bean, md5, md5Cache);
        try {
            ChecksumPair pair = future.get();
            if (!pair.checksumsFound()) {
                //Remove unfound checksum, so it won't reside in the cache, possibly misinforming
                md5Cache.remove(md5);
                return;
            }
            String resultSha1 = pair.getSha1();
            bean.setSha1(resultSha1);

            //Also update the SHA1 cache with the result, for future requests
            sha1Cache.put(resultSha1, future);
            log.trace("Populated sha1 checksum for {}", bean);
        } catch (Exception e) {
            //Remove the problematic checksum to avoid cache poisoning
            md5Cache.remove(md5);
            log.error("An error occurred while trying to populate missing SHA1 checksum for build file bean with MD5 " +
                    "checksum '{}' : ", md5, e.getMessage());
            log.debug("Bean '" + md5 + "' checksum population error: ", e);
        }
    }

    /**
     * Locates and fills the missing MD5 checksum of a build file bean
     *
     * @param bean Bean to populate
     */
    private void populateBeanMd5Checksum(BuildFileBean bean) {
        String sha1 = bean.getSha1();
        FutureTask<ChecksumPair> future = getFuture(bean, sha1, sha1Cache);
        try {
            ChecksumPair pair = future.get();
            if (!pair.checksumsFound()) {
                //Remove unfound checksum, so it won't reside in the cache, possibly misinforming
                sha1Cache.remove(sha1);
                return;
            }
            String resultMd5 = pair.getMd5();
            bean.setMd5(resultMd5);

            //Also update the MD5 cache with the result, for future requests
            md5Cache.put(resultMd5, future);
        } catch (Exception e) {
            //Remove the problematic checksum to avoid cache poisoning
            sha1Cache.remove(sha1);
            log.error("An error occurred while trying to populate missing MD5 checksum for build file bean with SHA1 " +
                    "checksum '{}' : ", sha1, e.getMessage());
            log.debug("Bean '" + sha1 + "' checksum population error: ", e);
        }
    }

    /**
     * Returns a FutureTask object for populating a missing checksum
     *
     * @param bean                  Bean to populate
     * @param existingChecksum      The checksum which already exists in the bean, can help us search for the missing
     *                              one
     * @param existingChecksumCache The cache of the existing checksum, will keep the missing checksum for future
     *                              requests, might already hold the value
     * @return Future that returns the value of the missing checksum search
     */
    private FutureTask<ChecksumPair> getFuture(BuildFileBean bean, String existingChecksum,
            ConcurrentMap<String, FutureTask<ChecksumPair>> existingChecksumCache) {

        //Create callable and future tasks for checksum location
        Callable<ChecksumPair> callable = new MissingChecksumCallable(bean.getSha1(), bean.getMd5());
        FutureTask<ChecksumPair> future = new FutureTask<ChecksumPair>(callable);

        //Use the *put if absent* to make sure that a similar task has not been executed yet
        FutureTask<ChecksumPair> cachedFuture = existingChecksumCache.putIfAbsent(existingChecksum, future);
        if (cachedFuture == null) {
            //Might try to run a ran task once more (cache concurrency issue), but future protects from this
            executor.submit(future);
            return future;
        } else {
            return cachedFuture;
        }
    }

    /**
     * Collects all the checksums from the files of the given modules
     *
     * @param modules             Modules to collect from
     * @param artifactChecksums   Artifact checksum set to append to
     * @param dependencyChecksums Dependency checksum set to append to
     */
    private void collectModuleChecksums(List<Module> modules, Set<String> artifactChecksums,
            Set<String> dependencyChecksums) {
        if (modules != null) {
            for (Module module : modules) {
                collectBuildFileChecksums(module.getArtifacts(), artifactChecksums);
                collectBuildFileChecksums(module.getDependencies(), dependencyChecksums);
            }
        }
    }

    /**
     * Collects all the checksums of the given build files. Each checksum value will be prepended with a template of
     * {CHECKSUM_TYPE}
     *
     * @param buildFiles         Build files to collect from
     * @param buildFileChecksums Checksum set to append to
     */
    private void collectBuildFileChecksums(List<? extends BuildFileBean> buildFiles, Set<String> buildFileChecksums) {
        if (buildFiles != null) {
            for (BuildFileBean buildFile : buildFiles) {
                String md5 = buildFile.getMd5();
                String sha1 = buildFile.getSha1();

                if (StringUtils.isNotBlank(md5)) {
                    buildFileChecksums.add(BUILD_CHECKSUM_PREFIX_MD5 + md5);
                }
                if (StringUtils.isNotBlank(sha1)) {
                    buildFileChecksums.add(BUILD_CHECKSUM_PREFIX_SHA1 + sha1);
                }
            }
        }
    }

    /**
     * Create the required tree structure up to the number node level and return it
     *
     * @param escapedBuildName Jcr-escaped build name
     * @param buildNumber      Original build number
     * @param buildStarted     Original build started
     * @return Build number level node
     */
    private MutableVfsNode createAndGetNumberNode(String escapedBuildName, String buildNumber,
            String buildStarted) {

        MutableVfsNode buildsNode = vfsDataService.getOrCreate(getPathFactory().getBuildsRootPath());
        MutableVfsNode buildNameNode = buildsNode.getOrCreateSubNode(escapedBuildName, VfsNodeType.UNSTRUCTURED);

        Calendar newStartDate = Calendar.getInstance();
        try {
            newStartDate.setTime(new SimpleDateFormat(Build.STARTED_FORMAT).parse(buildStarted));
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse given build start date.", e);
        }

        try {
            boolean isLatest = true;
            if (buildNameNode.hasProperty(PROP_BUILD_LATEST_START_TIME)) {
                Calendar existingStartDate = buildNameNode.getProperty(PROP_BUILD_LATEST_START_TIME).getDate();
                if (existingStartDate != null) {
                    isLatest = existingStartDate.before(newStartDate);
                }
            }

            if (isLatest) {
                buildNameNode.setProperty(PROP_BUILD_LATEST_NUMBER, buildNumber);
                buildNameNode.setProperty(PROP_BUILD_LATEST_START_TIME, newStartDate);
            }
        } catch (RepositoryRuntimeException e) {
            throw new RepositoryRuntimeException("Could not update the start date property of the build name node: " +
                    escapedBuildName, e);
        }

        return buildNameNode.getOrCreateSubNode(getPathFactory().escape(buildNumber), VfsNodeType.UNSTRUCTURED);
    }

    /**
     * Saves the build file checksums as properties on the given build node
     *
     * @param buildStartedNode    "Build-started" level node
     * @param artifactChecksums   Artifact checksum set
     * @param dependencyChecksums Dependency checksum set
     */
    private void saveBuildFileChecksums(MutableVfsNode buildStartedNode, Set<String> artifactChecksums,
            Set<String> dependencyChecksums) throws RepositoryException {
        saveBuildChecksumsProperty(buildStartedNode, PROP_BUILD_ARTIFACT_CHECKSUMS, artifactChecksums);
        saveBuildChecksumsProperty(buildStartedNode, PROP_BUILD_DEPENDENCY_CHECKSUMS, dependencyChecksums);
    }

    /**
     * Assembles an absolute JCR path of the build node from the given params
     *
     * @param buildName    Build name
     * @param buildNumber  Build number
     * @param buildStarted Build started
     * @return Build absolute JCR path
     */
    private String getBuildPathFromParams(String buildName,
            @Nullable String buildNumber,
            @Nullable String buildStarted) {
        PathFactory pathFactory = getPathFactory();
        PathBuilder pathBuilder = pathFactory.createBuilder(pathFactory.getBuildsRootPath());
        return pathBuilder.append(buildName, buildNumber, buildStarted).toString();
    }

    /**
     * Saves the given checksums on the given build node
     *
     * @param buildStartedNode Build node to save on
     * @param checksumPropName Checksum property name to save on
     * @param checksums        Set of checksums to save
     */
    private void saveBuildChecksumsProperty(MutableVfsNode buildStartedNode, String checksumPropName,
            Set<String> checksums)
            throws RepositoryException {
        if (!checksums.isEmpty()) {
            buildStartedNode.setProperty(checksumPropName, checksums.toArray(new String[checksums.size()]));
        }
    }

    /**
     * Returns the best matching file info object by build name and number
     *
     * @param searchResults    File bean search results
     * @param resultProperties Search result property map
     * @param buildName        Build name to search for
     * @param buildNumber      Build number to search for
     * @param strictMatching   True if the artifact finder should operate in strict mode
     * @return The file info of a result that best matches the given build name and number
     */
    private Set<FileInfo> matchResultBuildNameAndNumber(Set<RepoPath> searchResults,
            Map<RepoPath, Properties> resultProperties, String buildName, String buildNumber, boolean strictMatching) {
        Map<RepoPath, Properties> matchingBuildNames = Maps.newHashMap();

        for (Map.Entry<RepoPath, Properties> repoPathPropertiesEntry : resultProperties.entrySet()) {
            Properties properties = repoPathPropertiesEntry.getValue();
            Set<String> buildNames = properties.get("build.name");
            if (buildNames != null && buildNames.contains(buildName)) {
                matchingBuildNames.put(repoPathPropertiesEntry.getKey(), properties);
            }
        }

        if (matchingBuildNames.isEmpty()) {
            Set<FileInfo> matchingItems = Sets.newHashSet();
            if (!strictMatching) {
                FileInfo latestItem = getLatestItem(searchResults);
                if (latestItem != null) {
                    matchingItems.add(latestItem);
                }
            }
            return matchingItems;
        } else {
            return matchResultBuildNumber(resultProperties, matchingBuildNames, buildNumber, strictMatching);
        }
    }

    /**
     * Returns the best matching file info object by build number
     *
     * @param resultProperties Search result property map
     * @param matchingPaths    File info paths that match by build name
     * @param buildNumber      Build number to search for
     * @param strictMatching   True if the artifact finder should operate in strict mode
     * @return The file info of a result that best matches the given build number
     */
    private Set<FileInfo> matchResultBuildNumber(Map<RepoPath, Properties> resultProperties,
            Map<RepoPath, Properties> matchingPaths, String buildNumber, boolean strictMatching) {
        Set<FileInfo> matchingItems = Sets.newHashSet();
        for (RepoPath repoPath : matchingPaths.keySet()) {
            Properties properties = resultProperties.get(repoPath);
            Set<String> buildNumbers = properties.get("build.number");
            if (buildNumbers != null && buildNumbers.contains(buildNumber)) {
                matchingItems.add(new FileInfoProxy(repoPath));
            }
        }

        if (matchingItems.isEmpty() && !strictMatching && !matchingPaths.isEmpty()) {
            matchingItems.add(new FileInfoProxy(matchingPaths.keySet().iterator().next()));
        }
        return matchingItems;
    }

    /**
     * Returns the file info object of the result watch was last modified
     *
     * @param searchResults Search results to search within
     * @return Latest modified search result file info. Null if no results were given
     */
    private FileInfo getLatestItem(Set<RepoPath> searchResults) {
        FileInfo latestItem = null;

        for (RepoPath result : searchResults) {
            FileInfo fileInfo = new FileInfoProxy(result);
            if ((latestItem == null) || (latestItem.getLastModified() < fileInfo.getLastModified())) {
                latestItem = fileInfo;
            }
        }
        return latestItem;
    }

    /**
     * Returns the path factory object
     *
     * @return the path factory
     */
    private PathFactory getPathFactory() {
        return PathFactoryHolder.get();
    }

    private void exportBuild(ExportSettings settings, VfsNode buildStartedNode, String buildName, String buildNumber,
            long exportedBuildCount, File buildsFolder) throws Exception {
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();

        String buildStarted = buildStartedNode.getName();
        multiStatusHolder.setDebug(
                String.format("Beginning export of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);

        ImportableExportableBuild exportedBuild = new ImportableExportableBuild();
        exportedBuild.setVersion(EXPORTABLE_BUILD_VERSION);
        exportedBuild.setBuildName(getPathFactory().unEscape(buildName));
        exportedBuild.setBuildNumber(getPathFactory().unEscape(buildNumber));
        exportedBuild.setBuildStarted(getPathFactory().unEscape(buildStarted));

        String jsonString = buildStartedNode.content().getContentAsString();
        exportedBuild.setJson(jsonString);

        exportBuildNodeMetadata(buildStartedNode, exportedBuild);

        File buildFile = new File(buildsFolder, "build" + Long.toString(exportedBuildCount) + ".xml");
        exportBuildToFile(exportedBuild, buildFile);

        multiStatusHolder.setDebug(
                String.format("Finished export of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);
    }

    private void importBuildFiles(ImportSettings settings, Collection<File> buildExportFiles) {

        XStream xStream = getImportableExportableXStream();
        for (File buildExportFile : buildExportFiles) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(buildExportFile);
                ImportableExportableBuild importableBuild = (ImportableExportableBuild) xStream.fromXML(inputStream);
                // import each build in a separate transaction
                getTransactionalMe().importBuild(settings, importableBuild);
            } catch (Exception e) {
                settings.getStatusHolder().setError("Error occurred during build info import", e, log);
                if (settings.isFailFast()) {
                    break;
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    private void exportBuildNodeMetadata(VfsNode buildStartedNode, ImportableExportableBuild exportedBuild)
            throws Exception {
        exportedBuild.setArtifactChecksums(getBuildFileChecksums(buildStartedNode, PROP_BUILD_ARTIFACT_CHECKSUMS));
        exportedBuild.setDependencyChecksums(getBuildFileChecksums(buildStartedNode, PROP_BUILD_DEPENDENCY_CHECKSUMS));

        exportedBuild.setCreated(buildStartedNode.getLongProperty(PROP_ARTIFACTORY_CREATED));
        exportedBuild.setLastModified(buildStartedNode.getLongProperty(PROP_ARTIFACTORY_LAST_MODIFIED));
        exportedBuild.setCreatedBy(buildStartedNode.getStringProperty(PROP_ARTIFACTORY_CREATED_BY));
        exportedBuild.setLastModifiedBy(buildStartedNode.getStringProperty(PROP_ARTIFACTORY_LAST_MODIFIED_BY));
        BinaryContent content = buildStartedNode.content();
        exportedBuild.setMimeType(content.getMimeType());
        exportedBuild.setChecksumsInfo(content.getChecksums());
    }

    private Set<String> getBuildFileChecksums(VfsNode buildStartedNode, String buildFileTypePropName)
            throws Exception {
        Set<String> checksums = Sets.newHashSet();
        if (buildStartedNode.hasProperty(buildFileTypePropName)) {
            checksums.addAll(buildStartedNode.getProperty(buildFileTypePropName).getStrings());
        }
        return checksums;
    }

    private void exportBuildToFile(ImportableExportableBuild exportedBuild, File buildFile) throws Exception {
        FileOutputStream buildFileOutputStream = null;
        try {
            buildFileOutputStream = new FileOutputStream(buildFile);
            getImportableExportableXStream().toXML(exportedBuild, buildFileOutputStream);
        } finally {
            IOUtils.closeQuietly(buildFileOutputStream);
        }
    }

    /**
     * Returns an XStream object to parse and generate {@link org.artifactory.api.build.ImportableExportableBuild}
     * classes
     *
     * @return ImportableExportableBuild ready XStream object
     */
    private XStream getImportableExportableXStream() {
        return XStreamFactory.create(ImportableExportableBuild.class);
    }

    /**
     * Returns an internal instance of the service
     *
     * @return InternalBuildService
     */
    private InternalBuildService getTransactionalMe() {
        return ContextHelper.get().beanForType(InternalBuildService.class);
    }

    /**
     * Returns the value of the build node checksum property
     *
     * @param buildNode    Build node to extract from
     * @param propertyName Name of checksum property
     * @return Property values
     */
    private Set<String> getChecksumPropertyValue(VfsNode buildNode, String propertyName) throws RepositoryException {
        Set<String> checksums = Sets.newHashSet();
        if (buildNode.hasProperty(propertyName)) {
            checksums.addAll(buildNode.getProperty(propertyName).getStrings());
        }
        return checksums;
    }

    private void updateReleaseLastStatusProperty(Build build, MutableVfsNode buildNode) throws RepositoryException {
        String latestReleaseStatus = getLatestReleaseStatus(build);

        if (latestReleaseStatus == null) {
            return;
        }

        updateReleaseLastStatusProperty(buildNode, latestReleaseStatus);
    }

    private void updateReleaseLastStatusProperty(MutableVfsNode buildNode, String latestReleaseStatus)
            throws RepositoryException {
        if (!buildNode.hasProperty(PROP_BUILD_RELEASE_LAST_STATUS)) {
            buildNode.setProperty(PROP_BUILD_RELEASE_LAST_STATUS, latestReleaseStatus);
        } else {
            String currentStatus = buildNode.getProperty(PROP_BUILD_RELEASE_LAST_STATUS).getString();
            if (!currentStatus.equals(latestReleaseStatus)) {
                buildNode.setProperty(PROP_BUILD_RELEASE_LAST_STATUS, latestReleaseStatus);
            }
        }
    }

    private String getLatestReleaseStatus(final Build build) {
        List<PromotionStatus> statuses = build.getStatuses();
        if ((statuses == null) || statuses.isEmpty()) {
            return null;
        }

        if (statuses.size() == 1) {
            return statuses.get(0).getStatus();
        }

        Collections.sort(statuses, new Comparator<PromotionStatus>() {
            @Override
            public int compare(PromotionStatus first, PromotionStatus second) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
                try {
                    Date firstDate = dateFormat.parse(first.getTimestamp());
                    Date secondDate = dateFormat.parse(second.getTimestamp());
                    return firstDate.compareTo(secondDate);
                } catch (ParseException e) {
                    log.error("Unable to parse build ({}, #{}, {}) statuses for comparison: {}",
                            new Object[]{build.getName(), build.getNumber(), build.getStarted(), e.getMessage()});
                    return 0;
                }
            }
        });

        //Return latest
        return statuses.get(statuses.size() - 1).getStatus();
    }
}
