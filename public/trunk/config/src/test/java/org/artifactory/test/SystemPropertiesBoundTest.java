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

package org.artifactory.test;

import org.artifactory.common.property.ArtifactorySystemProperties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * A convenience class for tests that require bind/unbind of {@link ArtifactorySystemProperties}.
 *
 * @author Yossi Shaul
 */
@Test
public class SystemPropertiesBoundTest {

    @BeforeMethod
    public void setup() {
        ArtifactorySystemProperties.bind(new ArtifactorySystemProperties());
    }

    @AfterMethod
    public void tearDown() {
        ArtifactorySystemProperties.unbind();
    }

}
