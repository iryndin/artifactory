/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.module;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.module.translate.PathTranslationHelper;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.layouts.token.BaseTokenFilter;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Noam Y. Tenne
 */
public abstract class ModuleInfoUtils {

    private static final Logger log = LoggerFactory.getLogger(ModuleInfoUtils.class);

    private ModuleInfoUtils() {
    }

    public static String constructArtifactPath(ModuleInfo moduleInfo, RepoLayout repoLayout) {
        return constructArtifactPath(moduleInfo, repoLayout, true);
    }

    public static String constructArtifactPath(ModuleInfo moduleInfo, RepoLayout repoLayout,
            boolean clearUnReplacedOptionals) {
        return constructItemPath(moduleInfo, repoLayout, false, clearUnReplacedOptionals);
    }

    public static String constructDescriptorPath(ModuleInfo moduleInfo, RepoLayout repoLayout,
            boolean clearUnReplacedOptionals) {
        return constructItemPath(moduleInfo, repoLayout, true, clearUnReplacedOptionals);
    }

    /**
     * Creates a module info object from an <b>artifact</b> path.<br/>
     * <b>NOTE:</b> Unless you've got a really good reason, refrain from using this method directly. Please create a
     * module info using the methods of {@link org.artifactory.repo.Repo} or {@link org.artifactory.api.repo.RepositoryService}
     *
     * @param itemPath   Artifact path (relative to the repository)
     * @param repoLayout Repository layout to use for resolution
     * @return Module info object. When supplied with a path that does not match the layout, validity is unwarranted.
     */
    public static ModuleInfo moduleInfoFromArtifactPath(String itemPath, RepoLayout repoLayout) {
        return moduleInfoFromItemPath(itemPath, repoLayout, false);
    }

    /**
     * Creates a module info object from a <b>descriptor</b> path.<br/>
     * <b>NOTE:</b> Unless you've got a really good reason, refrain from using this method directly. Please create a
     * module info using the methods of {@link org.artifactory.repo.Repo} or {@link org.artifactory.api.repo.RepositoryService}
     *
     * @param itemPath   Descriptor path (relative to the repository)
     * @param repoLayout Repository layout to use for resolution
     * @return Module info object. When supplied with a path that does not match the layout, validity is unwarranted.
     */
    public static ModuleInfo moduleInfoFromDescriptorPath(String itemPath, RepoLayout repoLayout) {
        ModuleInfo moduleInfo = moduleInfoFromItemPath(itemPath, repoLayout, true);
        if (repoLayout.isDistinctiveDescriptorPathPattern() && moduleInfo.isValid() &&
                StringUtils.isBlank(moduleInfo.getExt())) {
            return new ModuleInfoBuilder().organization(moduleInfo.getOrganization()).module(moduleInfo.getModule()).
                    baseRevision(moduleInfo.getBaseRevision()).
                    folderIntegrationRevision(moduleInfo.getFolderIntegrationRevision()).
                    fileIntegrationRevision(moduleInfo.getFileIntegrationRevision()).
                    classifier(moduleInfo.getClassifier()).type(moduleInfo.getType()).
                    ext(PathUtils.getExtension(itemPath)).build();
        }
        return moduleInfo;
    }

    public static String translateArtifactPath(RepoLayout sourceRepoLayout, RepoLayout targetRepoLayout, String path) {
        return translateArtifactPath(sourceRepoLayout, targetRepoLayout, path, null);
    }

    public static String translateArtifactPath(RepoLayout sourceRepoLayout, RepoLayout targetRepoLayout, String path,
            MultiStatusHolder multiStatusHolder) {

        if (!RepoLayoutUtils.canCrossLayouts(sourceRepoLayout, targetRepoLayout)) {
            if (multiStatusHolder != null) {
                multiStatusHolder.setWarning("Unable to translate path '" + path +
                        "': the source and target layouts are either equal or invalid.", log);
            }
            return path;
        }

        return new PathTranslationHelper().translatePath(sourceRepoLayout, targetRepoLayout, path, multiStatusHolder);
    }

    private static String constructItemPath(ModuleInfo moduleInfo, RepoLayout repoLayout, boolean descriptor,
            boolean clearUnReplacedOptionals) {
        if (moduleInfo == null) {
            throw new IllegalArgumentException("Unable to construct a path from a null module info object.");
        }
        if (!moduleInfo.isValid()) {
            throw new IllegalArgumentException("Unable to construct a path from an invalid module info object.");
        }
        if (repoLayout == null) {
            throw new IllegalArgumentException("Unable to construct a path from a null repository layout.");
        }

        String itemPathPattern;
        if (descriptor && repoLayout.isDistinctiveDescriptorPathPattern()) {
            itemPathPattern = repoLayout.getDescriptorPathPattern();
        } else {
            itemPathPattern = repoLayout.getArtifactPathPattern();
        }

        itemPathPattern = replaceDeclaredFields(moduleInfo, itemPathPattern);

        itemPathPattern = RepoLayoutUtils.removeReplacedTokenOptionalBrackets(itemPathPattern, false);

        if (clearUnReplacedOptionals) {
            itemPathPattern = RepoLayoutUtils.removeUnReplacedTokenOptionalBrackets(itemPathPattern);
        }

        return itemPathPattern;
    }

    /**
     * Insert all the given module info values to the pattern
     *
     * @param moduleInfo       Module info to use
     * @param itemPathTemplate Item path template to fill
     * @return Modified item path template
     */
    private static String replaceDeclaredFields(ModuleInfo moduleInfo, String itemPathTemplate) {

        for (String token : RepoLayoutUtils.TOKENS) {
            String wrappedToken = RepoLayoutUtils.wrapKeywordAsToken(token);

            if (StringUtils.contains(itemPathTemplate, wrappedToken)) {

                String tokenValue = getTokenValue(moduleInfo, token);

                if (RepoLayoutUtils.TYPE.equals(token) && StringUtils.isBlank(tokenValue)) {
                    tokenValue = moduleInfo.getExt();
                }

                if (StringUtils.isNotBlank(tokenValue)) {

                    if (RepoLayoutUtils.tokenHasFilter(token)) {
                        BaseTokenFilter tokenFilter = RepoLayoutUtils.TOKEN_FILTERS.get(token);
                        if (tokenFilter != null) {
                            tokenValue = tokenFilter.forPath(tokenValue);
                        }
                    }

                    itemPathTemplate = StringUtils.replace(itemPathTemplate, wrappedToken, tokenValue);
                }
            }
        }
        return itemPathTemplate;
    }

    private static String getTokenValue(ModuleInfo moduleInfo, String tokenName) {
        if (RepoLayoutUtils.ORGANIZATION.equals(tokenName)) {
            return moduleInfo.getOrganization();
        } else if (RepoLayoutUtils.ORGANIZATION_PATH.equals(tokenName)) {
            return moduleInfo.getOrganization();
        } else if (RepoLayoutUtils.MODULE.equals(tokenName)) {
            return moduleInfo.getModule();
        } else if (RepoLayoutUtils.BASE_REVISION.equals(tokenName)) {
            return moduleInfo.getBaseRevision();
        } else if (RepoLayoutUtils.FOLDER_INTEGRATION_REVISION.equals(tokenName)) {
            return moduleInfo.getFolderIntegrationRevision();
        } else if (RepoLayoutUtils.FILE_INTEGRATION_REVISION.equals(tokenName)) {
            return moduleInfo.getFileIntegrationRevision();
        } else if (RepoLayoutUtils.CLASSIFIER.equals(tokenName)) {
            return moduleInfo.getClassifier();
        } else if (RepoLayoutUtils.EXT.equals(tokenName)) {
            return moduleInfo.getExt();
        } else if (RepoLayoutUtils.TYPE.equals(tokenName)) {
            return moduleInfo.getType();
        }

        return null;
    }

    private static ModuleInfo moduleInfoFromItemPath(String itemPath, RepoLayout repoLayout, boolean descriptor) {
        if (StringUtils.isBlank(itemPath)) {
            throw new IllegalArgumentException("Cannot construct a module info object from a blank item path.");
        }
        if (repoLayout == null) {
            throw new IllegalArgumentException("Cannot construct a module info object from a null repository layout.");
        }

        String pattern;
        if (descriptor && repoLayout.isDistinctiveDescriptorPathPattern()) {
            pattern = repoLayout.getDescriptorPathPattern();
        } else {
            pattern = repoLayout.getArtifactPathPattern();
        }

        return moduleInfoFromPattern(itemPath, repoLayout, pattern);
    }

    private static ModuleInfo moduleInfoFromPattern(String itemPath, RepoLayout repoLayout, String pattern) {
        List<String> tokenAppearanceList = Lists.newArrayList();

        String itemPathPatternRegExp = RepoLayoutUtils.generateRegExpFromPattern(repoLayout, pattern,
                tokenAppearanceList);
        //TODO: [by yl] Cache the patterns
        Pattern itemPathRegExPattern = Pattern.compile(itemPathPatternRegExp);
        Matcher itemPathMatcher = itemPathRegExPattern.matcher(itemPath);
        ModuleInfo moduleInfo;
        if (!itemPathMatcher.matches()) {
            moduleInfo = new ModuleInfo();
        } else {
            moduleInfo = createModuleInfo(tokenAppearanceList, itemPathMatcher);
        }
        if (!moduleInfo.isValid()) {
            log.debug("Could not transform {} to a valid module info.", itemPath);
        }
        return moduleInfo;
    }

    private static ModuleInfo createModuleInfo(List<String> tokenAppearanceList, Matcher itemPathMatcher) {
        int itemPathPatternGroupCount = itemPathMatcher.groupCount();

        ModuleInfoBuilder moduleInfoBuilder = new ModuleInfoBuilder();
        for (int i = 0; i < itemPathPatternGroupCount; i++) {
            String groupName = tokenAppearanceList.get(i);
            String groupValue = itemPathMatcher.group(i + 1);

            /**
             * Check if the group name (token name) has a filter registered to it before checking for token aliases
             * since the filter might be registered to the alias
             */
            if (RepoLayoutUtils.tokenHasFilter(groupName)) {
                BaseTokenFilter tokenFilter = RepoLayoutUtils.TOKEN_FILTERS.get(groupName);
                if (tokenFilter != null) {
                    groupValue = tokenFilter.fromPath(groupValue);
                }
            }
            setTokenValue(moduleInfoBuilder, groupName, groupValue);
        }

        return moduleInfoBuilder.build();
    }

    private static void setTokenValue(ModuleInfoBuilder moduleInfoBuilder, String tokenName, String tokenValue) {
        if (RepoLayoutUtils.ORGANIZATION.equals(tokenName)) {
            moduleInfoBuilder.organization(tokenValue);
        } else if (RepoLayoutUtils.ORGANIZATION_PATH.equals(tokenName)) {
            moduleInfoBuilder.organization(tokenValue);
        } else if (RepoLayoutUtils.MODULE.equals(tokenName)) {
            moduleInfoBuilder.module(tokenValue);
        } else if (RepoLayoutUtils.BASE_REVISION.equals(tokenName)) {
            moduleInfoBuilder.baseRevision(tokenValue);
        } else if (RepoLayoutUtils.FOLDER_INTEGRATION_REVISION.equals(tokenName)) {
            moduleInfoBuilder.folderIntegrationRevision(tokenValue);
        } else if (RepoLayoutUtils.FILE_INTEGRATION_REVISION.equals(tokenName)) {
            moduleInfoBuilder.fileIntegrationRevision(tokenValue);
        } else if (RepoLayoutUtils.CLASSIFIER.equals(tokenName)) {
            moduleInfoBuilder.classifier(tokenValue);
        } else if (RepoLayoutUtils.EXT.equals(tokenName)) {
            moduleInfoBuilder.ext(tokenValue);
        } else if (RepoLayoutUtils.TYPE.equals(tokenName)) {
            moduleInfoBuilder.type(tokenValue);
        }
    }
}
