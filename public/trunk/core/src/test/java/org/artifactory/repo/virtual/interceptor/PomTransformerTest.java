package org.artifactory.repo.virtual.interceptor;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.virtual.interceptor.transformer.PomTransformer;
import org.artifactory.test.TestUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
public class PomTransformerTest {
    private String pomAsString;

    @BeforeClass
    private void setup() throws IOException {
        InputStream resource = TestUtils.getResource(
                "/org/artifactory/repo/virtual/interceptor/activeByDefault-test.pom");
        pomAsString = IOUtils.toString(resource);
        IOUtils.closeQuietly(resource);
    }

    @Test
    public void transformWithNothingPolicy() {
        PomTransformer pomTransformer = new PomTransformer(pomAsString, PomCleanupPolicy.nothing);
        String transformedPom = pomTransformer.transform();

        Assert.assertEquals(pomAsString, transformedPom, "Expected a matching document");
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void transformWithDiscardActiveReference() throws IOException, XmlPullParserException {

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
    }

    @Test
    public void transformWithDiscardAnyReference() throws IOException, XmlPullParserException {
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
