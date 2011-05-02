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

package org.artifactory.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.RepoLayoutBuilder;
import org.artifactory.util.layouts.token.BaseTokenFilter;
import org.artifactory.util.layouts.token.OrganizationPathTokenFilter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Noam Y. Tenne
 */
public abstract class RepoLayoutUtils {

    /**
     * LAYOUT PRESETS
     */
    public static final String MAVEN_2_DEFAULT_NAME = "maven-2-default";
    public static final String IVY_DEFAULT_NAME = "ivy-default";
    public static final String GRADLE_DEFAULT_NAME = "gradle-default";
    public static final String MAVEN_1_DEFAULT_NAME = "maven-1-default";

    public static final RepoLayout MAVEN_2_DEFAULT = new RepoLayoutBuilder()
            .name(MAVEN_2_DEFAULT_NAME)
            .artifactPathPattern("[orgPath]/[module]/[baseRev](-[folderItegRev])/" +
                    "[module]-[baseRev](-[fileItegRev])(-[classifier]).[ext]")
            .distinctiveDescriptorPathPattern(true)
            .descriptorPathPattern("[orgPath]/[module]/[baseRev](-[folderItegRev])/" +
                    "[module]-[baseRev](-[fileItegRev])(-[classifier]).pom")
            .folderIntegrationRevisionRegExp("SNAPSHOT")
            .fileIntegrationRevisionRegExp("SNAPSHOT|(?:(?:[0-9]{8}.[0-9]{6})-(?:[0-9]+))")
            .build();
    public static final RepoLayout IVY_DEFAULT = new RepoLayoutBuilder()
            .name(IVY_DEFAULT_NAME)
            .artifactPathPattern("[org]/[module]/[baseRev](-[folderItegRev])/[type]s/" +
                    "[module](-[classifier])-[baseRev](-[fileItegRev]).[ext]")
            .distinctiveDescriptorPathPattern(true)
            .descriptorPathPattern("[org]/[module]/[baseRev](-[folderItegRev])/[type]s/" +
                    "ivy-[baseRev](-[fileItegRev]).xml")
            .folderIntegrationRevisionRegExp("\\d{14}")
            .fileIntegrationRevisionRegExp("\\d{14}")
            .build();
    public static final RepoLayout GRADLE_DEFAULT = new RepoLayoutBuilder()
            .name(GRADLE_DEFAULT_NAME)
            .artifactPathPattern("[org]/[module]/[baseRev](-[folderItegRev])/" +
                    "[module]-[baseRev](-[fileItegRev])(-[classifier]).[ext]")
            .distinctiveDescriptorPathPattern(true)
            .descriptorPathPattern("[org]/[module]/ivy-[baseRev](-[fileItegRev]).xml")
            .folderIntegrationRevisionRegExp("\\d{14}")
            .fileIntegrationRevisionRegExp("\\d{14}")
            .build();
    public static final RepoLayout MAVEN_1_DEFAULT = new RepoLayoutBuilder()
            .name(MAVEN_1_DEFAULT_NAME)
            .artifactPathPattern("[org]/[type]s/[module]-[baseRev](-[fileItegRev])" +
                    "(-[classifier]).[ext]")
            .distinctiveDescriptorPathPattern(true)
            .descriptorPathPattern("[org]/[type]s/[module]-[baseRev](-[fileItegRev]).pom")
            .folderIntegrationRevisionRegExp(".+")
            .fileIntegrationRevisionRegExp(".+")
            .build();

    /**
     * LAYOUT TOKENS
     */
    public static final String ORGANIZATION = "org";
    public static final String ORGANIZATION_PATH = "orgPath";
    public static final String MODULE = "module";
    public static final String BASE_REVISION = "baseRev";
    public static final String FOLDER_INTEGRATION_REVISION = "folderItegRev";
    public static final String FILE_INTEGRATION_REVISION = "fileItegRev";
    public static final String CLASSIFIER = "classifier";
    public static final String EXT = "ext";
    public static final String TYPE = "type";

    public static final Set<String> TOKENS = Sets.newHashSet(ORGANIZATION, ORGANIZATION_PATH, MODULE, BASE_REVISION,
            FOLDER_INTEGRATION_REVISION, FILE_INTEGRATION_REVISION, CLASSIFIER, EXT, TYPE);

    public static final Map<String, BaseTokenFilter> TOKEN_FILTERS;

    private static final Pattern OPTIONAL_AREA_PATTERN = Pattern.compile("\\([^\\(]*\\)");
    private static final Pattern REPLACED_OPTIONAL_TOKEN_PATTERN = Pattern.compile("\\([^\\[\\(]*\\)");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\[[^\\[]*\\]");

    static {
        Map<String, BaseTokenFilter> temp = Maps.newHashMap();
        temp.put(RepoLayoutUtils.ORGANIZATION_PATH, OrganizationPathTokenFilter.getInstance());
        TOKEN_FILTERS = Collections.unmodifiableMap(temp);
    }

    private RepoLayoutUtils() {
    }

    public static boolean isReservedName(String layoutName) {
        return MAVEN_2_DEFAULT_NAME.equals(layoutName) ||
                IVY_DEFAULT_NAME.equals(layoutName) ||
                GRADLE_DEFAULT_NAME.equals(layoutName) ||
                MAVEN_1_DEFAULT_NAME.equals(layoutName);
    }

    public static boolean isDefaultM2(RepoLayout repoLayout) {
        return MAVEN_2_DEFAULT.equals(repoLayout);
    }

    public static boolean canCrossLayouts(RepoLayout source, RepoLayout target) {
        return (source != null) && (target != null) && !source.equals(target);
    }

    /**
     * Indicates whether the given layout contains the orgPath token, equal to Ivy's M2 compatibility,
     *
     * @param repoLayout Layout to check
     * @return True if the layout contains the orgPath token
     */
    public static boolean layoutContainsOrgPathToken(RepoLayout repoLayout) {
        if (repoLayout == null) {
            throw new IllegalArgumentException("Cannot check a null layout for token existence.");
        }

        String artifactPathPattern = repoLayout.getArtifactPathPattern();
        String descriptorPathPattern = repoLayout.getDescriptorPathPattern();

        return ((artifactPathPattern != null) && artifactPathPattern.contains(ORGANIZATION_PATH)) ||
                (repoLayout.isDistinctiveDescriptorPathPattern() && (descriptorPathPattern != null) &&
                        descriptorPathPattern.contains(ORGANIZATION_PATH));
    }

    /**
     * Find all optional areas that their token values were provided and remove the "optional" brackets that
     * surround them.
     *
     * @param itemPathTemplate     Item path template to modify
     * @param removeBracketContent True if the content of the optional bracket should be disposed
     * @return Modified item path template
     */
    public static String removeReplacedTokenOptionalBrackets(String itemPathTemplate, boolean removeBracketContent) {
        Matcher matcher = REPLACED_OPTIONAL_TOKEN_PATTERN.matcher(itemPathTemplate);

        int latestGroupEnd = 0;
        StringBuilder newPathBuilder = new StringBuilder();

        while (matcher.find()) {
            int replacedOptionalTokenAreaStart = matcher.start();
            int replacedOptionalTokenAreaEnd = matcher.end();
            String replacedOptionalTokenValue = matcher.group(0);

            newPathBuilder.append(itemPathTemplate.substring(latestGroupEnd, replacedOptionalTokenAreaStart));

            if (!removeBracketContent) {
                newPathBuilder.append(replacedOptionalTokenValue.replaceAll("[\\(\\)]", ""));
            }

            //Path after optional area
            latestGroupEnd = replacedOptionalTokenAreaEnd;
        }

        if ((latestGroupEnd != 0) && latestGroupEnd < itemPathTemplate.length()) {
            newPathBuilder.append(itemPathTemplate.substring(latestGroupEnd));
        }

        if (newPathBuilder.length() == 0) {
            return itemPathTemplate;
        }

        return newPathBuilder.toString();
    }

    /**
     * Find all remaining optional areas that were left with un-replaced tokens and remove them completely
     *
     * @param itemPathTemplate Item path template to modify
     * @return Modified item path template
     */
    public static String removeUnReplacedTokenOptionalBrackets(String itemPathTemplate) {
        Matcher matcher = OPTIONAL_AREA_PATTERN.matcher(itemPathTemplate);

        int latestGroupEnd = 0;
        StringBuilder newPathBuilder = new StringBuilder();

        while (matcher.find()) {
            int optionalAreaStart = matcher.start();
            int optionalAreaEnd = matcher.end();
            String optionalAreaValue = matcher.group(0);

            if (optionalAreaValue.contains("[")) {
                newPathBuilder.append(itemPathTemplate.substring(latestGroupEnd, optionalAreaStart));
                latestGroupEnd = optionalAreaEnd;
            }
        }

        if ((latestGroupEnd != 0) && latestGroupEnd < itemPathTemplate.length()) {
            newPathBuilder.append(itemPathTemplate.substring(latestGroupEnd));
        }

        if (newPathBuilder.length() == 0) {
            return itemPathTemplate;
        }

        return newPathBuilder.toString();
    }

    /**
     * Creates a regular expression based on the given path pattern and layout
     *
     * @param repoLayout          Repo layout to target
     * @param patternToUse        Pattern to translate
     * @param tokenAppearanceList Requires a new empty list to which every token that's located will be added.<br/>
     *                            assuming that no unexpected capturing-groups are created, this serves as an alternative to named groups
     * @return Regular expression of given path
     */
    public static String generateRegExpFromPattern(RepoLayout repoLayout, String patternToUse,
            List<String> tokenAppearanceList) {

        StringBuilder itemPathPatternRegExpBuilder = new StringBuilder();

        String[] itemPathPatternElements = patternToUse.split("[\\[\\]]");
        for (String itemPathPatternElement : itemPathPatternElements) {
            if (isReservedToken(itemPathPatternElement)) {

                /**
                 * If the current token already appears in the expression, give the primary token's location instead of
                 * the full expression
                 */
                if (tokenAppearanceList.contains(itemPathPatternElement)) {
                    itemPathPatternRegExpBuilder.append("\\").
                            append(tokenAppearanceList.indexOf(itemPathPatternElement) + 1);
                } else {
                    //Add the token to the appearance list for later reference
                    tokenAppearanceList.add(itemPathPatternElement);
                    itemPathPatternRegExpBuilder.append("(").append(getTokenRegExp(itemPathPatternElement, repoLayout))
                            .append(")");
                }

            } else {
                appendNonReservedToken(itemPathPatternRegExpBuilder, itemPathPatternElement);
            }
        }

        return itemPathPatternRegExpBuilder.toString();
    }

    /**
     * Indicates whether the given token has a value filter assigned to it
     *
     * @param tokenName Name of token check
     * @return True if the token relies on a filter
     */
    public static boolean tokenHasFilter(String tokenName) {
        return TOKEN_FILTERS.containsKey(tokenName);
    }

    /**
     * Returns the Ivy pattern representation of the layout's artifact patten
     *
     * @param repoLayout Layout to "translate"
     * @return Ivy pattern
     */
    public static String getArtifactLayoutAsIvyPattern(RepoLayout repoLayout) {
        return getItemLayoutAsIvyPattern(repoLayout, false);
    }

    /**
     * Returns the Ivy pattern representation of the layout's descriptor patten
     *
     * @param repoLayout Layout to "translate"
     * @return Ivy pattern
     */
    public static String getDescriptorLayoutAsIvyPattern(RepoLayout repoLayout) {
        return getItemLayoutAsIvyPattern(repoLayout, true);
    }

    /**
     * Wraps the given keyword with the token parentheses ('[', ']')
     *
     * @param keyword Keyword to wrap
     * @return Wrapped keyword
     */
    public static String wrapKeywordAsToken(String keyword) {
        return "[" + keyword + "]";
    }

    /**
     * Indicates whether the compared layouts are fully compatible (don't miss any tokens when crossed)
     *
     * @param first  Layout to compare
     * @param second Layout to compare
     * @return True if no tokens are missed between the layouts
     */
    public static boolean layoutsAreCompatible(RepoLayout first, RepoLayout second) {
        String firstArtifactPathPattern = first.getArtifactPathPattern();
        String secondArtifactPathPattern = second.getArtifactPathPattern();

        if (foundMissingTokens(firstArtifactPathPattern, secondArtifactPathPattern)) {
            return false;
        }
        if (foundMissingTokens(secondArtifactPathPattern, firstArtifactPathPattern)) {
            return false;
        }

        if (first.isDistinctiveDescriptorPathPattern() && second.isDistinctiveDescriptorPathPattern()) {

            String firstDescriptorPathPattern = first.getDescriptorPathPattern();
            String secondDescriptorPathPattern = second.getDescriptorPathPattern();

            if (foundMissingTokens(firstDescriptorPathPattern, secondDescriptorPathPattern)) {
                return false;
            }
            if (foundMissingTokens(secondDescriptorPathPattern, firstDescriptorPathPattern)) {
                return false;
            }
        }
        return true;
    }

    private static void appendNonReservedToken(StringBuilder itemPathPatternRegExpBuilder,
            String itemPathPatternElement) {
        char[] splitPathPatternElement = itemPathPatternElement.toCharArray();
        for (char elementToken : splitPathPatternElement) {
            //Dot and dash are reserved regular expression characters. Escape them
            if (('-' == elementToken) || ('.' == elementToken)) {
                itemPathPatternRegExpBuilder.append("\\");
            }

            itemPathPatternRegExpBuilder.append(elementToken);

            if ('(' == elementToken) {
                itemPathPatternRegExpBuilder.append("?:");
            }

            //Append the '?' character to the end of the parenthesis - optional group
            if (')' == elementToken) {
                itemPathPatternRegExpBuilder.append("?");
            }
        }
    }

    private static boolean isReservedToken(String pathElement) {
        return TOKENS.contains(pathElement);
    }

    private static String getTokenRegExp(String tokenName, RepoLayout repoLayout) {
        if (ORGANIZATION.equals(tokenName)) {
            return "[^/]+?";
        } else if (ORGANIZATION_PATH.equals(tokenName)) {
            return ".+?";
        } else if (MODULE.equals(tokenName)) {
            return "[^/]+";
        } else if (BASE_REVISION.equals(tokenName)) {
            return "[^/]+?";
        } else if (FOLDER_INTEGRATION_REVISION.equals(tokenName)) {
            return repoLayout.getFolderIntegrationRevisionRegExp();
        } else if (FILE_INTEGRATION_REVISION.equals(tokenName)) {
            return repoLayout.getFileIntegrationRevisionRegExp();
        } else if (CLASSIFIER.equals(tokenName)) {
            return "(?:(?!\\d))[^\\./]+";
        } else if (EXT.equals(tokenName)) {
            return "[^\\-/]+";
        } else if (TYPE.equals(tokenName)) {
            return "[^/]+?";
        }

        return null;
    }

    private static String getItemLayoutAsIvyPattern(RepoLayout repoLayout, boolean descriptor) {
        if (repoLayout == null) {
            throw new IllegalArgumentException("Cannot translate a null layout.");
        }

        String layoutToTranslate;

        if (descriptor && repoLayout.isDistinctiveDescriptorPathPattern()) {
            layoutToTranslate = repoLayout.getDescriptorPathPattern();
        } else {
            layoutToTranslate = repoLayout.getArtifactPathPattern();
        }

        String organizationToken = wrapKeywordAsToken("organization");
        layoutToTranslate = layoutToTranslate.replaceAll("\\[" + ORGANIZATION_PATH + "\\]", organizationToken);
        layoutToTranslate = layoutToTranslate.replaceAll("\\[" + ORGANIZATION + "\\]", organizationToken);
        layoutToTranslate = layoutToTranslate.replaceAll("\\[" + BASE_REVISION + "\\]", wrapKeywordAsToken("revision"));
        layoutToTranslate = layoutToTranslate.replaceAll("\\[" + FOLDER_INTEGRATION_REVISION + "\\]", "");
        layoutToTranslate = layoutToTranslate.replaceAll("\\[" + FILE_INTEGRATION_REVISION + "\\]", "");
        layoutToTranslate = removeReplacedTokenOptionalBrackets(layoutToTranslate, true);

        return layoutToTranslate;
    }

    private static boolean foundMissingTokens(String firstPattern, String secondPattern) {
        Matcher firstPathMatcher = TOKEN_PATTERN.matcher(firstPattern);

        while (firstPathMatcher.find()) {
            String token = firstPathMatcher.group(0);
            if (!secondPattern.contains(token)) {

                /**
                 * If the unfound token is orgPath but org is found, or the opposite, don't consider as missing,
                 * they are interchangeable
                 */
                if ((wrapKeywordAsToken(ORGANIZATION_PATH).equals(token) &&
                        secondPattern.contains(wrapKeywordAsToken(ORGANIZATION))) ||
                        (wrapKeywordAsToken(ORGANIZATION).equals(token) &&
                                secondPattern.contains(wrapKeywordAsToken(ORGANIZATION_PATH)))) {
                    continue;
                }
                return true;
            }
        }

        return false;
    }
}
