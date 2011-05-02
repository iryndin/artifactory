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

import org.artifactory.util.RepoLayoutUtils;
import org.testng.annotations.Test;

/**
 * @author Noam Y. Tenne
 */
@Test
public class Maven23ModuleInfoUtilsTest extends BaseModuleInfoUtilsTest {

    public Maven23ModuleInfoUtilsTest() {
        super(RepoLayoutUtils.MAVEN_2_DEFAULT);
    }

    public void testRelease() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                ext("jar");
        testArtifactModuleToModule(builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("org/moo/bob/1.0/bob-1.0.pom", builder.build());
    }

    public void testReleaseWithClassifier() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                classifier("sources").ext("jar");
        testArtifactModuleToModule(builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("org/moo/bob/1.0/bob-1.0-sources.pom", builder.build());
    }

    public void testSuperLongRelease() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").ext("jar");
        testArtifactModuleToModule(builder.build());
        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0/bob-mcmoo-mbob-1.0.jar",
                builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0/" +
                "bob-mcmoo-mbob-1.0.pom", builder.build());
    }

    public void testSuperLongReleaseWithClassifier() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").classifier("sources").ext("jar");
        testArtifactModuleToModule(builder.build());
        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0/" +
                "bob-mcmoo-mbob-1.0-sources.jar", builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0/" +
                "bob-mcmoo-mbob-1.0-sources.pom", builder.build());
    }

    public void testUniqueSnapshot() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                folderIntegrationRevision("SNAPSHOT").fileIntegrationRevision("22222222.222222-1").ext("jar");

        testArtifactModuleToModule(builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("org/moo/bob/1.0-SNAPSHOT/bob-1.0-22222222.222222-1.pom", builder.build());
    }

    public void testUniqueSnapshotWithClassifier() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                folderIntegrationRevision("SNAPSHOT").fileIntegrationRevision("22222222.222222-1").
                classifier("sources").ext("jar");

        testArtifactModuleToModule(builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("org/moo/bob/1.0-SNAPSHOT/bob-1.0-22222222.222222-1-sources.pom", builder.build());
    }

    public void testSuperLongUniqueSnapshot() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").folderIntegrationRevision("SNAPSHOT").
                fileIntegrationRevision("22222222.222222-1").ext("jar");

        testArtifactModuleToModule(builder.build());
        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-22222222.222222-1.jar", builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-22222222.222222-1.pom", builder.build());
    }

    public void testSuperLongUniqueSnapshotWithClassifier() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").folderIntegrationRevision("SNAPSHOT").
                fileIntegrationRevision("22222222.222222-1").classifier("sources").ext("jar");

        testArtifactModuleToModule(builder.build());
        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-22222222.222222-1-sources.jar", builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-22222222.222222-1-sources.pom", builder.build());
    }

    public void testNonUniqueSnapshot() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                folderIntegrationRevision("SNAPSHOT").fileIntegrationRevision("SNAPSHOT").ext("jar");

        testArtifactModuleToModule(builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("org/moo/bob/1.0-SNAPSHOT/bob-1.0-SNAPSHOT.pom", builder.build());
    }

    public void testNonUniqueSnapshotWithClassifier() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                folderIntegrationRevision("SNAPSHOT").fileIntegrationRevision("SNAPSHOT").classifier("sources").
                ext("jar");

        testArtifactModuleToModule(builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("org/moo/bob/1.0-SNAPSHOT/bob-1.0-SNAPSHOT-sources.pom", builder.build());
    }

    public void testSuperLongNonUniqueSnapshot() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").folderIntegrationRevision("SNAPSHOT").
                fileIntegrationRevision("SNAPSHOT").ext("jar");

        testArtifactModuleToModule(builder.build());
        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-SNAPSHOT.jar", builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-SNAPSHOT.pom", builder.build());
    }

    public void testSuperLongNonUniqueSnapshotWithClassifier() {
        ModuleInfoBuilder builder = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").folderIntegrationRevision("SNAPSHOT").
                fileIntegrationRevision("SNAPSHOT").classifier("sources").ext("jar");

        testArtifactModuleToModule(builder.build());
        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-SNAPSHOT-sources.jar", builder.build());

        builder.ext("pom");
        testDescriptorPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0-SNAPSHOT/" +
                "bob-mcmoo-mbob-1.0-SNAPSHOT-sources.pom", builder.build());
    }

    public void testReleaseChecksum() {
        ModuleInfo moduleInfo = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                ext("tar.gz.md5").build();

        testArtifactModuleToModule(moduleInfo);

        testArtifactPathToModule("org/moo/bob/1.0/bob-1.0.tar.gz.md5", moduleInfo);
    }

    public void testSuperLongReleaseChecksum() {
        ModuleInfo moduleInfo = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").ext("tar.gz.md5").build();

        testArtifactModuleToModule(moduleInfo);

        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0/" +
                "bob-mcmoo-mbob-1.0.tar.gz.md5", moduleInfo);
    }

    public void testReleaseChecksumWithClassifier() {
        ModuleInfo moduleInfo = new ModuleInfoBuilder().organization("org.moo").module("bob").baseRevision("1.0").
                classifier("sources").ext("tar.gz.sha1").build();

        testArtifactModuleToModule(moduleInfo);

        testArtifactPathToModule("org/moo/bob/1.0/bob-1.0-sources.tar.gz.sha1", moduleInfo);
    }

    public void testSuperLongReleaseChecksumWithClassifier() {
        ModuleInfo moduleInfo = new ModuleInfoBuilder().organization("this.is.a-super.ultra-really.mega-long.org").
                module("bob-mcmoo-mbob").baseRevision("1.0").classifier("sources").ext("tar.gz.sha1").build();

        testArtifactModuleToModule(moduleInfo);

        testArtifactPathToModule("this/is/a-super/ultra-really/mega-long/org/bob-mcmoo-mbob/1.0/" +
                "bob-mcmoo-mbob-1.0-sources.tar.gz.sha1", moduleInfo);
    }
}