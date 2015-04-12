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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import org.artifactory.addon.BlackDuckAddon;
import org.artifactory.addon.license.LicensesAddon;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.build.AfterBuildSaveAction;
import org.artifactory.addon.plugin.build.BeforeBuildSaveAction;
import org.artifactory.api.build.BuildRunComparators;
import org.artifactory.api.build.ImportableExportableBuild;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.aql.AqlConverts;
import org.artifactory.aql.AqlService;
import org.artifactory.aql.api.domain.sensitive.AqlApiBuild;
import org.artifactory.aql.api.domain.sensitive.AqlApiItem;
import org.artifactory.aql.api.internal.AqlBase;
import org.artifactory.aql.model.AqlComparatorEnum;
import org.artifactory.aql.model.AqlFieldEnum;
import org.artifactory.aql.result.AqlEagerResult;
import org.artifactory.aql.result.rows.AqlBaseFullRowImpl;
import org.artifactory.aql.result.rows.AqlBuild;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.factory.xstream.XStreamFactory;
import org.artifactory.fs.FileInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.build.service.BuildStoreService;
import org.artifactory.storage.db.DbService;
import org.artifactory.version.CompoundVersionDetails;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Issue;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.release.PromotionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static org.artifactory.aql.api.internal.AqlBase.and;

/**
 * Build service main implementation
 *
 * @author Noam Y. Tenne
 */
@Service
@Reloadable(beanClass = InternalBuildService.class, initAfter = {DbService.class})
public class BuildServiceImpl implements InternalBuildService {
    public static final String BUILDS_EXPORT_DIR = "builds";
    private static final Logger log = LoggerFactory.getLogger(BuildServiceImpl.class);
    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private BuildStoreService buildStoreService;

    @Autowired
    private DbService dbService;

    @Autowired(required = false)
    private Builds builds;

    @Autowired
    private AqlService aqlService;

    @Override
    public void init() {
        //Nothing to init
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
    public void addBuild(@Nonnull DetailedBuildRun detailedBuildRun) {
        getTransactionalMe().addBuild(((DetailedBuildRunImpl) detailedBuildRun).build, false);
    }

    @Override
    public void addBuild(final Build build) {
        getTransactionalMe().addBuild(build, true);
    }

    @Override
    public void addBuild(final Build build, boolean activateCallbacks) {
        String buildName = build.getName();
        String buildNumber = build.getNumber();
        String buildStarted = build.getStarted();
        String currentUser = authorizationService.currentUsername();

        log.debug("Adding info for build '{}' #{} d{}", buildName, buildNumber, buildStarted);

        build.setArtifactoryPrincipal(currentUser);
        buildStoreService.populateMissingChecksums(build);

        aggregatePreviousBuildIssues(build);

        DetailedBuildRun detailedBuildRun = new DetailedBuildRunImpl(build);
        PluginsAddon pluginsAddon = addonsManager.addonByType(PluginsAddon.class);
        pluginsAddon.execPluginActions(BeforeBuildSaveAction.class, builds, detailedBuildRun);

        buildStoreService.addBuild(build);

        log.debug("Added info for build '{}' #{}", buildName, buildNumber);

        LicensesAddon licensesAddon = addonsManager.addonByType(LicensesAddon.class);
        licensesAddon.performOnBuildArtifacts(build);

        BlackDuckAddon blackDuckAddon = addonsManager.addonByType(BlackDuckAddon.class);
        blackDuckAddon.performBlackDuckOnBuildArtifacts(build);

        //Abort calling plugin actions if called from papi to avoid accidental endless loops
        if (activateCallbacks) {
            pluginsAddon.execPluginActions(AfterBuildSaveAction.class, builds, detailedBuildRun);
        }
    }

    /**
     * Check if the latest build available has issues, add them all the our newly created build
     * only if the previous build status is not in "Released" status (configurable status from our plugins).
     * This way we collect all previous issues related to the same version which is not released yet.
     *
     * @param newBuild the newly created build to add previous issues to
     */
    private void aggregatePreviousBuildIssues(Build newBuild) {
        Issues newBuildIssues = newBuild.getIssues();
        if (newBuildIssues == null) {
            return;
        }

        if (!newBuildIssues.isAggregateBuildIssues()) {
            return;
        }

        Build latestBuild = getLatestBuildByNameAndStatus(newBuild.getName(), LATEST_BUILD);
        if (latestBuild == null) {
            return;
        }

        // Only aggregate if the previous build does not equal to the user requested status (e.g: "Released")
        // this way we only aggregate the issues related to the current release
        List<PromotionStatus> statuses = latestBuild.getStatuses();
        if (statuses != null) {
            String aggregationBuildStatus = newBuildIssues.getAggregationBuildStatus();
            for (PromotionStatus status : statuses) {
                if (status.getStatus().equalsIgnoreCase(aggregationBuildStatus)) {
                    return;
                }
            }
        }

        // It is important to create new Issue instance so we won't mess up previous ones
        Issues previousIssues = latestBuild.getIssues();
        if (previousIssues != null) {
            Set<Issue> affectedIssues = previousIssues.getAffectedIssues();
            if (affectedIssues != null) {
                for (Issue issue : affectedIssues) {
                    Issue issueToAdd = new Issue(issue.getKey(), issue.getUrl(), issue.getSummary());
                    issueToAdd.setAggregated(true);
                    newBuildIssues.addIssue(issueToAdd);
                }
            }
        }
    }

    @Override
    public BuildRun getBuildRun(String buildName, String buildNumber, String buildStarted) {
        return buildStoreService.getBuildRun(buildName, buildNumber, buildStarted);
    }

    @Override
    public Build getBuild(BuildRun buildRun) {
        return buildStoreService.getBuildJson(buildRun);
    }

    @Override
    public String getBuildAsJson(BuildRun buildRun) {
        return buildStoreService.getBuildAsJson(buildRun);
    }

    @Override
    public void deleteBuild(String buildName, boolean deleteArtifacts, BasicStatusHolder multiStatusHolder) {
        if (deleteArtifacts) {
            Set<BuildRun> existingBuilds = searchBuildsByName(buildName);
            for (BuildRun existingBuild : existingBuilds) {
                removeBuildArtifacts(existingBuild, multiStatusHolder);
            }
        }
        buildStoreService.deleteAllBuilds(buildName);
    }

    @Override
    public void deleteBuild(BuildRun buildRun, boolean deleteArtifacts, BasicStatusHolder multiStatusHolder) {
        multiStatusHolder.debug("Starting to remove build '" + buildRun.getName() +
                "' #" + buildRun.getNumber(), log);
        String buildName = buildRun.getName();
        if (deleteArtifacts) {
            removeBuildArtifacts(buildRun, multiStatusHolder);
        }
        buildStoreService.deleteBuild(buildName, buildRun.getNumber(), buildRun.getStarted());
        Set<BuildRun> remainingBuilds = searchBuildsByName(buildName);
        if (remainingBuilds.isEmpty()) {
            deleteBuild(buildName, false, multiStatusHolder);
        }
        multiStatusHolder.debug("Finished removing build '" + buildRun.getName() +
                "' #" + buildRun.getNumber(), log);
    }

    private void removeBuildArtifacts(BuildRun buildRun, BasicStatusHolder status) {
        String buildName = buildRun.getName();
        String buildNumber = buildRun.getNumber();
        Build build = getBuild(buildRun);
        status.debug("Starting to remove the artifacts of build '" + buildName + "' #" + buildNumber, log);
        Map<Artifact, FileInfo> buildArtifactsInfo = getBuildArtifactsInfo(build, StringUtils.EMPTY);
        for (FileInfo fileInfo : buildArtifactsInfo.values()) {
            RepoPath repoPath = fileInfo.getRepoPath();
            BasicStatusHolder undeployStatus = repositoryService.undeploy(repoPath, true, true);
            status.merge(undeployStatus);
        }
        status.debug("Finished removing the artifacts of build '" + buildName + "' #" + buildNumber, log);
    }

    @Override
    public Build getLatestBuildByNameAndStatus(String buildName, final String buildStatus) {
        if (StringUtils.isBlank(buildName)) {
            return null;
        }
        if (StringUtils.isBlank(buildStatus)) {
            return null;
        }
        //let's find all builds
        Set<BuildRun> buildsByName = searchBuildsByName(buildName);
        if (buildsByName == null || buildsByName.isEmpty()) { //no builds - no glory
            return null;
        }
        List<BuildRun> buildRuns = newArrayList(buildsByName);
        Collections.sort(buildRuns, BuildRunComparators.getComparatorFor(buildRuns));
        BuildRun latestBuildRun;

        if (buildStatus.equals(LATEST_BUILD)) {
            latestBuildRun = getLast(buildRuns, null);
        } else {
            latestBuildRun = getLast(filter(buildRuns, new Predicate<BuildRun>() {
                @Override
                public boolean apply(BuildRun buildRun) {
                    // Search for the latest build by the given status
                    return buildStatus.equals(buildRun.getReleaseStatus());
                }
            }), null);

        }
        return latestBuildRun == null ? null :
                getBuild(latestBuildRun);
    }

    @Override
    public
    @Nullable
    Build getLatestBuildByNameAndNumber(String buildName, String buildNumber) {
        if (StringUtils.isBlank(buildName)) {
            return null;
        }
        return buildStoreService.getLatestBuild(buildName, buildNumber);
    }

    @Override
    public Set<BuildRun> searchBuildsByName(String buildName) {
        return buildStoreService.findBuildsByName(buildName);
    }

    @Override
    public List<String> getBuildNames() {
        List<String> allBuildNames = buildStoreService.getAllBuildNames();
        Collections.sort(allBuildNames, null);
        return allBuildNames;
    }

    @Override
    public List<BuildRun> getAllPreviousBuilds(String buildName, String buildNumber, String buildStarted) {
        final BuildRun currentBuildRun = getTransactionalMe().getBuildRun(buildName, buildNumber, buildStarted);
        Set<BuildRun> buildRuns = searchBuildsByName(buildName);
        final Comparator<BuildRun> buildNumberComparator = BuildRunComparators.getBuildStartDateComparator();
        Iterables.removeIf(buildRuns, new Predicate<BuildRun>() {
            @Override
            public boolean apply(@Nullable BuildRun input) {
                // Remove all builds equals or after the current one
                return buildNumberComparator.compare(currentBuildRun, input) <= 0;
            }
        });

        List<BuildRun> buildRunsList = Lists.newArrayList(buildRuns);
        Comparator<BuildRun> reverseComparator = Collections.reverseOrder(buildNumberComparator);
        Collections.sort(buildRunsList, reverseComparator);

        return buildRunsList;
    }

    @Override
    public Set<BuildRun> searchBuildsByNameAndNumber(String buildName, String buildNumber) {
        return buildStoreService.findBuildsByNameAndNumber(buildName, buildNumber);
    }

    @Override
    public Map<Artifact, FileInfo> getNonStrictBuildArtifactsInfo(Build build, String sourceRepository) {
        AqlBase.AndClause<AqlApiBuild> and = AqlApiBuild.and(
                AqlApiBuild.name().equal(build.getName()),
                AqlApiBuild.number().equal(build.getNumber())
        );
        if (StringUtils.isNotBlank(sourceRepository)) {
            and.append(
                    AqlApiBuild.module().artifact().item().repo().equal(sourceRepository)
            );
        }

        AqlApiBuild aqlBuild = AqlApiBuild.create().filter(and);

        aqlBuild.include(
                AqlApiBuild.module().artifact().item().sha1Actual(),
                AqlApiBuild.module().artifact().item().md5Actual(),
                AqlApiBuild.module().artifact().item().sha1Orginal(),
                AqlApiBuild.module().artifact().item().md5Orginal(),
                AqlApiBuild.module().artifact().item().created(),
                AqlApiBuild.module().artifact().item().modifiedBy(),
                AqlApiBuild.module().artifact().item().createdBy(),
                AqlApiBuild.module().artifact().item().updated(),
                AqlApiBuild.module().artifact().item().repo(),
                AqlApiBuild.module().artifact().item().path(),
                AqlApiBuild.module().artifact().item().size(),
                AqlApiBuild.module().artifact().item().name()
                //Ordering by the last updated field, in case of duplicates with the same checksum.
        ).sortBy(AqlFieldEnum.itemUpdated).asc();

        Map<String, BuildFileBean> buildFileBeans = getBuildFileBeans(build, true);
        return matchBeanToFileInfo(aqlBuild, buildFileBeans);
    }

    @Override
    public Map<Artifact, FileInfo> getBuildArtifactsInfo(Build build, String sourceRepository) {
        AqlBase.AndClause and = and();
        if (StringUtils.isNotBlank(sourceRepository)) {
            and.append(
                    AqlApiItem.repo().equal(sourceRepository)
            );
        }
        and.append(AqlApiItem.property().property("build.name", AqlComparatorEnum.equals, build.getName()));
        and.append(AqlApiItem.property().property("build.number", AqlComparatorEnum.equals, build.getNumber()));
        AqlBase aqlItem = AqlApiItem.create().filter(and);

        //Ordering by the last updated field, in case of duplicates with the same checksum.
        aqlItem.sortBy(AqlFieldEnum.itemUpdated).asc();

        Map<String, BuildFileBean> buildFileBeans = getBuildFileBeans(build, true);
        return matchBeanToFileInfo(aqlItem, buildFileBeans);
    }

    @Override
    public Map<Dependency, FileInfo> getBuildDependenciesInfo(Build build, String sourceRepository) {
        AqlBase.AndClause<AqlApiBuild> and = AqlApiBuild.and(
                AqlApiBuild.name().equal(build.getName()),
                AqlApiBuild.number().equal(build.getNumber())
        );
        if (StringUtils.isNotBlank(sourceRepository)) {
            and.append(
                    AqlApiBuild.module().dependecy().item().repo().equal(sourceRepository)
            );
        }

        AqlApiBuild aqlBuild = AqlApiBuild.create().filter(and);

        aqlBuild.include(
                AqlApiBuild.module().dependecy().item().sha1Actual(),
                AqlApiBuild.module().dependecy().item().md5Actual(),
                AqlApiBuild.module().dependecy().item().sha1Orginal(),
                AqlApiBuild.module().dependecy().item().md5Orginal(),
                AqlApiBuild.module().dependecy().item().created(),
                AqlApiBuild.module().dependecy().item().modifiedBy(),
                AqlApiBuild.module().dependecy().item().createdBy(),
                AqlApiBuild.module().dependecy().item().updated(),
                AqlApiBuild.module().dependecy().item().repo(),
                AqlApiBuild.module().dependecy().item().path(),
                AqlApiBuild.module().dependecy().item().name(),
                AqlApiBuild.module().dependecy().item().size()
                //Ordering by the last updated field, in case of duplicates with the same checksum.
        ).sortBy(AqlFieldEnum.itemUpdated).asc();

        Map<String, BuildFileBean> buildFileBeans = getBuildFileBeans(build, false);
        return matchBeanToFileInfo(aqlBuild, buildFileBeans);
    }

    private <T extends BuildFileBean> Map<T, FileInfo> matchBeanToFileInfo(AqlBase aqlBuild,
            Map<String, BuildFileBean> buildFileBeans) {
        Map<T, FileInfo> foundResults = new HashMap<>();

        AqlEagerResult<AqlBuild> aqlResult = aqlService.executeQueryEager(aqlBuild);
        for (AqlBuild item : aqlResult.getResults()) {
            BuildFileBean buildFileBean = buildFileBeans.get(((AqlBaseFullRowImpl) item).getActualSha1());
            if (buildFileBean != null) {
                foundResults.put((T) buildFileBean,
                        (FileInfo) AqlConverts.toFileInfo.apply((AqlBaseFullRowImpl) item));
            }
        }

        return foundResults;
    }

    private <T extends BuildFileBean> Map<String, T> getBuildFileBeans(Build build, boolean artifacts) {
        Map<String, T> beansMap = Maps.newHashMap();
        List<Module> modules = build.getModules();
        if (modules == null) {
            return beansMap;
        }

        for (Module module : modules) {
            if (artifacts) {
                if (module.getArtifacts() != null) {
                    for (Artifact artifact : module.getArtifacts()) {
                        if (artifact.getSha1() != null) {
                            beansMap.put(artifact.getSha1(), (T) artifact);
                        } else {
                            log.warn("Artifact: " + artifact.getName() + " is missing SHA1," +
                                    " under build: " + build.getName());
                        }
                    }
                }
            } else { // dependencies
                if (module.getDependencies() != null) {
                    for (Dependency dependency : module.getDependencies()) {
                        if (dependency.getSha1() != null) {
                            beansMap.put(dependency.getSha1(), (T) dependency);
                        } else {
                            log.warn("Dependency: " + dependency.getId() + " is missing SHA1," +
                                    " under build: " + build.getName());
                        }
                    }
                }
            }
        }
        return beansMap;
    }

    @Override
    public void exportTo(ExportSettings settings) {
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();
        multiStatusHolder.debug("Starting build info export", log);

        File buildsFolder = new File(settings.getBaseDir(), BUILDS_EXPORT_DIR);
        prepareBuildsFolder(settings, multiStatusHolder, buildsFolder);
        if (multiStatusHolder.isError()) {
            return;
        }

        try {
            long exportedBuildCount = 1;
            List<String> buildNames = buildStoreService.getAllBuildNames();
            for (String buildName : buildNames) {
                Set<BuildRun> buildsByName = buildStoreService.findBuildsByName(buildName);
                for (BuildRun buildRun : buildsByName) {
                    String buildNumber = buildRun.getNumber();
                    try {
                        exportBuild(settings, buildRun, exportedBuildCount, buildsFolder);
                        exportedBuildCount++;
                    } catch (Exception e) {
                        String errorMessage = String.format("Failed to export build info: %s:%s", buildName,
                                buildNumber);
                        if (settings.isFailFast()) {
                            throw new Exception(errorMessage, e);
                        }
                        multiStatusHolder.error(errorMessage, e, log);
                    }
                }
            }
        } catch (Exception e) {
            multiStatusHolder.error("Error occurred during build info export.", e, log);
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
                multiStatusHolder.error("Failed to clean previous builds backup folder.", e, log);
            }
        }

        multiStatusHolder.debug("Finished build info export", log);
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
                    multiStatusHolder.error(
                            "Failed to create incremental builds temp backup dir: " + tempBuildBackupDir, e, log);
                }
            }
        } else {
            try {
                FileUtils.forceMkdir(buildsFolder);
            } catch (IOException e) {
                multiStatusHolder.error("Failed to create builds backup dir: " + buildsFolder, e, log);
            }
        }
    }

    @Override
    public void importFrom(ImportSettings settings) {
        final MutableStatusHolder multiStatusHolder = settings.getStatusHolder();
        multiStatusHolder.status("Starting build info import", log);

        dbService.invokeInTransaction("BuildImport-deleteAllBuilds", new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    // delete all existing builds
                    buildStoreService.deleteAllBuilds();
                } catch (Exception e) {
                    multiStatusHolder.error("Failed to delete builds root node", e, log);
                }
                return null;
            }
        });

        File buildsFolder = new File(settings.getBaseDir(), BUILDS_EXPORT_DIR);
        String buildsFolderPath = buildsFolder.getPath();
        if (!buildsFolder.exists()) {
            multiStatusHolder.status("'" + buildsFolderPath + "' folder is either non-existent or not a " +
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

        Collection<File> buildExportFiles =
                FileUtils.listFiles(buildsFolder, buildExportFileFilter, DirectoryFileFilter.DIRECTORY);

        if (buildExportFiles.isEmpty()) {
            multiStatusHolder.status("'" + buildsFolderPath + "' folder does not contain build export files. " +
                    "Build info import was not performed", log);
            return;
        }

        importBuildFiles(settings, buildExportFiles);
        multiStatusHolder.status("Finished build info import", log);
    }

    @Override
    public void importBuild(ImportSettings settings, ImportableExportableBuild build) throws Exception {
        String buildName = build.getBuildName();
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();
        String buildNumber = build.getBuildNumber();
        String buildStarted = build.getBuildStarted();
        try {
            multiStatusHolder.debug(
                    String.format("Beginning import of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);
            buildStoreService.addBuild(build.getJson());
        } catch (Exception e) {
            String msg = "Could not import build " + buildName + ":" + buildNumber + ":" + buildStarted;
            // Print stack trace in debug
            log.debug(msg, e);
            multiStatusHolder.error(msg, e, log);
        }
        multiStatusHolder.debug(
                String.format("Finished import of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);
    }

    @Override
    public Set<String> findScopes(Build build) {
        final Set<String> scopes = Sets.newHashSet();
        if (build.getModules() != null) {
            for (Module module : build.getModules()) {
                if (module.getDependencies() != null) {
                    for (Dependency dependency : module.getDependencies()) {
                        Set<String> dependencyScopes = dependency.getScopes();
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
                    !"gradle".equalsIgnoreCase(buildAgentName) && !"MSBuild".equalsIgnoreCase(buildAgentName);
        }

        BuildType type = build.getType();
        return BuildType.ANT.equals(type) || BuildType.GENERIC.equals(type);
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
                getTransactionalMe().renameBuild(buildToRename, to);
                log.info("Renamed build number '{}' that started at '{}' from '{}' to '{}'.", new String[]{
                        buildToRename.getNumber(), buildToRename.getStarted(), buildToRename.getName(), to});
            } catch (Exception e) {
                log.error("Failed to rename build: '{}' #{} that started at {}.", new String[]{buildToRename.getName(),
                        buildToRename.getNumber(), buildToRename.getStarted()});
            }
        }
    }

    @Override
    public void renameBuild(BuildRun buildRun, String to) {
        Build build = buildStoreService.getBuildJson(buildRun);
        if (build == null) {
            throw new StorageException("Cannot rename non existent build " + buildRun);
        }
        boolean changed = false;
        if (!StringUtils.equals(build.getName(), to)) {
            build.setName(to);
            changed = true;
        }
        if (!StringUtils.equals(buildRun.getName(), to)) {
            changed = true;
        }
        if (!changed) {
            log.info("Build " + buildRun + " already named " + to + " nothing to do!");
        }
        buildStoreService.renameBuild(buildRun, build, authorizationService.currentUsername());
    }

    @Override
    //Deletes the existing build (which has the same name, number and started) and adds the new one instead.
    public void updateBuild(@Nonnull DetailedBuildRun detailedBuildRun) {
        //getTransactionalMe().updateBuild(((DetailedBuildRunImpl) detailedBuildRun).build, true);
        BasicStatusHolder status = new BasicStatusHolder();

        //Reaching update implies the same build (name, number and started) already exists
        log.info("Updating build {} Number {} that started at {}", detailedBuildRun.getName(),
                detailedBuildRun.getNumber(), detailedBuildRun.getStarted(), log);
        log.debug("Deleting build {} : {} : {}", detailedBuildRun.getName(), detailedBuildRun.getNumber(),
                detailedBuildRun.getStarted(), log);
        getTransactionalMe().deleteBuild(detailedBuildRun, false, status);
        if (status.hasErrors()) {
            log.error(status.getLastError().toString(), log);
        }
        log.debug("Adding new build {} : {} : {}", detailedBuildRun.getName(), detailedBuildRun.getNumber(),
                detailedBuildRun.getStarted(), log);
        getTransactionalMe().addBuild(detailedBuildRun);

        log.info("Update of build {} Number {} that started at {} completed successfully", detailedBuildRun.getName(),
                detailedBuildRun.getNumber(), detailedBuildRun.getStarted(), log);
    }

    @Override
    public void updateBuild(final Build build, boolean updateChecksumProperties) {
    }

    @Override
    public void addPromotionStatus(Build build, PromotionStatus promotion) {
        buildStoreService.addPromotionStatus(build, promotion, authorizationService.currentUsername());
    }

    private void exportBuild(ExportSettings settings, BuildRun buildRun,
            long exportedBuildCount, File buildsFolder) throws Exception {
        MutableStatusHolder multiStatusHolder = settings.getStatusHolder();

        String buildName = buildRun.getName();
        String buildNumber = buildRun.getNumber();
        String buildStarted = buildRun.getStarted();
        multiStatusHolder.debug(
                String.format("Beginning export of build: %s:%s:%s", buildName, buildNumber, buildStarted), log);

        ImportableExportableBuild exportedBuild = buildStoreService.getExportableBuild(buildRun);

        File buildFile = new File(buildsFolder, "build" + Long.toString(exportedBuildCount) + ".xml");
        exportBuildToFile(exportedBuild, buildFile);

        multiStatusHolder.debug(
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
                settings.getStatusHolder().error("Error occurred during build info import", e, log);
                if (settings.isFailFast()) {
                    break;
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
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
}
