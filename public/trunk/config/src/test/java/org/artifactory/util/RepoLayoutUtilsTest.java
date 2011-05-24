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

import com.google.common.collect.Lists;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.RepoLayoutBuilder;
import org.artifactory.util.layouts.token.OrganizationPathTokenFilter;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests the repository layouts utility methods and constants
 *
 * @author Noam Y. Tenne
 */
@Test
public class RepoLayoutUtilsTest {

    public void testDefaultLayoutNames() {
        testDefaultLayoutName(RepoLayoutUtils.MAVEN_2_DEFAULT_NAME, "maven-2-default");
        testDefaultLayoutName(RepoLayoutUtils.IVY_DEFAULT_NAME, "ivy-default");
        testDefaultLayoutName(RepoLayoutUtils.GRADLE_DEFAULT_NAME, "gradle-default");
        testDefaultLayoutName(RepoLayoutUtils.MAVEN_1_DEFAULT_NAME, "maven-1-default");
    }

    public void testDefaultLayouts() {
        testDefaultLayout(RepoLayoutUtils.MAVEN_2_DEFAULT,
                "[orgPath]/[module]/[baseRev](-[folderItegRev])/" +
                        "[module]-[baseRev](-[fileItegRev])(-[classifier]).[ext]",
                true,
                "[orgPath]/[module]/[baseRev](-[folderItegRev])/[module]-[baseRev](-[fileItegRev])(-[classifier]).pom",
                "SNAPSHOT", "SNAPSHOT|(?:(?:[0-9]{8}.[0-9]{6})-(?:[0-9]+))");

        testDefaultLayout(RepoLayoutUtils.IVY_DEFAULT,
                "[org]/[module]/[baseRev](-[folderItegRev])/[type]s/" +
                        "[module](-[classifier])-[baseRev](-[fileItegRev]).[ext]",
                true,
                "[org]/[module]/[baseRev](-[folderItegRev])/[type]s/ivy-[baseRev](-[fileItegRev]).xml",
                "\\d{14}", "\\d{14}");

        testDefaultLayout(RepoLayoutUtils.GRADLE_DEFAULT,
                "[org]/[module]/[baseRev](-[folderItegRev])/[module]-[baseRev](-[fileItegRev])(-[classifier]).[ext]",
                true,
                "[org]/[module]/ivy-[baseRev](-[fileItegRev]).xml",
                "\\d{14}", "\\d{14}");

        testDefaultLayout(RepoLayoutUtils.MAVEN_1_DEFAULT,
                "[org]/[type]s/[module]-[baseRev](-[fileItegRev])(-[classifier]).[ext]",
                true,
                "[org]/[type]s/[module]-[baseRev](-[fileItegRev]).pom",
                ".+", ".+");
    }

    public void testDefaultTokenValues() {
        assertEquals(RepoLayoutUtils.ORGANIZATION, "org", "Unexpected default 'organization' token value.");
        assertEquals(RepoLayoutUtils.ORGANIZATION_PATH, "orgPath",
                "Unexpected default 'organization path' token value.");
        assertEquals(RepoLayoutUtils.MODULE, "module", "Unexpected default 'module' token value.");
        assertEquals(RepoLayoutUtils.BASE_REVISION, "baseRev", "Unexpected default 'base revision' token value.");
        assertEquals(RepoLayoutUtils.FOLDER_INTEGRATION_REVISION, "folderItegRev",
                "Unexpected default 'folder integration revision' token value.");
        assertEquals(RepoLayoutUtils.FILE_INTEGRATION_REVISION, "fileItegRev",
                "Unexpected default 'file integration revision' token value.");
        assertEquals(RepoLayoutUtils.CLASSIFIER, "classifier", "Unexpected default 'classifier' token value.");
        assertEquals(RepoLayoutUtils.EXT, "ext", "Unexpected default 'extension' token value.");
        assertEquals(RepoLayoutUtils.TYPE, "type", "Unexpected default 'type' token value.");
    }

    public void testDefaultTokenSet() {
        assertEquals(RepoLayoutUtils.TOKENS.size(), 9, "Unexpected size of layout token set.");
        assertTokenSetContents(RepoLayoutUtils.ORGANIZATION, RepoLayoutUtils.ORGANIZATION_PATH,
                RepoLayoutUtils.MODULE, RepoLayoutUtils.BASE_REVISION, RepoLayoutUtils.FOLDER_INTEGRATION_REVISION,
                RepoLayoutUtils.FILE_INTEGRATION_REVISION, RepoLayoutUtils.CLASSIFIER, RepoLayoutUtils.EXT,
                RepoLayoutUtils.TYPE);
    }

    public void testDefaultTokenFilterMap() {
        assertEquals(RepoLayoutUtils.TOKEN_FILTERS.size(), 1, "Unexpected size of default token filter map.");
        assertTrue(RepoLayoutUtils.TOKEN_FILTERS.containsKey(RepoLayoutUtils.ORGANIZATION_PATH),
                "Default token filter map should contain a filter for 'orgPath'.");
        assertEquals(RepoLayoutUtils.TOKEN_FILTERS.get(RepoLayoutUtils.ORGANIZATION_PATH),
                OrganizationPathTokenFilter.getInstance(), "Unexpected filter found for 'orgPath'.");
    }

    public void testReservedRepoLayoutNames() {
        testReservedRepoLayoutName(RepoLayoutUtils.MAVEN_2_DEFAULT_NAME);
        testReservedRepoLayoutName(RepoLayoutUtils.IVY_DEFAULT_NAME);
        testReservedRepoLayoutName(RepoLayoutUtils.GRADLE_DEFAULT_NAME);
        testReservedRepoLayoutName(RepoLayoutUtils.MAVEN_1_DEFAULT_NAME);
        assertFalse(RepoLayoutUtils.isReservedName("momo"), "Unexpected reserved layout name.");
    }

    public void testIsDefaultM2Layout() {
        assertTrue(RepoLayoutUtils.isDefaultM2(RepoLayoutUtils.MAVEN_2_DEFAULT), "Default M2 layout isn't recognized.");
        assertFalse(RepoLayoutUtils.isDefaultM2(RepoLayoutUtils.IVY_DEFAULT),
                "Default Ivy layout should not be recognized as default M2.");
        assertFalse(RepoLayoutUtils.isDefaultM2(RepoLayoutUtils.GRADLE_DEFAULT),
                "Default Gradle layout should not be recognized as default M2.");
        assertFalse(RepoLayoutUtils.isDefaultM2(RepoLayoutUtils.MAVEN_1_DEFAULT),
                "Default M1 layout should not be recognized as default M2.");
    }

    public void testCanCrossLayouts() {
        assertFalse(RepoLayoutUtils.canCrossLayouts(null, RepoLayoutUtils.MAVEN_2_DEFAULT),
                "Layouts shouldn't be crossable if one of them is null.");
        assertFalse(RepoLayoutUtils.canCrossLayouts(RepoLayoutUtils.MAVEN_2_DEFAULT, null),
                "Layouts shouldn't be crossable if one of them is null.");
        assertFalse(RepoLayoutUtils.canCrossLayouts(RepoLayoutUtils.MAVEN_2_DEFAULT,
                RepoLayoutUtils.MAVEN_2_DEFAULT), "Layouts shouldn't be crossable if they are equal.");
        assertTrue(RepoLayoutUtils.canCrossLayouts(RepoLayoutUtils.MAVEN_2_DEFAULT,
                RepoLayoutUtils.GRADLE_DEFAULT), "Layouts should be crossable if they are different.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "(.*)null layout for token existence(.*)")
    public void testNullLayoutContainsOrgPathToken() {
        RepoLayoutUtils.layoutContainsOrgPathToken(null);
    }

    public void testLayoutContainsOrgPathToken() {
        RepoLayoutBuilder repoLayoutBuilder = new RepoLayoutBuilder().artifactPathPattern(null).descriptorPathPattern(
                null);

        assertFalse(RepoLayoutUtils.layoutContainsOrgPathToken(repoLayoutBuilder.build()),
                "Null paths shouldn't contain the 'orgPath' token.");

        repoLayoutBuilder.artifactPathPattern("[orgPath]");
        assertTrue(RepoLayoutUtils.layoutContainsOrgPathToken(repoLayoutBuilder.build()),
                "Expected to find the 'orgPath' token");

        repoLayoutBuilder.artifactPathPattern(null);
        repoLayoutBuilder.distinctiveDescriptorPathPattern(true);
        repoLayoutBuilder.descriptorPathPattern("[orgPath]");
        assertTrue(RepoLayoutUtils.layoutContainsOrgPathToken(repoLayoutBuilder.build()),
                "Expected to find the 'orgPath' token");

        repoLayoutBuilder.artifactPathPattern("[orgPath]");
        assertTrue(RepoLayoutUtils.layoutContainsOrgPathToken(repoLayoutBuilder.build()),
                "Expected to find the 'orgPath' token");
    }

    public void testRemoveReplacedTokenOptionalBrackets() {
        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("", false), "");
        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("", true), "");

        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("[org]-momo", false), "[org]-momo");
        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("[org]-momo", true), "[org]-momo");

        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("[org](-[momo])", false), "[org](-[momo])");
        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("[org](-[momo])", true), "[org](-[momo])");

        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("[org](-momo)", false), "[org]-momo");
        assertEquals(RepoLayoutUtils.removeReplacedTokenOptionalBrackets("[org](-momo)", true), "[org]");
    }

    public void testRemoveUnReplacedTokenOptionalBrackets() {
        assertEquals(RepoLayoutUtils.removeUnReplacedTokenOptionalBrackets(""), "");

        assertEquals(RepoLayoutUtils.removeUnReplacedTokenOptionalBrackets("[org]-momo"), "[org]-momo");

        assertEquals(RepoLayoutUtils.removeUnReplacedTokenOptionalBrackets("[org](-[momo])"), "[org]");

        assertEquals(RepoLayoutUtils.removeUnReplacedTokenOptionalBrackets("[org](-momo)"), "[org](-momo)");
    }

    public void testGenerateRegExpFromPatternOfDefaultLayouts() {
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT,
                RepoLayoutUtils.MAVEN_2_DEFAULT.getArtifactPathPattern(),
                "(.+?)/([^/]+)/([^/]+?)(?:\\-(SNAPSHOT))?/\\2\\-\\3(?:\\-(SNAPSHOT|(?:(?:[0-9]{8}.[0-9]{6})-" +
                        "(?:[0-9]+))))?(?:\\-((?:(?!\\d))[^\\./]+))?\\.([^\\-/]+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT,
                RepoLayoutUtils.MAVEN_2_DEFAULT.getDescriptorPathPattern(),
                "(.+?)/([^/]+)/([^/]+?)(?:\\-(SNAPSHOT))?/\\2\\-\\3(?:\\-(SNAPSHOT|(?:(?:[0-9]{8}.[0-9]{6})-" +
                        "(?:[0-9]+))))?(?:\\-((?:(?!\\d))[^\\./]+))?\\.pom");

        testGeneratedPatternRegExp(RepoLayoutUtils.IVY_DEFAULT,
                RepoLayoutUtils.IVY_DEFAULT.getArtifactPathPattern(),
                "([^/]+?)/([^/]+)/([^/]+?)(?:\\-(\\d{14}))?/([^/]+?)s/\\2(?:\\-((?:(?!\\d))[^\\./]+))?\\-" +
                        "\\3(?:\\-(\\d{14}))?\\.([^\\-/]+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.IVY_DEFAULT,
                RepoLayoutUtils.IVY_DEFAULT.getDescriptorPathPattern(),
                "([^/]+?)/([^/]+)/([^/]+?)(?:\\-(\\d{14}))?/([^/]+?)s/ivy\\-\\3(?:\\-(\\d{14}))?\\.xml");

        testGeneratedPatternRegExp(RepoLayoutUtils.GRADLE_DEFAULT,
                RepoLayoutUtils.GRADLE_DEFAULT.getArtifactPathPattern(),
                "([^/]+?)/([^/]+)/([^/]+?)(?:\\-(\\d{14}))?/\\2\\-\\3(?:\\-(\\d{14}))?(?:\\-" +
                        "((?:(?!\\d))[^\\./]+))?\\.([^\\-/]+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.GRADLE_DEFAULT,
                RepoLayoutUtils.GRADLE_DEFAULT.getDescriptorPathPattern(),
                "([^/]+?)/([^/]+)/ivy\\-([^/]+?)(?:\\-(\\d{14}))?\\.xml");
    }

    public void testDefaultTokenRegExpValues() {
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.ORGANIZATION, "([^/]+?)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.ORGANIZATION_PATH, "(.+?)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.MODULE, "([^/]+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.BASE_REVISION, "([^/]+?)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.CLASSIFIER,
                "((?:(?!\\d))[^\\./]+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.EXT, "([^\\-/]+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.TYPE, "([^/]+?)");

        //Integration revisions
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.FOLDER_INTEGRATION_REVISION,
                "(SNAPSHOT)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.FILE_INTEGRATION_REVISION,
                "(SNAPSHOT|(?:(?:[0-9]{8}.[0-9]{6})-(?:[0-9]+)))");
        testGeneratedPatternRegExp(RepoLayoutUtils.IVY_DEFAULT, RepoLayoutUtils.FOLDER_INTEGRATION_REVISION,
                "(\\d{14})");
        testGeneratedPatternRegExp(RepoLayoutUtils.IVY_DEFAULT, RepoLayoutUtils.FILE_INTEGRATION_REVISION, "(\\d{14})");
        testGeneratedPatternRegExp(RepoLayoutUtils.GRADLE_DEFAULT, RepoLayoutUtils.FOLDER_INTEGRATION_REVISION,
                "(\\d{14})");
        testGeneratedPatternRegExp(RepoLayoutUtils.GRADLE_DEFAULT, RepoLayoutUtils.FILE_INTEGRATION_REVISION,
                "(\\d{14})");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_1_DEFAULT, RepoLayoutUtils.FOLDER_INTEGRATION_REVISION,
                "(.+)");
        testGeneratedPatternRegExp(RepoLayoutUtils.MAVEN_1_DEFAULT, RepoLayoutUtils.FILE_INTEGRATION_REVISION,
                "(.+)");
    }

    public void testTokenHasFilter() {
        testTokenFilterAssociation(RepoLayoutUtils.ORGANIZATION, false);
        testTokenFilterAssociation(RepoLayoutUtils.ORGANIZATION_PATH, true);
        testTokenFilterAssociation(RepoLayoutUtils.MODULE, false);
        testTokenFilterAssociation(RepoLayoutUtils.BASE_REVISION, false);
        testTokenFilterAssociation(RepoLayoutUtils.FOLDER_INTEGRATION_REVISION, false);
        testTokenFilterAssociation(RepoLayoutUtils.FILE_INTEGRATION_REVISION, false);
        testTokenFilterAssociation(RepoLayoutUtils.CLASSIFIER, false);
        testTokenFilterAssociation(RepoLayoutUtils.EXT, false);
        testTokenFilterAssociation(RepoLayoutUtils.TYPE, false);
    }

    public void testGetArtifactLayoutAsIvyPattern() {
        testArtifactLayoutAsIvyPattern(RepoLayoutUtils.MAVEN_2_DEFAULT, "[organization]/[module]/[revision]/" +
                "[module]-[revision](-[classifier]).[ext]");
        testArtifactLayoutAsIvyPattern(RepoLayoutUtils.IVY_DEFAULT, "[organization]/[module]/[revision]/[type]s/" +
                "[module](-[classifier])-[revision].[ext]");
        testArtifactLayoutAsIvyPattern(RepoLayoutUtils.GRADLE_DEFAULT, "[organization]/[module]/[revision]/" +
                "[module]-[revision](-[classifier]).[ext]");
        testArtifactLayoutAsIvyPattern(RepoLayoutUtils.MAVEN_1_DEFAULT, "[organization]/[type]s/[module]-[revision]" +
                "(-[classifier]).[ext]");
    }

    public void testGetDescriptorLayoutAsIvyPattern() {
        testDescriptorLayoutAsIvyPattern(RepoLayoutUtils.MAVEN_2_DEFAULT, "[organization]/[module]/[revision]/" +
                "[module]-[revision](-[classifier]).pom");
        testDescriptorLayoutAsIvyPattern(RepoLayoutUtils.IVY_DEFAULT, "[organization]/[module]/[revision]/[type]s/" +
                "ivy-[revision].xml");
        testDescriptorLayoutAsIvyPattern(RepoLayoutUtils.GRADLE_DEFAULT, "[organization]/[module]/ivy-[revision].xml");
        testDescriptorLayoutAsIvyPattern(RepoLayoutUtils.MAVEN_1_DEFAULT, "[organization]/[type]s/" +
                "[module]-[revision].pom");
    }

    public void testWrapKeywordAsToken() {
        testWrappedKeywords(null, "[null]");
        testWrappedKeywords("", "[]");
        testWrappedKeywords(" ", "[ ]");

        for (String defaultToken : RepoLayoutUtils.TOKENS) {
            testWrappedKeywords(defaultToken, String.format("[%s]", defaultToken));
        }

        testWrappedKeywords("momo", "[momo]");
        testWrappedKeywords("popo", "[popo]");
    }

    public void testLayoutsAreCompatible() {
        RepoLayout[] repoLayouts = {RepoLayoutUtils.MAVEN_2_DEFAULT, RepoLayoutUtils.IVY_DEFAULT,
                RepoLayoutUtils.GRADLE_DEFAULT, RepoLayoutUtils.MAVEN_1_DEFAULT};

        for (RepoLayout firstLayout : repoLayouts) {

            for (RepoLayout secondLayout : repoLayouts) {

                boolean layoutsAreCompatible = RepoLayoutUtils.layoutsAreCompatible(firstLayout, secondLayout);
                if (firstLayout.equals(secondLayout)) {
                    assertTrue(layoutsAreCompatible, "Expected layouts to be fully compatible.");
                } else {
                    assertFalse(layoutsAreCompatible, "Expected layouts to be incompatible.");
                }
            }
        }
    }

    private void testDefaultLayoutName(String actualName, String expectedName) {
        assertEquals(actualName, expectedName, "Unexpected default layout name.");
    }

    private void testDefaultLayout(RepoLayout repoLayout, String artifactPathPattern,
            boolean distinctiveDescriptorPathPattern, String descriptorPathPattern,
            String folderIntegrationRevisionRegExp, String fileIntegrationRevisionRegExp) {
        assertEquals(repoLayout.getArtifactPathPattern(), artifactPathPattern,
                "Unexpected default artifact path pattern.");
        assertEquals(repoLayout.isDistinctiveDescriptorPathPattern(), distinctiveDescriptorPathPattern,
                "Unexpected default distinctive descriptor path pattern.");
        assertEquals(repoLayout.getDescriptorPathPattern(), descriptorPathPattern,
                "Unexpected default descriptor path pattern.");
        assertEquals(repoLayout.getFolderIntegrationRevisionRegExp(), folderIntegrationRevisionRegExp,
                "Unexpected default folder integration revision regular expression.");
        assertEquals(repoLayout.getFileIntegrationRevisionRegExp(), fileIntegrationRevisionRegExp,
                "Unexpected default filer integration revision regular expression.");
    }

    private void assertTokenSetContents(String... expectedContents) {
        for (String expectedContent : expectedContents) {
            assertTrue(RepoLayoutUtils.TOKENS.contains(expectedContent),
                    "Default layout token set should contain '" + expectedContent + "'");
        }
    }

    private void testReservedRepoLayoutName(String expectedName) {
        assertTrue(RepoLayoutUtils.isReservedName(expectedName), "'" + expectedName +
                "' should be a reserved layout name.");
    }

    private void testGeneratedPatternRegExp(RepoLayout repoLayout, String pattern, String expectedRegExp) {
        assertEquals(RepoLayoutUtils.generateRegExpFromPattern(repoLayout, pattern, Lists.<String>newArrayList()),
                expectedRegExp, "Unexpected converted path pattern regular expression.");
    }

    private void testTokenFilterAssociation(String token, boolean shouldHaveFilter) {
        boolean tokenHasFilter = RepoLayoutUtils.tokenHasFilter(token);
        if (shouldHaveFilter) {
            assertTrue(tokenHasFilter, "Expected token to have a filter.");
        } else {
            assertFalse(tokenHasFilter, "Unexpected token filter.");
        }
    }

    private void testArtifactLayoutAsIvyPattern(RepoLayout repoLayout, String expectedPattern) {
        assertEquals(RepoLayoutUtils.getArtifactLayoutAsIvyPattern(repoLayout), expectedPattern,
                "Unexpected converted ivy pattern.");
    }

    private void testDescriptorLayoutAsIvyPattern(RepoLayout repoLayout, String expectedPattern) {
        assertEquals(RepoLayoutUtils.getDescriptorLayoutAsIvyPattern(repoLayout), expectedPattern,
                "Unexpected converted ivy pattern.");
    }

    private void testWrappedKeywords(String keywordToWrapped, String expectedWrappedValue) {
        assertEquals(RepoLayoutUtils.wrapKeywordAsToken(keywordToWrapped), expectedWrappedValue,
                "Unexpected wrapped token value.");
    }
}
