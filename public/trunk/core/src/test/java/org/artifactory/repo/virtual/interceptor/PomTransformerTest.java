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

package org.artifactory.repo.virtual.interceptor;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.virtual.interceptor.transformer.PomTransformer;
import org.artifactory.util.ResourceUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eli Givoni
 */
@Test
public class PomTransformerTest {
    private String pomAsString;

    @BeforeClass
    private void setup() throws IOException {
        InputStream resource = ResourceUtils.getResource(
                "/org/artifactory/repo/virtual/interceptor/activeByDefault-test.pom");
        pomAsString = IOUtils.toString(resource);
        IOUtils.closeQuietly(resource);
    }

    public void transformWithNothingPolicy() throws IOException {
        PomTransformer pomTransformer = new PomTransformer(pomAsString, PomCleanupPolicy.nothing);
        String transformedPom = pomTransformer.transform();

        Assert.assertEquals(pomAsString, transformedPom, "Expected a matching document");
        Assert.assertTrue(pomAsString.contains("This is a comment"));
    }

    @SuppressWarnings({"unchecked"})
    public void transformWithDiscardActiveReference() {

        PomTransformer pomTransformer = new PomTransformer(pomAsString, PomCleanupPolicy.discard_active_reference);
        String transformedPom = pomTransformer.transform();

        Model pom = MavenModelUtils.stringToMavenModel(transformedPom);
        List repositoriesList = pom.getRepositories();
        List pluginsRepositoriesList = pom.getPluginRepositories();

        assertEmptyList(repositoriesList, pluginsRepositoriesList);

        List<Profile> pomProfiles = pom.getProfiles();
        for (Profile profile : pomProfiles) {
            boolean activeByDefault = false;
            Activation activation = profile.getActivation();
            if (activation != null) {
                activeByDefault = activation.isActiveByDefault();
            }
            List profileRepositories = profile.getRepositories();
            List profilePluginsRepositories = profile.getPluginRepositories();
            if (activeByDefault) {
                assertEmptyList(profileRepositories, profilePluginsRepositories);
            } else {
                assertNotEmptyList(profileRepositories, profilePluginsRepositories);
            }
        }
        Assert.assertTrue(pomAsString.contains("This is a comment"));
    }

    public void transformWithDiscardAnyReference() {
        PomTransformer pomTransformer = new PomTransformer(pomAsString, PomCleanupPolicy.discard_any_reference);
        String transformedPom = pomTransformer.transform();

        Model pom = MavenModelUtils.stringToMavenModel(transformedPom);
        List repositoriesList = pom.getRepositories();
        List pluginsRepositoriesList = pom.getPluginRepositories();

        assertEmptyList(repositoriesList, pluginsRepositoriesList);

        List<Profile> pomProfiles = pom.getProfiles();
        for (Profile profile : pomProfiles) {
            List profileRepositories = profile.getRepositories();
            List profilePluginsRepositories = profile.getPluginRepositories();

            assertEmptyList(profileRepositories, profilePluginsRepositories);
        }
        Assert.assertTrue(pomAsString.contains("This is a comment"));
    }

    public void transformBadPom() throws IOException {
        InputStream badPomResource = ResourceUtils.getResource(
                "/org/artifactory/repo/virtual/interceptor/bad.pom");
        String badPom = IOUtils.toString(badPomResource);
        IOUtils.closeQuietly(badPomResource);
        PomTransformer transformer = new PomTransformer(badPom, PomCleanupPolicy.discard_active_reference);
        String nonTransformedPom = transformer.transform();
        Assert.assertEquals(nonTransformedPom, badPom, "xml document should not have been altered");
        Assert.assertTrue(pomAsString.contains("This is a comment"));
    }

    private void assertEmptyList(Object... list) {
        for (Object o : list) {
            List elementList = (ArrayList) o;
            Assert.assertTrue(elementList.isEmpty(), "Expected an empty list");
        }
    }

    private void assertNotEmptyList(Object... list) {
        for (Object o : list) {
            List elementList = (ArrayList) o;
            Assert.assertFalse(elementList.isEmpty(), "Expected not an empty list");
        }
    }
}
