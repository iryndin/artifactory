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

import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.property.Property;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.FileInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.DoesNotExistException;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class BaseBuildPromoter {

    private static final Logger log = LoggerFactory.getLogger(BaseBuildPromoter.class);

    protected AuthorizationService authorizationService;
    protected InternalBuildService buildService;
    private RepositoryService repositoryService;

    public BaseBuildPromoter() {
        ArtifactoryContext context = ContextHelper.get();
        authorizationService = context.getAuthorizationService();
        buildService = context.beanForType(InternalBuildService.class);
        repositoryService = context.getRepositoryService();
    }

    protected Build getBuild(BuildRun buildRun) {
        return buildService.getBuild(buildRun);
    }

    protected void assertRepoExists(String targetRepoKey) {
        LocalRepoDescriptor targetRepo = repositoryService.localOrCachedRepoDescriptorByKey(targetRepoKey);
        if (targetRepo == null) {
            throw new DoesNotExistException("Cannot find target repository by the key '" + targetRepoKey + "'.");
        }
    }

    /**
     * Collect items to move
     *
     * @param build                 Build info to collect from
     * @param artifacts             True if the build artifacts should be collected
     * @param dependencies          True if the build dependencies should be collected
     * @param scopes                Scopes of dependencies to collect
     * @param failOnMissingArtifact
     * @param strictMatching
     * @param multiStatusHolder     Status holder
     * @return Item repo paths
     */
    protected Set<RepoPath> collectItems(Build build, boolean artifacts, boolean dependencies,
            Collection<String> scopes, boolean failOnMissingArtifact, boolean strictMatching,
            MultiStatusHolder multiStatusHolder) {
        Set<RepoPath> itemsToMove = Sets.newHashSet();

        List<Module> moduleList = getModuleList(build);
        if (moduleList == null) {
            return itemsToMove;
        }

        String buildName = build.getName();
        String buildNumber = build.getNumber();

        for (Module module : moduleList) {

            List<Artifact> artifactList = module.getArtifacts();
            if (artifacts && (artifactList != null)) {
                for (Artifact artifact : artifactList) {
                    handleArtifact(itemsToMove, buildName, buildNumber, artifact, failOnMissingArtifact, strictMatching,
                            multiStatusHolder);
                }
            }

            List<Dependency> dependencyList = module.getDependencies();
            if (dependencies && (dependencyList != null)) {
                for (Dependency dependency : dependencyList) {
                    handleDependency(scopes, itemsToMove, buildName, buildNumber, dependency);
                }
            }
        }

        return itemsToMove;
    }

    private List<Module> getModuleList(Build build) {
        if (build == null) {
            return null;
        }

        List<Module> moduleList = build.getModules();
        if (moduleList == null) {
            return null;
        }

        return moduleList;
    }

    private void handleArtifact(Set<RepoPath> itemsToMove, String buildName, String buildNumber, Artifact artifact,
            boolean failOnMissingArtifact, boolean strictMatching, MultiStatusHolder multiStatusHolder) {
        Set<FileInfo> artifactInfos = locateItems(buildName, buildNumber, artifact, strictMatching);
        if (artifactInfos.isEmpty()) {
            String errorMessage = "Unable to find artifact '" + artifact.getName() +
                    "' of build '" + buildName + "' #" + buildNumber;
            if (failOnMissingArtifact) {
                throw new ItemNotFoundRuntimeException(errorMessage + ": aborting promotion.");
            }
            multiStatusHolder.error(errorMessage, log);
            return;
        }
        itemsToMove.add(artifactInfos.iterator().next().getRepoPath());
    }

    private void handleDependency(Collection<String> scopes, Set<RepoPath> itemsToMove, String buildName,
            String buildNumber, Dependency dependency) {
        List<String> dependencyScopes = dependency.getScopes();
        if (org.artifactory.util.CollectionUtils.isNullOrEmpty(scopes) || (dependencyScopes != null &&
                CollectionUtils.containsAny(dependencyScopes, scopes))) {
            Set<FileInfo> dependencyInfos = locateItems(buildName, buildNumber, dependency, false);
            if (dependencyInfos != null && !dependencyInfos.isEmpty()) {
                itemsToMove.add(dependencyInfos.iterator().next().getRepoPath());
            }
        }
    }

    /**
     * Move items
     *
     * @param itemsToMove   Collection of items to move
     * @param targetRepoKey Key of target repository to move to
     * @param dryRun        True if the action should run dry (simulate)
     * @param failFast      True if the operation should abort upon the first occurring warning or error
     * @return Result status holder
     */
    protected MoveMultiStatusHolder move(Set<RepoPath> itemsToMove, String targetRepoKey, boolean dryRun,
            boolean failFast) {
        return repositoryService.move(itemsToMove, targetRepoKey,
                (Properties) InfoFactoryHolder.get().createProperties(), dryRun, failFast);
    }

    /**
     * Copy items
     *
     * @param itemsToCopy   Collection of items to copy
     * @param targetRepoKey Key of target repository to copy to
     * @param dryRun        True if the action should run dry (simulate)
     * @param failFast      True if the operation should abort upon the first occurring warning or error
     * @return Result status holder
     */
    protected MoveMultiStatusHolder copy(Set<RepoPath> itemsToCopy, String targetRepoKey, boolean dryRun,
            boolean failFast) {
        return repositoryService.copy(itemsToCopy, targetRepoKey,
                (Properties) InfoFactoryHolder.get().createProperties(), dryRun, failFast);
    }

    /**
     * Searches for the physical artifact of the given build file and adds it to the item collection if found
     *
     * @param buildName
     * @param buildNumber
     * @param buildFileBean  Build file to locate
     * @param strictMatching True if the artifact finder should operate in strict mode
     */
    private Set<FileInfo> locateItems(String buildName, String buildNumber, BuildFileBean buildFileBean,
            boolean strictMatching) {
        return buildService.getBuildFileBeanInfo(buildName, buildNumber, buildFileBean, strictMatching);
    }

    protected void tagBuildItemsWithProperties(Set<RepoPath> itemsToTag, Properties properties, boolean failFast,
            boolean dryRun, MultiStatusHolder multiStatusHolder) {
        for (RepoPath itemToTag : itemsToTag) {
            if (!authorizationService.canAnnotate(itemToTag)) {
                multiStatusHolder.warn("User doesn't have permissions to annotate '" + itemToTag + "'", log);
                if (failFast) {
                    return;
                } else {
                    continue;
                }
            }
            if (!dryRun) {
                PropertiesAddon propertiesAddon =
                        ContextHelper.get().beanForType(AddonsManager.class).addonByType(PropertiesAddon.class);
                Multiset<String> keys = properties.keys();
                for (String key : keys) {
                    Set<String> valuesForKey = properties.get(key);
                    Property property = new Property();
                    property.setName(key);
                    String[] values = new String[valuesForKey.size()];
                    valuesForKey.toArray(values);
                    propertiesAddon.addProperty(itemToTag, null, property, values);
                }
            }
        }
    }
}
