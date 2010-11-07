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

package org.artifactory.build;

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.artifact.ChecksumSearchControls;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.StatusEntry;
import org.artifactory.common.StatusEntryLevel;
import org.artifactory.repo.RepoPath;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;

import java.util.List;
import java.util.Set;

/**
 * A helper class for moving or copying build artifacts and dependencies
 *
 * @author Noam Y. Tenne
 */
public class BuildItemMoveCopyHelper {

    private BuildService buildService;
    private SearchService searchService;
    private RepositoryService repositoryService;

    /**
     * Default constructor
     */
    public BuildItemMoveCopyHelper() {
        ArtifactoryContext context = ContextHelper.get();
        buildService = context.beanForType(BuildService.class);
        searchService = context.beanForType(SearchService.class);
        repositoryService = context.getRepositoryService();
    }

    /**
     * Move or copy build artifacts and\or dependencies
     *
     * @param move           True if the items should be moved. False if they should be copied
     * @param basicBuildInfo Basic info of the selected build
     * @param targetRepoKey  Key of target repository to move to
     * @param artifacts      True if the build artifacts should be moved\copied
     * @param dependencies   True if the build dependencies should be moved\copied
     * @param scopes         Scopes of dependencies to copy (agnostic if null or empty)
     * @param dryRun         True if the action should run dry (simulate)
     * @return Result of action
     */
    public MoveCopyResult moveOrCopy(boolean move, BasicBuildInfo basicBuildInfo, String targetRepoKey,
            boolean artifacts, boolean dependencies, List<String> scopes, boolean dryRun) {
        Build build = buildService.getBuild(basicBuildInfo.getName(), basicBuildInfo.getNumber(),
                basicBuildInfo.getStarted());

        Set<RepoPath> itemsToMove = collectItems(build, artifacts, dependencies, scopes);

        MoveCopyResult result = new MoveCopyResult();

        if (move) {
            try {
                MoveMultiStatusHolder moveMultiStatusHolder = move(itemsToMove, targetRepoKey, dryRun);
                appendMessages(result, moveMultiStatusHolder);
            } catch (Exception e) {
                result.messages.add(new MoveCopyResult.MoveCopyMessages(StatusEntryLevel.ERROR,
                        "Error occurred while moving: " + e.getMessage()));
                return result;
            }
        } else {
            try {
                MoveMultiStatusHolder moveMultiStatusHolder = copy(itemsToMove, targetRepoKey, dryRun);
                appendMessages(result, moveMultiStatusHolder);
            } catch (Exception e) {
                result.messages.add(new MoveCopyResult.MoveCopyMessages(StatusEntryLevel.ERROR,
                        "Error occurred while copying: " + e.getMessage()));
                return result;
            }
        }

        return result;
    }

    /**
     * Collect items to move
     *
     * @param build        Build info to collect from
     * @param artifacts    True if the build artifacts should be collected
     * @param dependencies True if the build dependencies should be collected
     * @param scopes       Scopes of dependencies to collect
     * @return Item repo paths
     */
    private Set<RepoPath> collectItems(Build build, boolean artifacts, boolean dependencies, List<String> scopes) {
        Set<RepoPath> itemsToMove = Sets.newHashSet();

        if (build != null) {
            List<Module> moduleList = build.getModules();
            if (moduleList != null) {
                for (Module module : moduleList) {

                    List<Artifact> artifactList = module.getArtifacts();
                    if (artifacts && (artifactList != null)) {
                        for (Artifact artifact : artifactList) {
                            locateAndAddItem(itemsToMove, artifact);
                        }
                    }

                    List<Dependency> dependencyList = module.getDependencies();
                    if (dependencies && (dependencyList != null)) {
                        for (Dependency dependency : dependencyList) {
                            List<String> dependencyScopes = dependency.getScopes();
                            if (scopes == null || scopes.isEmpty() || (dependencyScopes != null &&
                                    CollectionUtils.containsAny(dependencyScopes, scopes))) {
                                locateAndAddItem(itemsToMove, dependency);
                            }
                        }
                    }
                }
            }
        }

        return itemsToMove;
    }

    /**
     * Searches for the physical artifact of the given build file and adds it to the item collection if found
     *
     * @param itemsToMove   Collection of items
     * @param buildFileBean Build file to locate
     */
    private void locateAndAddItem(Set<RepoPath> itemsToMove, BuildFileBean buildFileBean) {
        ChecksumSearchControls controls = new ChecksumSearchControls();
        controls.addChecksum(ChecksumType.sha1, buildFileBean.getSha1());
        controls.addChecksum(ChecksumType.md5, buildFileBean.getMd5());
        Set<RepoPath> repoPaths = searchService.searchArtifactsByChecksum(controls);
        if (!repoPaths.isEmpty()) {
            itemsToMove.add(repoPaths.iterator().next());
        }
    }

    /**
     * Move items
     *
     * @param itemsToMove   Collection of items to move
     * @param targetRepoKey Key of target repository to move to
     * @param dryRun        True if the action should run dry (simulate)
     * @return Result status holder
     */
    private MoveMultiStatusHolder move(Set<RepoPath> itemsToMove, String targetRepoKey, boolean dryRun) {
        return repositoryService.move(itemsToMove, targetRepoKey, dryRun);
    }

    /**
     * Copy items
     *
     * @param itemsToCopy   Collection of items to copy
     * @param targetRepoKey Key of target repository to copy to
     * @param dryRun        True if the action should run dry (simulate)
     * @return Result status holder
     */
    private MoveMultiStatusHolder copy(Set<RepoPath> itemsToCopy, String targetRepoKey, boolean dryRun) {
        return repositoryService.copy(itemsToCopy, targetRepoKey, dryRun);
    }

    /**
     * Append the status holder messages to the move copy result
     *
     * @param result                Result to append to
     * @param moveMultiStatusHolder Status holder to copy from
     */
    private void appendMessages(MoveCopyResult result, MoveMultiStatusHolder moveMultiStatusHolder) {
        for (StatusEntry statusEntry : moveMultiStatusHolder.getAllEntries()) {
            result.messages.add(new MoveCopyResult.MoveCopyMessages(statusEntry));
        }
    }
}