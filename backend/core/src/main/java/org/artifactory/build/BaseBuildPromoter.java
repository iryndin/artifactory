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
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.PropertiesAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MoveMultiStatusHolder;
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
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.release.Promotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
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
     * @param build             Build info to collect from
     * @param promotion
     * @param strictMatching
     * @param multiStatusHolder Status holder
     * @return Item repo paths
     */
    protected Set<RepoPath> collectItems(Build build, Promotion promotion, boolean strictMatching,
            BasicStatusHolder multiStatusHolder) {
        Set<RepoPath> itemsToMove = Sets.newHashSet();

        if (noModules(build)) {
            return itemsToMove;
        }

        //Artifacts should be collected
        if (promotion.isArtifacts()) {
            Map<org.jfrog.build.api.Artifact, FileInfo> buildArtifactsInfo = buildService.getBuildArtifactsInfo(build,
                    promotion.getSourceRepo());
            String errorMessage = "Unable to find artifacts of build '" + build.getName() + "' #" + build.getNumber();
            validateFullPromote(build, multiStatusHolder, buildArtifactsInfo, true);

            if (!multiStatusHolder.getErrors().isEmpty()) {
                if (promotion.isFailFast()) {
                    throw new ItemNotFoundRuntimeException(errorMessage + ": aborting promotion.");
                }
            }
            for (FileInfo fileInfo : buildArtifactsInfo.values()) {
                itemsToMove.add(fileInfo.getRepoPath());
            }
        }

        //Build dependencies should be collected
        if (promotion.isDependencies()) {
            Map<Dependency, FileInfo> buildDependenciesInfo = buildService.getBuildDependenciesInfo(build,
                    promotion.getSourceRepo());
            validateFullPromote(build, multiStatusHolder, buildDependenciesInfo, false);
            for (Map.Entry<Dependency, FileInfo> entry : buildDependenciesInfo.entrySet()) {
                Dependency dependency = entry.getKey();
                if (dependency != null) {
                    Set<String> dependencyScopes = dependency.getScopes();
                    //Scopes of dependencies to collect
                    if (org.artifactory.util.CollectionUtils.isNullOrEmpty(
                            promotion.getScopes()) || (dependencyScopes != null &&
                            CollectionUtils.containsAny(dependencyScopes, promotion.getScopes()))) {
                        itemsToMove.add(entry.getValue().getRepoPath());
                    }
                }
            }
        }

        return itemsToMove;
    }

    private <T extends BuildFileBean> void validateFullPromote(Build build, BasicStatusHolder multiStatusHolder,
            Map<T, FileInfo> buildArtifactsInfo,
            boolean artifacts) {
        List<BuildFileBean> buildBeans = Lists.newArrayList();
        for (Module module : build.getModules()) {
            if (artifacts) {
                if (module.getArtifacts() != null) {
                    buildBeans.addAll(module.getArtifacts());
                }
            } else {
                if (module.getDependencies() != null) {
                    buildBeans.addAll(module.getDependencies());
                }
            }
        }

        for (final BuildFileBean buildBean : buildBeans) {
            Iterable<T> filter = Iterables.filter(buildArtifactsInfo.keySet(),
                    new Predicate<BuildFileBean>() {
                        @Override
                        public boolean apply(BuildFileBean input) {
                            return buildBean.getMd5().equals(input.getMd5()) &&
                                    buildBean.getSha1().equals(input.getSha1());
                        }
                    });

            if (Iterables.isEmpty(filter)) {
                if (buildBean instanceof org.jfrog.build.api.Artifact) {
                    multiStatusHolder.error(
                            "Unable to find artifact '" + ((org.jfrog.build.api.Artifact) buildBean).getName() + "' of build '" +
                                    build.getName() + "' #" + build.getNumber(), log);
                }
                if (buildBean instanceof org.jfrog.build.api.Dependency) {
                    multiStatusHolder.error(
                            "Unable to find dependency '" + ((org.jfrog.build.api.Dependency) buildBean).getId() + "' of build '" +
                                    build.getName() + "' #" + build.getNumber(), log);
                }
            }
        }
    }

    private boolean noModules(Build build) {
        if (build == null) {
            return true;
        }

        return build.getModules() == null;
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

    protected void tagBuildItemsWithProperties(Set<RepoPath> itemsToTag, Properties properties, boolean failFast,
            boolean dryRun, BasicStatusHolder multiStatusHolder) {
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
