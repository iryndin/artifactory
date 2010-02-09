package org.artifactory.repo;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import org.testng.annotations.Test;

/**
 * Tests the RepoDetails class
 *
 * @author Noam Y. Tenne
 */
@Test
public class RepoDetailsTest {

    /**
     * Tests that all the repo details are null when initializing the default constructor
     */
    public void testDefaultConstructor() {
        RepoDetails details = new RepoDetails();
        assertNull(details.getKey(), "Repository key should be null.");
        assertNull(details.getDescription(), "Repository description should be null.");
        assertNull(details.getType(), "Repository type should be null.");
        assertNull(details.getUrl(), "Repository URL should be null.");
        assertNull(details.getConfiguration(), "Repository configuration URL should be null.");
    }

    /**
     * Tests that all the local\virtual relevant details are set by the constructor properly
     */
    public void testLocalOrVirtualConstructor() {
        String repoKey = "moo";
        String repoDesc = "moo repository";
        RepoDetailsType repoType = RepoDetailsType.LOCAL;
        String repoUrl = "http://art/moo";

        RepoDetails details = new RepoDetails(repoKey, repoDesc, repoType, repoUrl);

        assertEquals(details.getKey(), repoKey, "Repository key should be " + repoKey);
        assertEquals(details.getDescription(), repoDesc, "Repository description should be " + repoDesc);
        assertEquals(details.getType(), repoType, "Repository type should be " + repoType);
        assertEquals(details.getUrl(), repoUrl, "Repository URL should be " + repoUrl);
        assertNull(details.getConfiguration(), "Repository configuration URL should be null.");
    }

    /**
     * Tests that all the remote relevant details are set by the constructor properly
     */
    public void testRemoteConstructor() {
        String repoKey = "bla";
        String repoDesc = "bla repository";
        RepoDetailsType repoType = RepoDetailsType.REMOTE;
        String repoUrl = "http://art/bla";
        String configUrl = "http://art/bla/configuration";

        RepoDetails details = new RepoDetails(repoKey, repoDesc, repoType, repoUrl, configUrl);

        assertEquals(details.getKey(), repoKey, "Repository key should be " + repoKey);
        assertEquals(details.getDescription(), repoDesc, "Repository description should be " + repoDesc);
        assertEquals(details.getType(), repoType, "Repository type should be " + repoType);
        assertEquals(details.getUrl(), repoUrl, "Repository URL should be " + repoUrl);
        assertEquals(details.getConfiguration(), configUrl, "Repository configuration URL should be " + configUrl);
    }

    /**
     * Tests that all the details are set by the setters properly
     */
    public void testSetters() {
        String repoKey = "pop";
        String repoDesc = "pop repository";
        RepoDetailsType repoType = RepoDetailsType.VIRTUAL;
        String repoUrl = "http://art/pop";
        String configUrl = "http://art/pop/configuration";

        RepoDetails details = new RepoDetails();
        details.setKey(repoKey);
        details.setDescription(repoDesc);
        details.setType(repoType);
        details.setUrl(repoUrl);
        details.setConfiguration(configUrl);

        assertEquals(details.getKey(), repoKey, "Repository key should be " + repoKey);
        assertEquals(details.getDescription(), repoDesc, "Repository description should be " + repoDesc);
        assertEquals(details.getType(), repoType, "Repository type should be " + repoType);
        assertEquals(details.getUrl(), repoUrl, "Repository URL should be " + repoUrl);
        assertEquals(details.getConfiguration(), configUrl, "Repository configuration URL should be " + configUrl);
    }
}
