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

package org.artifactory.maven;

import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.exception.maven.BadPomException;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PomTargetPathValidatorTest {

    public void validatePomTargetValidPath() throws IOException {
        InputStream inputStream = ResourceUtils.getResource("/org/artifactory/maven/yourpit-1.0.0-alpha2.pom");
        String path = "yourpit/yourpit/1.0.0-alpha2";

        ModuleInfo moduleInfo = ModuleInfoUtils.moduleInfoFromDescriptorPath(path, RepoLayoutUtils.MAVEN_2_DEFAULT);

        new PomTargetPathValidator(path, moduleInfo).validate(inputStream, false);
    }

    @Test(expectedExceptions = BadPomException.class)
    public void validatePomTargetInValidPath() throws IOException {
        InputStream inputStream = ResourceUtils.getResource("/org/artifactory/maven/yourpit-1.0.0-alpha2.pom");
        String path = "blab/bla/1.0.0-alpha2";

        ModuleInfo moduleInfo = ModuleInfoUtils.moduleInfoFromDescriptorPath(path, RepoLayoutUtils.MAVEN_2_DEFAULT);

        new PomTargetPathValidator(path, moduleInfo).validate(inputStream, false);
    }
}
